import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardDetalle, DashboardService, DashboardResumen } from '../../services/DashboardService';
import { DocumentoPendienteDto, RevisionDirectorService } from '../../services/revision-director';
import { DocumentoTitulacionDto, DocumentoTitulacionService } from '../../services/documento-titulacion';
import { getSessionEntityId, getSessionUser, hasRole } from '../../services/session';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <h1>{{ pageTitle() }}</h1>
        <p>{{ pageSubtitle() }}</p>
      </div>

      <div class="status" *ngIf="loading">Cargando información...</div>
      <div class="status error" *ngIf="error">{{ error }}</div>

      <ng-container *ngIf="isCoordinator()">
        <div class="stats" *ngIf="!loading && resumen">
          <article class="card"><h3>Propuestas en revisión</h3><strong>{{ resumen.propuestasPendientes }}</strong></article>
          <article class="card"><h3>Tutorías activas</h3><strong>{{ resumen.tutoriasActivas }}</strong></article>
          <article class="card"><h3>Proyectos aprobados</h3><strong>{{ resumen.proyectosAprobados }}</strong></article>
          <article class="card"><h3>Documentos pendientes</h3><strong>{{ resumen.documentosPendientes }}</strong></article>
        </div>

        <div class="grid">
          <article class="card large">
            <h3>Accesos de coordinación</h3>
            <div class="quick-links">
              <a routerLink="/app/coordinador/seguimiento">Seguimiento de proyectos</a>
              <a routerLink="/app/coordinador/validacion">Validación administrativa</a>
              <a routerLink="/app/admin/usuarios">Administrar usuarios</a>
              <a routerLink="/app/admin/roles">Roles del aplicativo</a>
            </div>
          </article>
          <article class="card large">
            <h3>Alertas rápidas</h3>
            <ul *ngIf="detalle?.alertas?.length; else emptyAlertas">
              <li *ngFor="let alerta of detalle?.alertas">{{ alerta.mensaje }}</li>
            </ul>
            <ng-template #emptyAlertas><p class="empty">Sin alertas disponibles.</p></ng-template>
          </article>
        </div>
      </ng-container>

      <ng-container *ngIf="isStudent()">
        <div class="stats student-stats">
          <article class="card">
            <h3>Estado del documento</h3>
            <strong>{{ documentoEstudiante?.estado || 'SIN_INICIAR' }}</strong>
          </article>
          <article class="card">
            <h3>Completitud estimada</h3>
            <strong>{{ completitudDocumento() }}%</strong>
          </article>
          <article class="card">
            <h3>Observaciones recibidas</h3>
            <strong>0</strong>
          </article>
        </div>

        <div class="grid student-grid">
          <article class="card large student-highlight">
            <h3>Tribunal asignado</h3>
            <p class="student-detail">
              <strong class="detail-main">{{ documentoEstudiante?.tribunal || 'Pendiente de asignación' }}</strong>
            </p>
            <div class="meeting-info">
              <div>
                <span class="label">Fecha</span>
                <span class="value">{{ formatFechaSustentacion() }}</span>
              </div>
              <div>
                <span class="label">Hora</span>
                <span class="value">{{ formatHoraSustentacion() }}</span>
              </div>
              <div>
                <span class="label">Lugar</span>
                <span class="value">{{ documentoEstudiante?.lugarSustentacion || 'Por confirmar' }}</span>
              </div>
            </div>
          </article>

          <article class="card large student-highlight recommendations-card">
            <h3>Recomendaciones para tu sustentación</h3>
            <p class="recommendation-text">{{ recomendacionesPreview() }}</p>
            <ul>
              <li>Prepara una presentación breve enfocada en objetivos, metodología y resultados.</li>
              <li>Practica respuestas para preguntas técnicas y justifica tus decisiones de investigación.</li>
              <li>Revisa que anexos y referencias estén listos para consulta del tribunal.</li>
            </ul>
            <div class="quick-links">
              <a routerLink="/app/titulacion2/documento">Actualizar recomendaciones</a>
            </div>
          </article>

          <article class="card large">
            <h3>Tu plan de trabajo</h3>
            <ul>
              <li>Completa título, resumen e introducción.</li>
              <li>Verifica metodología y resultados antes de enviar.</li>
              <li>Envía el documento a revisión cuando todo esté listo.</li>
            </ul>
            <div class="quick-links">
              <a routerLink="/app/titulacion2/documento">Abrir mi documento</a>
            </div>
          </article>
        </div>
      </ng-container>

      <ng-container *ngIf="isTeacher()">
        <div class="stats">
          <article class="card"><h3>Proyectos por revisar</h3><strong>{{ pendientesDocente.length }}</strong></article>
        </div>

        <div class="grid">
          <article class="card large">
            <h3>Revisión pendiente</h3>
            <ul *ngIf="pendientesDocente.length; else noPendientes">
              <li *ngFor="let item of pendientesDocente">
                {{ item.titulo || 'Sin título' }} · Estudiante {{ item.nombreEstudiante || ('#' + item.idEstudiante) }}
              </li>
            </ul>
            <ng-template #noPendientes><p class="empty">No existen proyectos pendientes en este momento.</p></ng-template>
            <div class="quick-links"><a routerLink="/app/titulacion2/revision">Ir al módulo de revisión</a></div>
          </article>
        </div>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .page { display: flex; flex-direction: column; gap: 1.5rem; }
      .page-header h1 { margin: 0 0 0.4rem; font-size: 1.6rem; }
      .page-header p { margin: 0; color: #6b7280; }
      .status { padding: 0.75rem 1rem; border-radius: 0.75rem; background: #fff; border: 1px solid #e5e7eb; font-weight: 600; }
      .status.error { color: #b42318; border-color: #fecaca; background: #fff5f5; }
      .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }
      .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; }
      .card { background: #fff; border: 1px solid #e5e7eb; border-radius: 0.75rem; padding: 1.2rem; }
      .card strong { display: block; font-size: 1.6rem; margin: 0.5rem 0; color: #0f7a3a; }
      .card.large ul { margin: 0.8rem 0 0; padding-left: 1.2rem; color: #4b5563; }
      .quick-links { margin-top: 0.8rem; display: grid; gap: 0.45rem; }
      .quick-links a { color: #0f7a3a; font-weight: 600; text-decoration: none; }

      .student-grid .card { min-height: 100%; }
      .student-highlight { border-color: #bbf7d0; background: linear-gradient(180deg, #f7fff9 0%, #ffffff 100%); }
      .student-detail { margin: 0.5rem 0 0; }
      .detail-main { font-size: 1.05rem !important; line-height: 1.5; color: #14532d !important; }
      .meeting-info { margin-top: 0.9rem; display: grid; gap: 0.6rem; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); }
      .meeting-info div { background: #ecfdf3; border: 1px solid #d1fae5; border-radius: 0.65rem; padding: 0.55rem 0.7rem; }
      .meeting-info .label { display: block; color: #166534; font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.02em; }
      .meeting-info .value { display: block; color: #14532d; font-weight: 600; margin-top: 0.1rem; }
      .recommendations-card ul { margin-top: 0.7rem; }
      .recommendation-text { margin-top: 0.65rem; color: #14532d; background: #ecfdf3; border: 1px solid #d1fae5; border-radius: 0.65rem; padding: 0.65rem 0.8rem; }
      .empty { margin: 0.8rem 0 0; color: #6b7280; }
    `
  ]
})
export class DashboardComponent implements OnInit {
  resumen: DashboardResumen | null = null;
  detalle: DashboardDetalle | null = null;
  documentoEstudiante: DocumentoTitulacionDto | null = null;
  pendientesDocente: DocumentoPendienteDto[] = [];

  loading = false;
  error = '';

  private readonly user = signal(getSessionUser());
  isCoordinator = computed(() => hasRole(this.user()?.rol, 'ROLE_COORDINADOR'));
  isTeacher = computed(() => hasRole(this.user()?.rol, 'ROLE_DOCENTE'));
  isStudent = computed(() => hasRole(this.user()?.rol, 'ROLE_ESTUDIANTE'));

  constructor(
    private readonly dashboardService: DashboardService,
    private readonly revisionService: RevisionDirectorService,
    private readonly documentoService: DocumentoTitulacionService
  ) {}

  ngOnInit(): void {
    if (this.isCoordinator()) {
      this.cargarDashboardCoordinacion();
      return;
    }

    if (this.isTeacher()) {
      this.cargarDashboardDocente();
      return;
    }

    if (this.isStudent()) {
      this.cargarDashboardEstudiante();
    }
  }

  pageTitle(): string {
    if (this.isCoordinator()) return 'Dashboard de coordinación';
    if (this.isTeacher()) return 'Dashboard docente';
    if (this.isStudent()) return 'Mi dashboard de titulación';
    return 'Dashboard';
  }

  pageSubtitle(): string {
    if (this.isCoordinator()) return 'Monitorea coordinación y administración del aplicativo.';
    if (this.isTeacher()) return 'Revisa los proyectos que tienes asignados.';
    if (this.isStudent()) return 'Gestiona tu avance de proyecto y prepara tu envío a revisión.';
    return 'Panel principal del proceso de titulación.';
  }

  completitudDocumento(): number {
    const doc = this.documentoEstudiante;
    if (!doc) return 0;

    const fields = [
      doc.titulo,
      doc.resumen,
      doc.introduccion,
      doc.metodologia,
      doc.resultados,
      doc.conclusiones,
      doc.bibliografia
    ];

    const completados = fields.filter((f) => (f ?? '').toString().trim().length > 0).length;
    return Math.round((completados / fields.length) * 100);
  }

  formatFechaSustentacion(): string {
    const fecha = this.documentoEstudiante?.fechaSustentacion;
    if (!fecha) return 'Por definir';

    const value = new Date(`${fecha}T00:00:00`);
    return Number.isNaN(value.getTime())
      ? fecha
      : value.toLocaleDateString('es-EC', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
  }

  formatHoraSustentacion(): string {
    const hora = this.documentoEstudiante?.horaSustentacion;
    if (!hora) return 'Por definir';
    return hora.slice(0, 5);
  }

  recomendacionesPreview(): string {
    const contenido = this.documentoEstudiante?.recomendaciones;
    if (!contenido) {
      return 'Aún no has registrado recomendaciones en tu documento. Añádelas para tener una guía clara antes de la defensa.';
    }

    const textoPlano = contenido
      .replace(/<[^>]*>/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();

    if (!textoPlano) {
      return 'Aún no has registrado recomendaciones en tu documento. Añádelas para tener una guía clara antes de la defensa.';
    }

    return textoPlano.length > 220 ? `${textoPlano.slice(0, 220)}...` : textoPlano;
  }

  private cargarDashboardCoordinacion(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService.getResumen().subscribe({
      next: (resumen) => {
        this.resumen = resumen;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el resumen del dashboard.';
        this.loading = false;
      }
    });

    this.dashboardService.getDetalle().subscribe({
      next: (detalle) => (this.detalle = detalle),
      error: () => (this.detalle = { alertas: [], actividades: [] })
    });
  }

  private cargarDashboardEstudiante(): void {
    const idEstudiante = getSessionEntityId(this.user(), 'estudiante');
    if (!idEstudiante) {
      this.error = 'No se pudo identificar el estudiante autenticado.';
      return;
    }

    this.loading = true;
    this.documentoService.getDocumento(idEstudiante).subscribe({
      next: (doc) => {
        this.documentoEstudiante = doc;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudo cargar tu documento de titulación.';
        this.loading = false;
      }
    });
  }

  private cargarDashboardDocente(): void {
    const idDocente = getSessionEntityId(this.user(), 'docente');
    if (!idDocente) {
      this.error = 'No se pudo identificar el docente autenticado.';
      return;
    }

    this.loading = true;
    this.revisionService.pendientes(idDocente).subscribe({
      next: (items) => {
        this.pendientesDocente = items ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'No se pudo cargar los proyectos pendientes.';
        this.loading = false;
      }
    });
  }
}
