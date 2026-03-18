import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  EstudianteDeDocenteDto,
  ComplexivoInformeDto,
  ComplexivoAsesoriaDto
} from '../../../services/complexivo.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

type Tab = 'informe' | 'asesorias';

@Component({
  selector: 'app-complexivo-docente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexivo-docente.html',
  styleUrls: ['./complexivo-docente.scss']
})
export class ComplexivoDocenteComponent implements OnInit {

  private idDocente = 0;

  loading   = signal(false);
  error     = signal<string | null>(null);
  ok        = signal<string | null>(null);

  estudiantes       = signal<EstudianteDeDocenteDto[]>([]);
  estudSeleccionado = signal<EstudianteDeDocenteDto | null>(null);
  informe           = signal<ComplexivoInformeDto | null>(null);
  asesorias         = signal<ComplexivoAsesoriaDto[]>([]);

  tab = signal<Tab>('informe');

  obsAprobar  = '';
  obsRechazar = '';
  obsAsesoria = '';
  mostrarFormRechazo = false;

  constructor(private api: ComplexivoService) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = getSessionEntityId(user, 'docente')
      ?? Number(user?.['idUsuario'] ?? user?.['id_usuario'] ?? 0);
    this.cargarEstudiantes();
  }

  seleccionar(est: EstudianteDeDocenteDto): void {
    this.estudSeleccionado.set(est);
    this.informe.set(null);
    this.asesorias.set([]);
    this.error.set(null);
    this.ok.set(null);
    this.obsAprobar = '';
    this.obsRechazar = '';
    this.obsAsesoria = '';
    this.mostrarFormRechazo = false;
    this.tab.set('informe');
    this.cargarInforme(est.idComplexivo);
    this.cargarAsesorias(est.idComplexivo);
  }

  setTab(t: Tab): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
  }

  aprobar(): void {
    const inf = this.informe();
    if (!inf?.idInforme) return;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.aprobarInforme(this.idDocente, inf.idInforme, this.obsAprobar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe aprobado correctamente.');
          this.obsAprobar = '';
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazar(): void {
    const inf = this.informe();
    if (!inf?.idInforme || !this.obsRechazar.trim()) {
      this.error.set('Debes escribir el motivo del rechazo.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.rechazarInforme(this.idDocente, inf.idInforme, this.obsRechazar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe rechazado. El estudiante podrá corregirlo.');
          this.obsRechazar = '';
          this.mostrarFormRechazo = false;
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  registrarAsesoria(): void {
    const est = this.estudSeleccionado();
    if (!est || !this.obsAsesoria.trim()) {
      this.error.set('Escribe las observaciones de la asesoría.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.registrarAsesoria(this.idDocente, est.idComplexivo, this.obsAsesoria)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Asesoría registrada.');
          this.obsAsesoria = '';
          this.cargarAsesorias(est.idComplexivo);
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al registrar asesoría.')
      });
  }

  badgeEstado(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR: 'badge-draft', ENTREGADO: 'badge-pending',
      APROBADO: 'badge-ok',    RECHAZADO: 'badge-error'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }

  private cargarEstudiantes(): void {
    this.loading.set(true);
    this.api.getMisEstudiantes(this.idDocente)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.estudiantes.set(d),
        error: (e) => this.error.set(e?.error?.message ?? 'Error al cargar estudiantes.')
      });
  }

  private cargarInforme(idComplexivo: number): void {
    this.loading.set(true);
    this.api.getInformeDocente(this.idDocente, idComplexivo)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.informe.set(d),
        error: () => this.informe.set(null)
      });
  }

  private cargarAsesorias(idComplexivo: number): void {
    this.api.listarAsesorias(this.idDocente, idComplexivo)
      .subscribe({
        next: (d) => this.asesorias.set(d),
        error: () => this.asesorias.set([])
      });
  }
}
