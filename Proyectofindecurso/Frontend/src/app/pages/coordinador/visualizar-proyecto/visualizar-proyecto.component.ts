// Proyectofindecurso/Frontend/src/app/pages/coordinador/visualizar-proyecto/visualizar-proyecto.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  CoordinadorService,
  ObservacionAdministrativa,
  SeguimientoProyecto
} from '../../../services/coordinador';
import { DocumentoTitulacionDto, DocumentoTitulacionService } from '../../../services/documento-titulacion';
import { catchError, finalize, of, timeout } from 'rxjs';

@Component({
  selector: 'app-visualizar-proyecto',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Visualización de proyecto</h1>
          <p>Resumen ejecutivo y estado del proyecto para coordinación.</p>
        </div>
        <a class="back" routerLink="/coordinador/seguimiento">Volver al seguimiento</a>
      </header>

      <div class="hero card">
        <div class="hero-content">
          <div>
            <span class="label">Proyecto</span>
            <h2>{{ proyecto?.tituloProyecto || documento?.titulo || 'Proyecto sin título' }}</h2>
            <p class="subtitle">
              {{ proyecto?.estudiante || estudianteLabel() }} ·
              {{ proyecto?.director || directorLabel() }}
            </p>
          </div>
          <div class="pill">Estado: {{ proyecto?.estado || 'En seguimiento' }}</div>
        </div>
        <div class="hero-grid">
          <div class="stat">
            <span>Avance</span>
            <strong>{{ proyecto?.avance ?? 0 }}%</strong>
          </div>
          <div class="stat">
            <span>Última revisión</span>
            <strong>{{ formatFecha(proyecto?.ultimaRevision) }}</strong>
          </div>
          <div class="stat">
            <span>Director</span>
            <strong>{{ proyecto?.director || 'Sin asignar' }}</strong>
          </div>
          <div class="stat">
            <span>Tutorías</span>
            <strong>{{ proyecto?.avance ? 'Registradas' : 'Pendientes' }}</strong>
          </div>
        </div>
      </div>

      <div class="card">
        <h3>Contenido del documento</h3>
        <p *ngIf="cargandoDocumento" class="doc-help">Cargando documento...</p>
        <p *ngIf="errorDocumento" class="error">{{ errorDocumento }}</p>
        <p class="doc-help">Se muestran todas las secciones registradas del documento de titulación.</p>
        <div class="doc-sections" *ngIf="documento; else sinDocumento">
          <article class="doc-item" *ngFor="let seccion of seccionesDocumento()">
            <h4>{{ seccion.titulo }}</h4>
            <p>{{ seccion.contenido }}</p>
          </article>
        </div>
        <ng-template #sinDocumento>
          <p class="empty">No hay documento cargado para este proyecto.</p>
        </ng-template>
      </div>

      <div class="card">
        <h3>Observaciones recientes</h3>
        <div class="timeline">
          <div *ngFor="let observacion of observaciones" class="timeline-item">
            <span class="dot"></span>
            <div>
              <strong>{{ observacion.tipo }}</strong>
              <p>{{ observacion.detalle }}</p>
            </div>
            <span class="date">{{ formatFecha(observacion.creadoEn) }}</span>
          </div>
          <p *ngIf="observaciones.length === 0" class="empty">No hay observaciones registradas.</p>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .page {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 1rem;
      }

      .page-header h1 {
        margin: 0 0 0.35rem;
      }

      .page-header p {
        margin: 0;
        color: #6b7280;
      }

      .back {
        text-decoration: none;
        color: #0f7a3a;
        font-weight: 600;
        border: 1px solid #d1d5db;
        padding: 0.45rem 0.85rem;
        border-radius: 0.6rem;
        background: #ffffff;
      }

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
      }

      .hero {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        background: linear-gradient(135deg, rgba(15, 122, 58, 0.12), rgba(15, 122, 58, 0.02));
      }

      .hero-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
      }

      .hero h2 {
        margin: 0.4rem 0 0.25rem;
      }

      .subtitle {
        margin: 0;
        color: #4b5563;
      }

      .label {
        font-size: 0.85rem;
        font-weight: 600;
        color: #0f7a3a;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }

      .pill {
        padding: 0.35rem 0.8rem;
        border-radius: 999px;
        background: #e9f6ef;
        color: #0f7a3a;
        font-weight: 600;
      }

      .hero-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 0.75rem;
      }

      .stat {
        background: #ffffff;
        border-radius: 0.7rem;
        border: 1px solid #e5e7eb;
        padding: 0.75rem;
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }

      .stat span {
        color: #6b7280;
        font-size: 0.85rem;
      }

      .stat strong {
        font-size: 1.05rem;
      }

      .doc-help {
        margin-top: 0;
        color: #6b7280;
      }

      .doc-sections {
        display: grid;
        gap: 0.85rem;
      }

      .doc-item {
        border: 1px solid #e5e7eb;
        border-radius: 0.65rem;
        padding: 0.85rem;
        background: #f9fafb;
      }

      .doc-item h4 {
        margin: 0 0 0.35rem;
      }

      .doc-item p {
        margin: 0;
        color: #374151;
        white-space: pre-wrap;
      }

      .timeline {
        display: grid;
        gap: 1rem;
      }

      .timeline-item {
        display: grid;
        grid-template-columns: auto 1fr auto;
        gap: 0.75rem;
        align-items: start;
      }

      .dot {
        width: 0.65rem;
        height: 0.65rem;
        background: #0f7a3a;
        border-radius: 50%;
        margin-top: 0.35rem;
      }

      .date {
        color: #6b7280;
        font-size: 0.85rem;
        white-space: nowrap;
      }

      .empty {
        color: #6b7280;
        margin: 0;
      }

      .error {
        margin: 0.5rem 0;
        color: #b91c1c;
        font-weight: 600;
      }
    `
  ]
})
export class VisualizarProyectoComponent implements OnInit {
  idProyecto: number | null = null;
  idEstudianteQuery: number | null = null;
  proyecto: SeguimientoProyecto | null = null;
  documento: DocumentoTitulacionDto | null = null;
  observaciones: ObservacionAdministrativa[] = [];
  cargandoDocumento = false;
  errorDocumento: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private coordinadorService: CoordinadorService,
    private documentoService: DocumentoTitulacionService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const id = params.get('idProyecto');
      this.idProyecto = id ? Number(id) : null;
      const idEst = params.get('idEstudiante');
      this.idEstudianteQuery = idEst ? Number(idEst) : null;
      this.cargarProyectoYDocumento();
      this.cargarObservaciones();
    });
  }

  cargarProyectoYDocumento(): void {
    if (!this.idProyecto && !this.idEstudianteQuery) {
      this.proyecto = null;
      this.documento = null;
      return;
    }

    if (!this.idProyecto && this.idEstudianteQuery) {
      this.proyecto = null;
      this.cargarDocumento(this.idEstudianteQuery);
      return;
    }

    this.coordinadorService.getSeguimiento().subscribe({
      next: (data) => {
        this.proyecto =
          data.find((item) => item.idProyecto === this.idProyecto) ??
          data.find((item) => String(item.idProyecto) === String(this.idProyecto)) ??
          null;

        this.cargarDocumento(this.proyecto?.idEstudiante ?? this.idEstudianteQuery);
      },
      error: () => {
        this.proyecto = null;
        this.cargarDocumento(this.idEstudianteQuery);
      }
    });
  }

  cargarDocumento(idEstudiante: number | null): void {
    if (!this.idProyecto && !idEstudiante) {
      this.documento = null;
      this.cargandoDocumento = false;
      return;
    }

    this.cargandoDocumento = true;
    this.errorDocumento = null;

    const fallbackPorEstudiante = () => {
      if (!idEstudiante) {
        return of(null);
      }
      return this.documentoService.getDocumento(idEstudiante).pipe(
        timeout(8000),
        catchError(() => of(null))
      );
    };

    const request$ = this.idProyecto
      ? this.coordinadorService.getDocumentoProyecto(this.idProyecto).pipe(
        timeout(8000),
        catchError(() => fallbackPorEstudiante())
      )
      : fallbackPorEstudiante();

    request$
      .pipe(
        finalize(() => {
          this.cargandoDocumento = false;
        })
      )
      .subscribe((data) => {
        this.documento = data;
        if (!data) {
          this.errorDocumento = 'No se pudo cargar el documento para este proyecto.';
        }
      });
  }

  seccionesDocumento(): Array<{ titulo: string; contenido: string }> {
    if (!this.documento) {
      return [];
    }

    const secciones = [
      { titulo: 'Título', contenido: this.documento.titulo },
      { titulo: 'Resumen', contenido: this.documento.resumen },
      { titulo: 'Abstract', contenido: this.documento.abstractText },
      { titulo: 'Introducción', contenido: this.documento.introduccion },
      { titulo: 'Planteamiento del problema', contenido: this.documento.planteamientoProblema || this.documento.problema },
      { titulo: 'Objetivo general', contenido: this.documento.objetivoGeneral || this.documento.objetivosGenerales },
      { titulo: 'Objetivos específicos', contenido: this.documento.objetivosEspecificos },
      { titulo: 'Justificación', contenido: this.documento.justificacion },
      { titulo: 'Marco teórico', contenido: this.documento.marcoTeorico },
      { titulo: 'Metodología', contenido: this.documento.metodologia },
      { titulo: 'Resultados', contenido: this.documento.resultados },
      { titulo: 'Discusión', contenido: this.documento.discusion },
      { titulo: 'Conclusiones', contenido: this.documento.conclusiones },
      { titulo: 'Recomendaciones', contenido: this.documento.recomendaciones },
      { titulo: 'Bibliografía', contenido: this.documento.bibliografia },
      { titulo: 'Anexos', contenido: this.documento.anexos }
    ];

    return secciones.filter((item): item is { titulo: string; contenido: string } => !!item.contenido?.trim());
  }

  cargarObservaciones(): void {
    if (!this.idProyecto) {
      this.observaciones = [];
      return;
    }
    this.coordinadorService.getObservaciones(this.idProyecto).subscribe((data) => {
      this.observaciones = data;
    });
  }

  estudianteLabel(): string {
    if (this.proyecto?.estudiante) {
      return this.proyecto.estudiante;
    }
    return this.documento?.idEstudiante ? `Estudiante #${this.documento.idEstudiante}` : 'Sin estudiante asignado';
  }

  directorLabel(): string {
    if (this.proyecto?.director) {
      return this.proyecto.director;
    }
    return this.documento?.idDirector ? `Director #${this.documento.idDirector}` : 'Sin director';
  }

  formatFecha(fecha?: string | null): string {
    if (!fecha) {
      return 'Sin registro';
    }
    const date = new Date(fecha);
    if (Number.isNaN(date.getTime())) {
      return fecha;
    }
    return new Intl.DateTimeFormat('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date);
  }
}
