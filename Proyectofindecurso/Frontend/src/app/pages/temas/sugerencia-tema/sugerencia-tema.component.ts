import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { getSessionUser, getSessionEntityId } from '../../../services/session';
import {
  ComisionTemasService,
  EstadoModalidadDto,
  TemaBancoDto,
  PropuestaTemaDto,
  CrearPropuestaRequest
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

  private idEstudiante: number = getSessionEntityId(getSessionUser(), 'estudiante') ?? 0;
  private routerSub: Subscription | null = null;

  constructor(
    private readonly comisionService: ComisionTemasService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.cargarTodo();

    // ✅ Recarga cuando el usuario navega a esta página
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

    // ✅ FIX: solo mostrar spinner si aún no tenemos datos de modalidad
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
    if (modo === 'tema-banco') this.cargarTemasDisponibles();
  }

  // ── Modalidad ──────────────────────────────────────────────────────────

  cargarModalidad(): void {
    // ✅ FIX: no mostrar spinner si ya tenemos datos (evita el bug de carga)
    if (!this.estadoModalidad) {
      this.cargandoModalidad = true;
    }

    this.comisionService.obtenerEstadoModalidad(this.idEstudiante).subscribe({
      next: (estado) => {
        this.estadoModalidad       = estado;
        this.modalidadSeleccionada = estado.idModalidad;
        this.cargandoModalidad     = false;
      },
      error: () => {
        this.cargandoModalidad = false;
        // Si falla y no hay datos previos, mostrar bloque sin modalidad
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
        this.cargarPropuestas();
        this.cargarTemasAprobados();
      },
      error: (err) => {
        this.errorModalidad     = err?.error?.message ?? 'Error al guardar la modalidad.';
        this.guardandoModalidad = false;
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
      next: (data) => { this.propuestasEstudiante = data; this.cargandoPropuestas = false; },
      error: ()    => { this.cargandoPropuestas = false; }
    });
  }

  cargarTemasAprobados(): void {
    this.cargandoAprobados = true;
    this.comisionService.listarTemasAprobadosEstudiante(this.idEstudiante).subscribe({
      next: (data) => { this.temasAprobados = data; this.cargandoAprobados = false; },
      error: ()    => { this.cargandoAprobados = false; }
    });
  }

  cargarTemasDisponibles(): void {
    if (this.temasDisponibles.length > 0) return;
    this.cargandoTemas = true;
    this.comisionService.listarTemasDisponiblesEstudiante(this.idEstudiante).subscribe({
      next: (data) => { this.temasDisponibles = data; this.cargandoTemas = false; },
      error: ()    => { this.cargandoTemas = false; }
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

  enviarPropuesta(): void {
    this.errorMsg = '';
    this.exitoMsg = '';
    if (!this.form.titulo?.trim()) { this.errorMsg = 'El título es obligatorio.'; return; }
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
        this.exitoMsg = '¡Propuesta enviada! La comisión la revisará pronto.';
        this.limpiarFormulario();
        setTimeout(() => {
          this.vistaActual = 'mis-propuestas';
          this.exitoMsg    = '';
          this.cargarPropuestas();
        }, 1800);
      },
      error: (err) => {
        this.enviando = false;
        this.errorMsg = err?.error?.message || 'Error al enviar la propuesta.';
      }
    });
  }

  limpiarFormulario(): void {
    this.form             = this.formVacio();
    this.temaSeleccionado = null;
    this.errorMsg         = '';
    this.exitoMsg         = '';
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
        setTimeout(() => {
          this.vistaActual = 'mis-propuestas';
          this.exitoSug    = '';
        }, 2000);
      },
      error: (err) => {
        this.enviandoSug = false;
        this.errorSug    = err?.error?.message ?? 'Error al enviar la sugerencia.';
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
