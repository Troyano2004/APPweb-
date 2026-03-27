
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { filter, finalize, Subscription } from 'rxjs';
import { getSessionUser, getSessionEntityId } from '../../../services/session';
import {
  ComisionTemasService,
  EstadoModalidadDto,
  TemaBancoDto,
  PropuestaTemaDto,
  CrearPropuestaRequest,
  FeedbackIAPropuesta,
  RevisionPropuestaIAResponse
} from '../../../services/comision-temas';

type Vista = 'mis-propuestas' | 'nueva-propuesta' | 'sugerir-tema';
type ModoFormulario = 'tema-propio' | 'tema-banco';

@Component({
  selector: 'app-sugerencia-tema',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sugerencia-tema.component.html',
  styleUrls: ['./sugerencia-tema.component.scss']
})
export class SugerenciaTemaComponent implements OnInit, OnDestroy {

  vistaActual: Vista = 'mis-propuestas';
  modoFormulario: ModoFormulario = 'tema-propio';

  // ── Modalidad ──────────────────────────────────────────────────────────
  estadoModalidad: EstadoModalidadDto | null = null;
  cargandoModalidad       = false;
  guardandoModalidad      = false;
  modalidadSeleccionada: number | null = null;
  mostrarCambiarModalidad = false;
  errorModalidad          = '';
  okModalidad             = '';

  // ── Propuestas ────────────────────────────────────────────────────────
  propuestasEstudiante: PropuestaTemaDto[] = [];
  temasDisponibles: TemaBancoDto[]         = [];
  temaSeleccionado: TemaBancoDto | null    = null;
  cargandoPropuestas = false;
  cargandoTemas      = false;
  enviando           = false;
  errorMsg           = '';
  exitoMsg           = '';
  form: CrearPropuestaRequest = this.formVacio();

  // ── Temas aprobados ───────────────────────────────────────────────────
  temasAprobados: TemaBancoDto[] = [];
  cargandoAprobados = false;

  // ── Sugerencia simple ─────────────────────────────────────────────────
  formSugerencia = { titulo: '', descripcion: '' };
  enviandoSug = false;
  errorSug    = '';
  exitoSug    = '';

  // ── Panel IA (revisión PREVIA — usa datos del form, NO guarda en BD) ──
  analizandoIA   = false;
  respuestaIA:   RevisionPropuestaIAResponse | null = null;
  feedbackIA:    FeedbackIAPropuesta | null = null;
  errorIA        = '';
  modoIA: 'integral' | 'coherencia' | 'pertinencia' | 'viabilidad' = 'integral';
  instruccionIA  = '';

  private idEstudiante: number = getSessionEntityId(getSessionUser(), 'estudiante') ?? 0;
  private routerSub: Subscription | null = null;

  constructor(
    private readonly comisionService: ComisionTemasService,
    private readonly router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarTodo();

    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        if ((e.urlAfterRedirects ?? e.url ?? '').includes('/temas/mis-propuestas')) {
          this.cargarTodo();
        }
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  // ── Carga central ──────────────────────────────────────────────────────

  private cargarTodo(): void {
    this.vistaActual      = 'mis-propuestas';
    this.temasDisponibles = [];

    if (this.estadoModalidad) {
      this.cargandoModalidad = false;
    }

    this.cargarModalidad();
    this.cargarPropuestas();
    this.cargarTemasAprobados();
  }

  // ── Getters ────────────────────────────────────────────────────────────

  get tieneModalidad(): boolean {
    return this.estadoModalidad?.tieneModalidad ?? false;
  }

  /** Panel IA visible si hay análisis activo, resultado o error — no depende de propuesta guardada */
  get mostrarPanelIA(): boolean {
    return !!(this.feedbackIA || this.analizandoIA || this.errorIA);
  }

  get colorEstadoIA(): Record<string, boolean> {
    const e = this.feedbackIA?.estado_evaluacion;
    return {
      'estado-aprobable':        e === 'APROBABLE',
      'estado-requiere-ajustes': e === 'REQUIERE_AJUSTES',
      'estado-rechazable':       e === 'RECHAZABLE'
    };
  }

  // ── Navegación ─────────────────────────────────────────────────────────

  cambiarVista(vista: Vista): void {
    if (!this.tieneModalidad) return;
    this.vistaActual = vista;
    this.errorMsg = '';
    this.exitoMsg = '';
    this.errorSug = '';
    this.exitoSug = '';
    if (vista === 'nueva-propuesta' && this.modoFormulario === 'tema-banco') {
      this.cargarTemasDisponibles();
    }
  }

  cambiarModo(modo: ModoFormulario): void {
    this.modoFormulario = modo;
    this.limpiarFormulario();
    this.limpiarIA();
    if (modo === 'tema-banco') this.cargarTemasDisponibles();
  }

  // ── Modalidad ──────────────────────────────────────────────────────────

  cargarModalidad(): void {
    if (!this.estadoModalidad) {
      this.cargandoModalidad = true;
    }
    this.cdr.detectChanges();

    this.comisionService.obtenerEstadoModalidad(this.idEstudiante).subscribe({
      next: (estado) => {
        this.estadoModalidad       = estado;
        this.modalidadSeleccionada = estado.idModalidad;
        this.cargandoModalidad     = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoModalidad = false;
        if (!this.estadoModalidad) {
          this.estadoModalidad = {
            tieneModalidad: false,
            idEleccion: null,
            idModalidad: null,
            modalidad: null,
            idCarrera: null,
            modalidadesDisponibles: []
          };
        }
        this.cdr.detectChanges();
      }
    });
  }

  guardarModalidad(): void {
    if (!this.modalidadSeleccionada) {
      this.errorModalidad = 'Selecciona una modalidad para continuar.';
      return;
    }
    this.errorModalidad     = '';
    this.okModalidad        = '';
    this.guardandoModalidad = true;

    this.comisionService.seleccionarModalidad(this.idEstudiante, this.modalidadSeleccionada).subscribe({
      next: (estado) => {
        this.estadoModalidad         = estado;
        this.modalidadSeleccionada   = estado.idModalidad;
        this.okModalidad             = `✅ Modalidad guardada: ${estado.modalidad}`;
        this.guardandoModalidad      = false;
        this.mostrarCambiarModalidad = false;
        this.cdr.detectChanges();
        this.cargarPropuestas();
        this.cargarTemasAprobados();
      },
      error: (err) => {
        this.errorModalidad     = err?.error?.message ?? 'Error al guardar la modalidad.';
        this.guardandoModalidad = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirCambiarModalidad(): void {
    this.mostrarCambiarModalidad = true;
    this.modalidadSeleccionada   = this.estadoModalidad?.idModalidad ?? null;
    this.errorModalidad          = '';
    this.okModalidad             = '';
  }

  cerrarCambiarModalidad(): void {
    this.mostrarCambiarModalidad = false;
    this.errorModalidad          = '';
  }

  // ── Propuestas ─────────────────────────────────────────────────────────

  cargarPropuestas(): void {
    this.cargandoPropuestas = true;
    this.comisionService.listarPropuestasEstudiante(this.idEstudiante).subscribe({
      next: (data) => { this.propuestasEstudiante = data; this.cargandoPropuestas = false; this.cdr.detectChanges(); },
      error: ()    => { this.cargandoPropuestas = false; this.cdr.detectChanges(); }
    });
  }

  cargarTemasAprobados(): void {
    this.cargandoAprobados = true;
    this.comisionService.listarTemasAprobadosEstudiante(this.idEstudiante).subscribe({
      next: (data) => { this.temasAprobados = data; this.cargandoAprobados = false; this.cdr.detectChanges(); },
      error: ()    => { this.cargandoAprobados = false; this.cdr.detectChanges(); }
    });
  }

  cargarTemasDisponibles(): void {
    if (this.temasDisponibles.length > 0) return;
    this.cargandoTemas = true;
    this.comisionService.listarTemasDisponiblesEstudiante(this.idEstudiante).subscribe({
      next: (data) => { this.temasDisponibles = data; this.cargandoTemas = false; this.cdr.detectChanges(); },
      error: ()    => { this.cargandoTemas = false; this.cdr.detectChanges(); }
    });
  }

  seleccionarTema(tema: TemaBancoDto): void {
    this.temaSeleccionado       = tema;
    this.form.titulo            = tema.titulo;
    this.form.idTema            = tema.idTema;
    this.form.temaInvestigacion = tema.descripcion;
  }

  usarTemaAprobado(tema: TemaBancoDto): void {
    if (!this.tieneModalidad) return;
    this.temaSeleccionado       = tema;
    this.form.titulo            = tema.titulo;
    this.form.idTema            = tema.idTema;
    this.form.temaInvestigacion = tema.descripcion;
    this.modoFormulario         = 'tema-banco';
    this.vistaActual            = 'nueva-propuesta';
  }

  // ── Enviar propuesta (solo después de revisar con IA) ──────────────────
  // IMPORTANTE: el formulario NO se limpia automáticamente al enviar.
  // El usuario puede revisar la confirmación y luego limpiar manualmente.

  enviarPropuesta(): void {
    this.errorMsg = '';
    this.exitoMsg = '';

    if (!this.form.titulo?.trim()) {
      this.errorMsg = 'El título es obligatorio.';
      return;
    }
    if (this.modoFormulario === 'tema-banco' && !this.temaSeleccionado) {
      this.errorMsg = 'Debes seleccionar un tema del banco.';
      return;
    }

    this.enviando = true;
    const payload: CrearPropuestaRequest = { ...this.form };
    if (this.modoFormulario === 'tema-banco' && this.temaSeleccionado) {
      payload.idTema = this.temaSeleccionado.idTema;
    }

    this.comisionService.crearPropuestaEstudiante(this.idEstudiante, payload).subscribe({
      next: () => {
        this.enviando = false;
        // ✅ El form NO se limpia — el estudiante puede ver lo que envió
        this.exitoMsg = '✅ ¡Propuesta enviada a la comisión con éxito!';
        this.cdr.detectChanges();
        this.cargarPropuestas();
      },
      error: (err) => {
        this.enviando = false;
        this.errorMsg = err?.error?.message || 'Error al enviar la propuesta.';
        this.cdr.detectChanges();
      }
    });
  }

  limpiarFormulario(): void {
    this.form             = this.formVacio();
    this.temaSeleccionado = null;
    this.errorMsg         = '';
    this.exitoMsg         = '';
    this.limpiarIA();
  }

  // ── Panel IA: analiza los datos del FORM EN MEMORIA (sin guardar en BD) ──

  analizarConIA(): void {
    if (!this.form.titulo?.trim()) {
      this.errorIA = 'Escribe al menos el título antes de analizar.';
      this.cdr.detectChanges();
      return;
    }

    this.analizandoIA = true;
    this.errorIA      = '';
    this.feedbackIA   = null;
    this.respuestaIA  = null;
    this.cdr.detectChanges();

    this.comisionService.evaluarPropuestaConIAPrevia({
      idEstudiante:          this.idEstudiante,
      titulo:                this.form.titulo,
      temaInvestigacion:     this.form.temaInvestigacion,
      planteamientoProblema: this.form.planteamientoProblema,
      objetivosGenerales:    this.form.objetivosGenerales,
      objetivosEspecificos:  this.form.objetivosEspecificos,
      metodologia:           this.form.metodologia,
      resultadosEsperados:   this.form.resultadosEsperados,
      bibliografia:          (this.form as any).bibliografia,
      modo:                  this.modoIA,
      instruccionAdicional:  this.instruccionIA.trim() || undefined
    })
      .pipe(finalize(() => {
        this.analizandoIA = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (resp) => {
          this.respuestaIA = resp;
          try {
            const raw   = resp.feedbackIa;
            const start = raw.indexOf('{');
            const end   = raw.lastIndexOf('}');
            const json  = (start !== -1 && end > start)
              ? raw.substring(start, end + 1)
              : raw;
            this.feedbackIA = JSON.parse(json);
          } catch {
            this.errorIA = 'La IA respondió en formato inesperado. Intenta nuevamente.';
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorIA = err?.error?.message ?? 'Error al conectarse con el servicio de IA.';
          this.cdr.detectChanges();
        }
      });
  }

  limpiarIA(): void {
    this.feedbackIA   = null;
    this.respuestaIA  = null;
    this.errorIA      = '';
    this.instruccionIA = '';
    this.modoIA       = 'integral';
    this.cdr.detectChanges();
  }

  // ── Sugerencia ─────────────────────────────────────────────────────────

  enviarSugerencia(): void {
    this.errorSug = '';
    this.exitoSug = '';
    if (!this.formSugerencia.titulo.trim())      { this.errorSug = 'El título es obligatorio.'; return; }
    if (!this.formSugerencia.descripcion.trim())  { this.errorSug = 'La descripción es obligatoria.'; return; }
    if (!this.idEstudiante)                       { this.errorSug = 'No se pudo identificar tu sesión.'; return; }

    this.enviandoSug = true;
    this.comisionService.sugerirTema(
      this.idEstudiante,
      this.formSugerencia.titulo,
      this.formSugerencia.descripcion
    ).subscribe({
      next: () => {
        this.enviandoSug    = false;
        this.exitoSug       = '¡Sugerencia enviada! La comisión la revisará pronto.';
        this.formSugerencia = { titulo: '', descripcion: '' };
        this.cdr.detectChanges();
        setTimeout(() => {
          this.vistaActual = 'mis-propuestas';
          this.exitoSug    = '';
        }, 2000);
      },
      error: (err) => {
        this.enviandoSug = false;
        this.errorSug    = err?.error?.message ?? 'Error al enviar la sugerencia.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      EN_REVISION: 'En revisión',
      APROBADA:    'Aprobada',
      RECHAZADA:   'Rechazada'
    };
    return map[estado] ?? estado;
  }

  private formVacio(): CrearPropuestaRequest {
    return {
      titulo: '', temaInvestigacion: '', planteamientoProblema: '',
      objetivosGenerales: '', objetivosEspecificos: '', marcoTeorico: '',
      metodologia: '', resultadosEsperados: '', bibliografia: ''
    };
  }
}
