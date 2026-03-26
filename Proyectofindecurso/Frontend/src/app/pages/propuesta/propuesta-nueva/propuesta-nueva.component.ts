
import { Component, OnInit, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComisionTemasService,
  EstadoModalidadDto,
  FeedbackIAPropuesta,
  PropuestaTemaDto,
  RevisionPropuestaIAResponse,
  TemaBancoDto
} from '../../../services/comision-temas';
import { CatalogoCarrera, CatalogosService } from '../../../services/catalogos';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-propuesta-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './propuesta-nueva.component.html',
  styleUrl: './propuesta-nueva.component.scss'
})
export class PropuestaNuevaComponent implements OnInit {

  idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');

  historial           = signal<PropuestaTemaDto[]>([]);
  temasDisponibles    = signal<TemaBancoDto[]>([]);
  loading             = signal(false);
  loadingTemas        = signal(false);
  loadingCarreras     = signal(false);
  loadingModalidad    = signal(false);
  saving              = signal(false);
  guardandoModalidad  = signal(false);
  error               = signal<string | null>(null);
  ok                  = signal<string | null>(null);
  estadoModalidad     = signal<EstadoModalidadDto | null>(null);
  modalidadSeleccionada = signal<number | null>(null);
  carreras            = signal<CatalogoCarrera[]>([]);

  // ── Estado del panel IA ──────────────────────────────────────────────────
  idPropuestaGuardada  = signal<number | null>(null);
  analizandoIA         = signal(false);
  respuestaIA          = signal<RevisionPropuestaIAResponse | null>(null);
  feedbackIA           = signal<FeedbackIAPropuesta | null>(null);
  errorIA              = signal<string | null>(null);
  modoIA: 'integral' | 'coherencia' | 'pertinencia' | 'viabilidad' = 'integral';
  instruccionIA = '';

  // ── Formulario ───────────────────────────────────────────────────────────
  form = {
    idCarrera: 1,
    idTema: null as number | null,
    titulo: '',
    temaInvestigacion: '',
    planteamientoProblema: '',
    objetivosGenerales: '',
    objetivosEspecificos: '',
    metodologia: '',
    resultadosEsperados: '',
    bibliografia: ''
  };

  constructor(
    private readonly api: ComisionTemasService,
    private readonly catalogosApi: CatalogosService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarEstadoModalidad();
    this.cargarCarreras();
    this.cargarTemasDisponibles();
    this.cargarHistorial();
  }

  // ── Getters ──────────────────────────────────────────────────────────────

  get nombreCarreraSeleccionada(): string {
    return this.carreras().find(c => c.idCarrera === this.form.idCarrera)?.nombre ?? '';
  }

  get temasFiltradosPorCarrera(): TemaBancoDto[] {
    const nombre = this.normalizarTexto(this.nombreCarreraSeleccionada);
    if (!nombre) return this.temasDisponibles();
    return this.temasDisponibles().filter(t => this.normalizarTexto(t.carrera) === nombre);
  }

  get tieneModalidadSeleccionada(): boolean {
    return this.estadoModalidad()?.tieneModalidad ?? false;
  }

  get colorEstado(): Record<string, boolean> {
    const e = this.feedbackIA()?.estado_evaluacion;
    return {
      'estado-aprobable':       e === 'APROBABLE',
      'estado-requiere-ajustes': e === 'REQUIERE_AJUSTES',
      'estado-rechazable':      e === 'RECHAZABLE'
    };
  }

  get etiquetaModo(): string {
    const modos: Record<string, string> = {
      integral:    'Evaluación integral',
      coherencia:  'Coherencia interna',
      pertinencia: 'Pertinencia a la carrera',
      viabilidad:  'Viabilidad del proyecto'
    };
    return modos[this.modoIA] ?? 'Integral';
  }

  // ── Cargar datos ─────────────────────────────────────────────────────────

  cargarEstadoModalidad(): void {
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    this.loadingModalidad.set(true);
    this.api.obtenerEstadoModalidad(this.idEstudiante).subscribe({
      next: estado => {
        this.estadoModalidad.set(estado);
        this.modalidadSeleccionada.set(estado.idModalidad);
        this.loadingModalidad.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingModalidad.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  cargarCarreras(): void {
    this.loadingCarreras.set(true);
    this.catalogosApi.listarCarreras().subscribe({
      next: data => {
        this.carreras.set(data ?? []);
        if (data?.length > 0) this.form.idCarrera = data[0].idCarrera;
        this.loadingCarreras.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingCarreras.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  cargarTemasDisponibles(): void {
    if (!this.idEstudiante) return;
    this.loadingTemas.set(true);
    this.api.listarTemasDisponiblesEstudiante(this.idEstudiante).subscribe({
      next: data => {
        this.temasDisponibles.set(data ?? []);
        this.loadingTemas.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingTemas.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  onSeleccionCarrera(): void {
    this.form.idTema = null;
  }

  onSeleccionTema(): void {
    const tema = this.temasDisponibles().find(t => t.idTema === this.form.idTema);
    if (!tema) return;
    if (!this.form.titulo.trim())             this.form.titulo = tema.titulo;
    if (!this.form.temaInvestigacion.trim())  this.form.temaInvestigacion = tema.titulo;
  }

  cargarHistorial(): void {
    if (!this.idEstudiante) return;
    this.loading.set(true);
    this.api.listarPropuestasEstudiante(this.idEstudiante).subscribe({
      next: resp => {
        this.historial.set(resp ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar historial.');
        this.loading.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  // ── Modalidad ────────────────────────────────────────────────────────────

  guardarModalidad(): void {
    const idModalidad = this.modalidadSeleccionada();
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    if (!idModalidad) { this.error.set('Selecciona una modalidad.'); return; }

    this.guardandoModalidad.set(true);
    this.api.seleccionarModalidad(this.idEstudiante, idModalidad).subscribe({
      next: (estado) => {
        this.estadoModalidad.set(estado);
        this.modalidadSeleccionada.set(estado.idModalidad);
        this.ok.set(`Modalidad registrada: ${estado.modalidad}. Ya puedes enviar tu propuesta.`);
        this.guardandoModalidad.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error al guardar la modalidad.');
        this.guardandoModalidad.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  // ── Enviar propuesta ─────────────────────────────────────────────────────

  enviar(): void {
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    if (!this.tieneModalidadSeleccionada) {
      this.error.set('Antes de registrar la propuesta debes seleccionar tu modalidad.'); return;
    }
    if (!this.form.titulo.trim()) { this.error.set('El título es obligatorio.'); return; }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.limpiarIA();

    this.api.crearPropuestaEstudiante(this.idEstudiante, {
      idCarrera:             this.form.idCarrera,
      idTema:                this.form.idTema ?? undefined,
      titulo:                this.form.titulo,
      temaInvestigacion:     this.form.temaInvestigacion,
      planteamientoProblema: this.form.planteamientoProblema,
      objetivosGenerales:    this.form.objetivosGenerales,
      objetivosEspecificos:  this.form.objetivosEspecificos,
      metodologia:           this.form.metodologia,
      resultadosEsperados:   this.form.resultadosEsperados,
      bibliografia:          this.form.bibliografia
    }).subscribe({
      next: (propuestaCreada) => {
        this.ok.set('Tu propuesta fue enviada a la comisión. ¡Ahora puedes analizarla con IA!');
        this.idPropuestaGuardada.set(propuestaCreada.idPropuesta);
        this.resetForm();
        this.saving.set(false);
        this.cdr.detectChanges();  // ← CLAVE: fuerza que aparezca el panel IA
        this.cargarHistorial();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'No se pudo enviar la propuesta.');
        this.saving.set(false);
        this.cdr.detectChanges();
        this.cargarEstadoModalidad();
      }
    });
  }

  // ── IA: Analizar propuesta ───────────────────────────────────────────────

  analizarConIA(idPropuesta?: number): void {
    const id = idPropuesta ?? this.idPropuestaGuardada();
    if (!id) {
      this.errorIA.set('Primero debes enviar tu propuesta para poder analizarla con IA.');
      this.cdr.detectChanges();
      return;
    }

    this.analizandoIA.set(true);
    this.errorIA.set(null);
    this.feedbackIA.set(null);
    this.respuestaIA.set(null);
    this.idPropuestaGuardada.set(id);
    this.cdr.detectChanges();  // ← fuerza aparición del panel antes de la respuesta

    this.api.evaluarPropuestaConIA(id, {
      modo: this.modoIA,
      instruccionAdicional: this.instruccionIA.trim() || undefined
    })
      .pipe(finalize(() => {
        this.analizandoIA.set(false);
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: resp => {
          this.respuestaIA.set(resp);
          try {
            const parsed: FeedbackIAPropuesta = JSON.parse(resp.feedbackIa);
            this.feedbackIA.set(parsed);
          } catch {
            this.errorIA.set('La IA respondió en formato inesperado. Intente nuevamente.');
          }
          this.cdr.detectChanges();
        },
        error: err => {
          this.errorIA.set(err?.error?.message ?? 'Error al conectarse con el servicio de IA.');
          this.cdr.detectChanges();
        }
      });
  }

  limpiarIA(): void {
    this.feedbackIA.set(null);
    this.respuestaIA.set(null);
    this.errorIA.set(null);
    this.idPropuestaGuardada.set(null);
    this.instruccionIA = '';
    this.modoIA = 'integral';
    this.cdr.detectChanges();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private resetForm(): void {
    this.form = {
      idCarrera: this.form.idCarrera,
      idTema: null,
      titulo: '',
      temaInvestigacion: '',
      planteamientoProblema: '',
      objetivosGenerales: '',
      objetivosEspecificos: '',
      metodologia: '',
      resultadosEsperados: '',
      bibliografia: ''
    };
  }

  private normalizarTexto(valor: string | null | undefined): string {
    return (valor ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
  }
}
