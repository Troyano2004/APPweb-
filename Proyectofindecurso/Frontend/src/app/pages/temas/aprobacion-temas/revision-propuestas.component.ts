import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionUser, getSessionEntityId } from '../../../services/session';
import {
  ComisionTemasService,
  PropuestaTemaDto
} from '../../../services/comision-temas';

type FiltroEstado = 'TODAS' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA';

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

  cargando  = false;
  procesando = false;
  filtroActual: FiltroEstado = 'TODAS';

  readonly filtros: { valor: FiltroEstado; etiqueta: string }[] = [
    { valor: 'TODAS',       etiqueta: 'Todas' },
    { valor: 'EN_REVISION', etiqueta: 'En revisión' },
    { valor: 'APROBADA',    etiqueta: 'Aprobadas' },
    { valor: 'RECHAZADA',   etiqueta: 'Rechazadas' },
  ];

  private idDocente: number = getSessionEntityId(getSessionUser(), 'docente') ?? 0;

  constructor(private readonly comisionService: ComisionTemasService) {}

  ngOnInit(): void {
    this.cargarPropuestas();
  }

  cargarPropuestas(): void {
    this.cargando = true;
    this.comisionService.listarPropuestasComision(this.idDocente).subscribe({
      next: (data) => { this.propuestas = data; this.cargando = false; },
      error: ()     => { this.cargando = false; }
    });
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

    this.comisionService.decidirPropuesta(
      this.idDocente,
      this.propuestaEnRevision.idPropuesta,
      this.decisionEstado,
      this.decisionObservaciones
    ).subscribe({
      next: () => {
        this.procesando = false;
        this.cerrarModal();
        this.cargarPropuestas();
      },
      error: (err) => {
        this.procesando    = false;
        this.errorDecision = err?.error?.message || 'Error al registrar la decisión. Inténtalo nuevamente.';
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
}
