import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionUser, getSessionEntityId, getUserRoles } from '../../../services/session';
import {
  ComisionTemasService,
  PropuestaTemaDto
} from '../../../services/comision-temas';

type FiltroEstado = 'TODAS' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA';
type ModoRevision  = 'COMISION' | 'COMPLEXIVO';

@Component({
  selector: 'app-revision-propuestas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './revision-propuestas.component.html',
  styleUrls: ['./revision-propuestas.component.scss']
})
export class RevisionPropuestasComponent implements OnInit {

  propuestas: PropuestaTemaDto[] = [];
  propuestaEnRevision: PropuestaTemaDto | null = null;
  decisionEstado: 'APROBADA' | 'RECHAZADA' | null = null;
  decisionObservaciones = '';
  errorDecision = '';

  cargando   = false;
  procesando = false;
  filtroActual: FiltroEstado = 'TODAS';

  // Modo: la vista muestra propuestas de comisión O de complexivo
  modoRevision: ModoRevision = 'COMISION';
  esDocenteComplexivo = false;
  esMiembroComision   = false;

  readonly filtros: { valor: FiltroEstado; etiqueta: string }[] = [
    { valor: 'TODAS',       etiqueta: 'Todas' },
    { valor: 'EN_REVISION', etiqueta: 'En revisión' },
    { valor: 'APROBADA',    etiqueta: 'Aprobadas' },
    { valor: 'RECHAZADA',   etiqueta: 'Rechazadas' },
  ];

  private idDocente: number = getSessionEntityId(getSessionUser(), 'docente') ?? 0;

  constructor(private readonly comisionService: ComisionTemasService) {}

  ngOnInit(): void {
    this.detectarModo();
  }

  // ── Detecta si es comisión, docente complexivo, o ambos ──────────────────

  private detectarModo(): void {
    // Primero intentar cargar propuestas de comisión
    // Si el docente es miembro de comisión → carga las de comisión
    // Si tiene estudiantes complexivo asignados → carga las de complexivo
    // Si tiene ambos roles → muestra tabs para elegir

    this.comisionService.listarPropuestasComision(this.idDocente).subscribe({
      next: data => {
        this.esMiembroComision = true;
        if (this.modoRevision === 'COMISION') {
          this.propuestas = data;
          this.cargando = false;
        }
      },
      error: () => {
        // No es miembro de comisión — probar con complexivo
        this.esMiembroComision = false;
      }
    });

    this.comisionService.listarPropuestasComplexivo(this.idDocente).subscribe({
      next: data => {
        this.esDocenteComplexivo = true;
        if (!this.esMiembroComision) {
          // Solo es docente complexivo — cambiar modo automáticamente
          this.modoRevision = 'COMPLEXIVO';
          this.propuestas   = data;
          this.cargando     = false;
        }
      },
      error: () => {
        this.esDocenteComplexivo = false;
        this.cargando = false;
      }
    });
  }

  cambiarModo(modo: ModoRevision): void {
    this.modoRevision = modo;
    this.cargarPropuestas();
  }

  cargarPropuestas(): void {
    this.cargando = true;
    this.propuestas = [];

    if (this.modoRevision === 'COMPLEXIVO') {
      this.comisionService.listarPropuestasComplexivo(this.idDocente).subscribe({
        next: data => { this.propuestas = data; this.cargando = false; },
        error: ()   => { this.cargando = false; }
      });
    } else {
      this.comisionService.listarPropuestasComision(this.idDocente).subscribe({
        next: data => { this.propuestas = data; this.cargando = false; },
        error: ()   => { this.cargando = false; }
      });
    }
  }

  get propuestasFiltradas(): PropuestaTemaDto[] {
    if (this.filtroActual === 'TODAS') return this.propuestas;
    return this.propuestas.filter(p => p.estado === this.filtroActual);
  }

  contarPorEstado(estado: FiltroEstado): number {
    if (estado === 'TODAS') return this.propuestas.length;
    return this.propuestas.filter(p => p.estado === estado).length;
  }

  cambiarFiltro(f: FiltroEstado): void { this.filtroActual = f; }

  abrirDecision(p: PropuestaTemaDto): void {
    this.propuestaEnRevision   = p;
    this.decisionEstado        = null;
    this.decisionObservaciones = '';
    this.errorDecision         = '';
  }

  cerrarModal(): void {
    this.propuestaEnRevision   = null;
    this.decisionEstado        = null;
    this.decisionObservaciones = '';
    this.errorDecision         = '';
  }

  confirmarDecision(): void {
    if (!this.propuestaEnRevision || !this.decisionEstado) return;
    this.errorDecision = '';
    this.procesando    = true;

    const obs$ = this.modoRevision === 'COMPLEXIVO'
      ? this.comisionService.decidirPropuestaComplexivo(
        this.idDocente,
        this.propuestaEnRevision.idPropuesta,
        this.decisionEstado,
        this.decisionObservaciones
      )
      : this.comisionService.decidirPropuesta(
        this.idDocente,
        this.propuestaEnRevision.idPropuesta,
        this.decisionEstado,
        this.decisionObservaciones
      );

    obs$.subscribe({
      next: () => {
        this.procesando = false;
        this.cerrarModal();
        this.cargarPropuestas();
      },
      error: (err) => {
        this.procesando    = false;
        this.errorDecision = err?.error?.message || 'Error al registrar la decisión.';
      }
    });
  }

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      EN_REVISION: 'En revisión',
      APROBADA:    'Aprobada',
      RECHAZADA:   'Rechazada',
      TODAS:       'Todas'
    };
    return map[estado] ?? estado;
  }

  get tituloVista(): string {
    return this.modoRevision === 'COMPLEXIVO'
      ? 'Propuestas — Examen Complexivo'
      : 'Propuestas — Comisión Formativa';
  }

  get subtituloVista(): string {
    return this.modoRevision === 'COMPLEXIVO'
      ? 'Propuestas de tus estudiantes asignados en Complexivo'
      : 'Propuestas de titulación (TIC) pendientes de revisión por la comisión';
  }
}
