import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  CertificadoAntiplacioDto,
  DocumentoTitulacionTextoDto,
  GuardarRevisionIaRequest,
  RegistrarAntiplacioIaRequest,
  WasItAiResponse
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

// ── Tipos locales ─────────────────────────────────────────────────────────────

export type WasItAIResult = WasItAiResponse;

export interface SeccionAnalisis {
  clave: string;
  label: string;
  texto: string;
  resultado: WasItAIResult | null;
  analizando: boolean;
  error: string | null;
}

const SECCIONES_CONFIG: { clave: keyof DocumentoTitulacionTextoDto; label: string }[] = [
  { clave: 'resumen',               label: 'Resumen' },
  { clave: 'abstractText',          label: 'Abstract' },
  { clave: 'introduccion',          label: 'Introducción' },
  { clave: 'planteamientoProblema', label: 'Planteamiento del Problema' },
  { clave: 'justificacion',         label: 'Justificación' },
  { clave: 'objetivoGeneral',       label: 'Objetivo General' },
  { clave: 'objetivosEspecificos',  label: 'Objetivos Específicos' },
  { clave: 'marcoTeorico',          label: 'Marco Teórico' },
  { clave: 'metodologia',           label: 'Metodología' },
  { clave: 'resultados',            label: 'Resultados' },
  { clave: 'discusion',             label: 'Discusión' },
  { clave: 'conclusiones',          label: 'Conclusiones' },
  { clave: 'recomendaciones',       label: 'Recomendaciones' },
];

const PLACEHOLDER_PREFIXES = ['pendiente', 'por definir', 'sin definir'];

function esPlaceholder(texto: string): boolean {
  return PLACEHOLDER_PREFIXES.some(p => texto.trim().toLowerCase().startsWith(p));
}

@Component({
  selector: 'app-antiplagio-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './antiplagio-dt2.html',
  styleUrl: './antiplagio-dt2.scss'
})
export class AntiplagioDt2Component implements OnInit {

  // ── Estado general ──────────────────────────────────────────────────────────
  loading = signal(false);
  error   = signal<string | null>(null);
  ok      = signal<string | null>(null);

  // ── Proyectos ───────────────────────────────────────────────────────────────
  proyectos            = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  certificado          = signal<CertificadoAntiplacioDto | null>(null);

  // ── Documento de titulación ─────────────────────────────────────────────────
  documentoTexto    = signal<DocumentoTitulacionTextoDto | null>(null);
  cargandoDocumento = signal(false);

  // ── Análisis IA ─────────────────────────────────────────────────────────────
  analizandoIA        = signal(false);
  registrandoIA       = signal(false);
  guardandoIA         = signal(false);
  seccionesAnalizadas = signal<SeccionAnalisis[]>([]);
  promedioConfianzaIA = signal<number | null>(null);

  /** Número de secciones que ya terminaron de analizarse (para la barra de progreso) */
  seccionesCompletadas(): number {
    return this.seccionesAnalizadas().filter(s => !s.analizando).length;
  }

  private idDocenteDt2 = 0;

  constructor(private dt2: Dt2Service, private fb: FormBuilder) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocenteDt2 = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }

  // ── Selección de proyecto ───────────────────────────────────────────────────

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.error.set(null);
    this.ok.set(null);
    this.seccionesAnalizadas.set([]);
    this.promedioConfianzaIA.set(null);
    this.documentoTexto.set(null);
    this.cargarCertificado(p.idProyecto);
    this.cargarDocumentoTexto(p.idProyecto);
  }

  // ── Análisis IA + registro automático ──────────────────────────────────────

  async analizarYRegistrar(): Promise<void> {
    const doc = this.documentoTexto();
    const p   = this.proyectoSeleccionado();
    if (!doc || !p) return;

    // Filtrar secciones con contenido real
    const conTexto = SECCIONES_CONFIG
      .map(cfg => ({
        clave: cfg.clave,
        label: cfg.label,
        texto: (doc[cfg.clave] as string | null) ?? ''
      }))
      .filter(s => s.texto && s.texto.trim().length > 50 && !esPlaceholder(s.texto));

    if (conTexto.length === 0) {
      this.error.set('El documento no tiene secciones con contenido suficiente. Solicita al estudiante que complete el documento.');
      return;
    }

    // Inicializar secciones
    this.seccionesAnalizadas.set(
      conTexto.map(s => ({ ...s, resultado: null, analizando: true, error: null }))
    );
    this.analizandoIA.set(true);
    this.promedioConfianzaIA.set(null);
    this.error.set(null);
    this.ok.set(null);

    // Analizar sección por sección
    for (let i = 0; i < conTexto.length; i++) {
      try {
        const resultado = await this.llamarWasItAI(conTexto[i].texto);
        this.seccionesAnalizadas.update(secciones =>
          secciones.map((s, idx) => idx === i ? { ...s, resultado, analizando: false } : s)
        );
      } catch (err: any) {
        this.seccionesAnalizadas.update(secciones =>
          secciones.map((s, idx) =>
            idx === i ? { ...s, error: err?.message ?? 'Error al analizar', analizando: false } : s
          )
        );
      }
    }

    this.analizandoIA.set(false);
    const promedio = this.calcularPromedio();

    if (promedio === null) {
      this.error.set('No se pudo obtener resultado del análisis IA.');
      return;
    }

    // Guardar revisión IA en BD
    await this.guardarRevisionIA(p.idProyecto, promedio);

    // Registrar antiplagio automáticamente con el porcentaje IA
    this.registrarAntiplacioDesdeIA(p.idProyecto, promedio);
  }

  /** Re-analizar sin volver a registrar (solo para revisar resultados) */
  async reanalizar(): Promise<void> {
    this.seccionesAnalizadas.set([]);
    this.promedioConfianzaIA.set(null);
    await this.analizarYRegistrar();
  }

  // ── Helpers de UI ───────────────────────────────────────────────────────────

  getNivelRiesgo(): 'bajo' | 'medio' | 'alto' {
    const p = this.promedioConfianzaIA();
    if (p === null) return 'bajo';
    if (p < 40)    return 'bajo';
    if (p < 70)    return 'medio';
    return 'alto';
  }

  getBarraColor(confidence: number): string {
    const pct = confidence * 100;
    if (pct < 40) return '#38a169';
    if (pct < 70) return '#d69e2e';
    return '#e53e3e';
  }

  // ── Privados ────────────────────────────────────────────────────────────────

  private llamarWasItAI(texto: string): Promise<WasItAIResult> {
    return new Promise((resolve, reject) => {
      this.dt2.detectarConWasItAI({ content: texto }).subscribe({
        next: r  => resolve(r as WasItAIResult),
        error: (err: any) => reject(new Error(
          err?.error?.mensaje ?? err?.error?.message ?? `Error HTTP ${err?.status ?? ''}`
        ))
      });
    });
  }

  /** Calcula el promedio de confianza IA y actualiza la señal. Retorna el valor. */
  private calcularPromedio(): number | null {
    const con = this.seccionesAnalizadas().filter(s => s.resultado !== null);
    if (!con.length) return null;
    const suma = con.reduce((acc, s) => acc + s.resultado!.confidence * 100, 0);
    const prom = Math.round(suma / con.length);
    this.promedioConfianzaIA.set(prom);
    return prom;
  }

  /** Persiste el análisis IA en feedbackIa + estadoRevisionIa */
  private guardarRevisionIA(idProyecto: number, promedio: number): Promise<void> {
    return new Promise(resolve => {
      const nivel  = this.getNivelRiesgoDesde(promedio);
      const detalle = this.seccionesAnalizadas()
        .filter(s => s.resultado)
        .map(s => `${s.label}: ${(s.resultado!.confidence * 100).toFixed(1)}% IA — ${s.resultado!.analysis.reasoning}`)
        .join(' | ');

      const req: GuardarRevisionIaRequest = {
        porcentajePromedioIa: promedio,
        nivelRiesgo:          nivel.toUpperCase(),
        feedbackIa:           detalle,
        estadoRevisionIa:     nivel === 'bajo' ? 'LIMPIO' : nivel === 'medio' ? 'SOSPECHOSO' : 'ANALIZADO'
      };

      this.guardandoIA.set(true);
      this.dt2.guardarRevisionIa(idProyecto, req)
        .pipe(finalize(() => this.guardandoIA.set(false)))
        .subscribe({ next: () => resolve(), error: () => resolve() });
    });
  }

  /**
   * Registra el antiplagio automáticamente usando el porcentaje calculado por IA.
   * Llama a POST /api/dt2/proyectos/{id}/antiplagio sin archivo PDF,
   * usando el método registrarAntiplacioSinPdf del servicio.
   */
  private registrarAntiplacioDesdeIA(idProyecto: number, promedio: number): void {
    const req: RegistrarAntiplacioIaRequest = {
      idDocenteDt2:           this.idDocenteDt2,
      porcentajeCoincidencia: promedio,
      nivelRiesgo:            this.getNivelRiesgoDesde(promedio).toUpperCase(),
      observaciones:          `Análisis automático IA (WasItAIGenerated) — Nivel: ${this.getNivelRiesgoDesde(promedio).toUpperCase()}`
    };

    this.registrandoIA.set(true);
    this.error.set(null);

    this.dt2.registrarAntiplacioIA(idProyecto, req)
      .pipe(finalize(() => this.registrandoIA.set(false)))
      .subscribe({
        next: cert => {
          this.certificado.set(cert);
          const estado = cert.certificadoFavorable
            ? '✓ FAVORABLE — el documento pasa a ANTIPLAGIO_APROBADO. El coordinador puede programar la predefensa.'
            : '✗ RECHAZADO — índice IA ≥ 10 %. El estudiante debe corregir el documento.';
          this.ok.set(`Análisis completado y registrado. ${estado}`);
          this.cargarProyectos();
        },
        error: (err: any) => this.error.set(
          err?.error?.mensaje ?? err?.error?.message ?? 'Error al registrar el resultado antiplagio'
        )
      });
  }

  private getNivelRiesgoDesde(prom: number): 'bajo' | 'medio' | 'alto' {
    if (prom < 40) return 'bajo';
    if (prom < 70) return 'medio';
    return 'alto';
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    this.dt2.listarProyectosDocenteDt2(this.idDocenteDt2)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.proyectos.set(data),
        error: ()   => this.error.set('Error al cargar proyectos')
      });
  }

  private cargarCertificado(idProyecto: number): void {
    this.dt2.getCertificado(idProyecto).subscribe({
      next: data => this.certificado.set(data),
      error: ()   => this.certificado.set(null)
    });
  }

  private cargarDocumentoTexto(idProyecto: number): void {
    this.cargandoDocumento.set(true);
    this.dt2.getDocumentoTexto(idProyecto)
      .pipe(finalize(() => this.cargandoDocumento.set(false)))
      .subscribe({
        next: doc => this.documentoTexto.set(doc),
        error: ()  => this.documentoTexto.set(null)
      });
  }
}
