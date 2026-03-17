import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionUser, getSessionEntityId } from '../../../services/session';
import {
  ComisionTemasService,
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
export class SugerenciaTemaComponent implements OnInit {

  vistaActual: Vista = 'mis-propuestas';
  modoFormulario: ModoFormulario = 'tema-propio';

  // ── Propuestas de titulación ───────────────────────────────────────────
  propuestasEstudiante: PropuestaTemaDto[] = [];
  temasDisponibles: TemaBancoDto[]         = [];
  temaSeleccionado: TemaBancoDto | null    = null;

  cargandoPropuestas = false;
  cargandoTemas      = false;
  enviando           = false;
  errorMsg           = '';
  exitoMsg           = '';

  form: CrearPropuestaRequest = this.formVacio();

  // ── Temas aprobados (sugerencia aprobada por la comisión) ─────────────
  temasAprobados: TemaBancoDto[]  = [];
  cargandoAprobados               = false;

  // ── Sugerencia simple al banco ─────────────────────────────────────────
  formSugerencia = { titulo: '', descripcion: '' };
  enviandoSug    = false;
  errorSug       = '';
  exitoSug       = '';

  private idEstudiante: number = getSessionEntityId(getSessionUser(), 'estudiante') ?? 0;

  constructor(private readonly comisionService: ComisionTemasService) {}

  ngOnInit(): void {
    this.cargarPropuestas();
    this.cargarTemasAprobados();
  }

  // ── Navegación ─────────────────────────────────────────────────────────

  cambiarVista(vista: Vista): void {
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
    if (modo === 'tema-banco') {
      this.cargarTemasDisponibles();
    }
  }

  seleccionarTema(tema: TemaBancoDto): void {
    this.temaSeleccionado       = tema;
    this.form.titulo            = tema.titulo;
    this.form.idTema            = tema.idTema;
    this.form.temaInvestigacion = tema.descripcion;
  }

  usarTemaAprobado(tema: TemaBancoDto): void {
    this.temaSeleccionado       = tema;
    this.form.titulo            = tema.titulo;
    this.form.idTema            = tema.idTema;
    this.form.temaInvestigacion = tema.descripcion;
    this.modoFormulario         = 'tema-banco';
    this.cambiarVista('nueva-propuesta');
  }

  // ── Carga de datos ─────────────────────────────────────────────────────

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

  // ── Enviar propuesta de titulación ────────────────────────────────────

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
        this.exitoMsg = '¡Propuesta enviada! La comisión la revisará pronto.';
        this.limpiarFormulario();
        setTimeout(() => {
          this.cambiarVista('mis-propuestas');
          this.cargarPropuestas();
        }, 1800);
      },
      error: (err) => {
        this.enviando = false;
        this.errorMsg = err?.error?.message || 'Error al enviar la propuesta. Inténtalo nuevamente.';
      }
    });
  }

  limpiarFormulario(): void {
    this.form             = this.formVacio();
    this.temaSeleccionado = null;
    this.errorMsg         = '';
    this.exitoMsg         = '';
  }

  // ── Enviar sugerencia simple ──────────────────────────────────────────

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
        this.enviandoSug        = false;
        this.exitoSug           = '¡Sugerencia enviada! La comisión la revisará pronto.';
        this.formSugerencia     = { titulo: '', descripcion: '' };
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
      titulo: '',
      temaInvestigacion: '',
      planteamientoProblema: '',
      objetivosGenerales: '',
      objetivosEspecificos: '',
      marcoTeorico: '',
      metodologia: '',
      resultadosEsperados: '',
      bibliografia: ''
    };
  }
}
