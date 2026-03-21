import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  EstudianteDeDocenteDto,
  ComplexivoInformeDto,
  ComplexivoAsesoriaDto,
  PropuestaComplexivoDto
} from '../../../services/complexivo.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

type TabPrincipal = 'propuestas' | 'informes';
type TabInforme   = 'informe' | 'asesorias';

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

  tabPrincipal = signal<TabPrincipal>('propuestas');

  // Propuestas
  propuestas      = signal<PropuestaComplexivoDto[]>([]);
  propuestaSelec  = signal<PropuestaComplexivoDto | null>(null);
  obsDecision     = '';
  mostrarRechazoP = false;
  procesandoIdP   = signal<number | null>(null);

  // Informes
  estudiantes        = signal<EstudianteDeDocenteDto[]>([]);
  estudSeleccionado  = signal<EstudianteDeDocenteDto | null>(null);
  informe            = signal<ComplexivoInformeDto | null>(null);
  asesorias          = signal<ComplexivoAsesoriaDto[]>([]);
  tabInforme         = signal<TabInforme>('informe');
  obsAprobar         = '';
  obsRechazar        = '';
  obsAsesoria        = '';
  mostrarFormRechazo = false;

  constructor(private api: ComplexivoService) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = getSessionEntityId(user, 'docente')
      ?? Number(user?.['idUsuario'] ?? user?.['id_usuario'] ?? 0);
    this.cargarPropuestas();
    this.cargarEstudiantes();
  }

  setTabPrincipal(t: TabPrincipal): void {
    this.tabPrincipal.set(t);
    this.error.set(null); this.ok.set(null);
  }

  // ── Propuestas ─────────────────────────────────────────────────
  seleccionarPropuesta(p: PropuestaComplexivoDto): void {
    this.propuestaSelec.set(p);
    this.obsDecision = ''; this.mostrarRechazoP = false;
    this.error.set(null); this.ok.set(null);
  }

  aprobarPropuesta(): void {
    const p = this.propuestaSelec();
    if (!p) return;
    this.procesandoIdP.set(p.idPropuesta);
    this.error.set(null); this.ok.set(null);
    this.api.decidirPropuesta(this.idDocente, p.idPropuesta, 'APROBADA', this.obsDecision)
      .pipe(finalize(() => this.procesandoIdP.set(null)))
      .subscribe({
        next: (dto) => {
          this.ok.set('✅ Propuesta aprobada. El estudiante puede avanzar a Titulación II.');
          this.propuestaSelec.set(dto);
          this.obsDecision = '';
          this.cargarPropuestas();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazarPropuesta(): void {
    const p = this.propuestaSelec();
    if (!p || !this.obsDecision.trim()) {
      this.error.set('Escribe el motivo del rechazo.');
      return;
    }
    this.procesandoIdP.set(p.idPropuesta);
    this.error.set(null); this.ok.set(null);
    this.api.decidirPropuesta(this.idDocente, p.idPropuesta, 'RECHAZADA', this.obsDecision)
      .pipe(finalize(() => this.procesandoIdP.set(null)))
      .subscribe({
        next: (dto) => {
          this.ok.set('Propuesta rechazada. El estudiante deberá corregirla.');
          this.propuestaSelec.set(dto);
          this.obsDecision = ''; this.mostrarRechazoP = false;
          this.cargarPropuestas();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  // ── Informes ───────────────────────────────────────────────────
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
    this.tabInforme.set(t);
    this.error.set(null); this.ok.set(null);
  }

  aprobarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme) return;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.aprobarInforme(this.idDocente, inf.idInforme, this.obsAprobar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe aprobado.');
          this.obsAprobar = '';
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme || !this.obsRechazar.trim()) {
      this.error.set('Escribe el motivo del rechazo.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.rechazarInforme(this.idDocente, inf.idInforme, this.obsRechazar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe rechazado.');
          this.obsRechazar = ''; this.mostrarFormRechazo = false;
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  registrarAsesoria(): void {
    const est = this.estudSeleccionado();
    if (!est || !this.obsAsesoria.trim()) {
      this.error.set('Escribe las observaciones.');
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
        error: (e) => this.error.set(e?.error?.message ?? 'Error.')
      });
  }

  badgeEstado(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR: 'badge-draft',   ENTREGADO: 'badge-pending',
      APROBADO: 'badge-ok',      RECHAZADO: 'badge-error',
      APROBADA: 'badge-ok',      RECHAZADA: 'badge-error',
      EN_REVISION: 'badge-pending', ENVIADA: 'badge-pending',
      EN_CURSO: 'badge-ok'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }

  propuestaBloqueada(idPropuesta: number): boolean {
    return this.procesandoIdP() === idPropuesta;
  }

  private cargarPropuestas(): void {
    this.api.getPropuestasDocente(this.idDocente).subscribe({
      next: (d) => this.propuestas.set(d),
      error: () => this.propuestas.set([])
    });
  }

  private cargarEstudiantes(): void {
    this.loading.set(true);
    this.api.getMisEstudiantes(this.idDocente)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.estudiantes.set(d),
        error: (e) => this.error.set(e?.error?.message ?? 'Error al cargar.')
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
    this.api.listarAsesorias(this.idDocente, idComplexivo).subscribe({
      next: (d) => this.asesorias.set(d),
      error: () => this.asesorias.set([])
    });
  }
}
