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

type TabInforme = 'informe' | 'asesorias';

@Component({
  selector: 'app-complexivo-dt2',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexivo-dt2.html',
  styleUrls: ['./complexivo-dt2.scss']
})
export class ComplexivoDt2Component implements OnInit {

  private idDocente = 0;

  loading   = signal(false);
  error     = signal<string | null>(null);
  ok        = signal<string | null>(null);

  estudiantes       = signal<EstudianteDeDocenteDto[]>([]);
  estudSeleccionado = signal<EstudianteDeDocenteDto | null>(null);
  informe           = signal<ComplexivoInformeDto | null>(null);
  asesorias         = signal<ComplexivoAsesoriaDto[]>([]);
  tabInforme        = signal<TabInforme>('informe');
  obsAprobar        = '';
  obsRechazar       = '';
  obsAsesoria       = '';
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
    this.informe.set(null); this.asesorias.set([]);
    this.error.set(null); this.ok.set(null);
    this.obsAprobar = ''; this.obsRechazar = '';
    this.obsAsesoria = ''; this.mostrarFormRechazo = false;
    this.tabInforme.set('informe');
    this.cargarInforme(est.idComplexivo);
    this.cargarAsesorias(est.idComplexivo);
  }

  setTabInforme(t: TabInforme): void {
    this.tabInforme.set(t); this.error.set(null); this.ok.set(null);
  }

  aprobarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme) return;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.aprobarInformeDt2(this.idDocente, inf.idInforme, this.obsAprobar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => { this.informe.set(d); this.ok.set('Informe aprobado.'); this.obsAprobar = ''; this.cargarEstudiantes(); },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme || !this.obsRechazar.trim()) { this.error.set('Escribe el motivo del rechazo.'); return; }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.rechazarInformeDt2(this.idDocente, inf.idInforme, this.obsRechazar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => { this.informe.set(d); this.ok.set('Informe rechazado.'); this.obsRechazar = ''; this.mostrarFormRechazo = false; this.cargarEstudiantes(); },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  registrarAsesoria(): void {
    const est = this.estudSeleccionado();
    if (!est || !this.obsAsesoria.trim()) { this.error.set('Escribe las observaciones.'); return; }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.registrarAsesoriaDt2(this.idDocente, est.idComplexivo, this.obsAsesoria)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => { this.ok.set('Asesoría registrada.'); this.obsAsesoria = ''; this.cargarAsesorias(est.idComplexivo); },
        error: (e) => this.error.set(e?.error?.message ?? 'Error.')
      });
  }

  badgeEstado(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR: 'badge-draft', ENTREGADO: 'badge-pending',
      APROBADO: 'badge-ok', RECHAZADO: 'badge-error'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }

  private cargarEstudiantes(): void {
    this.loading.set(true);
    this.api.getMisEstudiantesDt2(this.idDocente)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: (d) => this.estudiantes.set(d), error: () => {} });
  }
  private cargarInforme(idComplexivo: number): void {
    this.loading.set(true);
    this.api.getInformeDocenteDt2(this.idDocente, idComplexivo)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: (d) => this.informe.set(d), error: () => this.informe.set(null) });
  }
  private cargarAsesorias(idComplexivo: number): void {
    this.api.listarAsesoriasDt2(this.idDocente, idComplexivo)
      .subscribe({ next: (d) => this.asesorias.set(d), error: () => this.asesorias.set([]) });
  }
}
