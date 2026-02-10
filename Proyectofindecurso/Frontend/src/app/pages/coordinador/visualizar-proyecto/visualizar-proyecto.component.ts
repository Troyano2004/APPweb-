import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  CoordinadorService,
  ObservacionAdministrativa,
  SeguimientoProyecto
} from '../../../services/coordinador';

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
            <h2>{{ proyecto?.tituloProyecto || 'Proyecto sin título' }}</h2>
            <p class="subtitle">
              {{ proyecto?.estudiante || 'Sin estudiante asignado' }} ·
              {{ proyecto?.director || 'Sin director' }}
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

      <div class="grid">
        <article class="card">
          <h3>Resumen</h3>
          <p>
            Información consolidada del avance, observaciones y estado de seguimiento.
          </p>
          <ul>
            <li>Estado actual: {{ proyecto?.estado || 'Sin estado' }}.</li>
            <li>Última revisión: {{ formatFecha(proyecto?.ultimaRevision) }}.</li>
            <li>Avance reportado: {{ proyecto?.avance ?? 0 }}%.</li>
          </ul>
        </article>
        <article class="card">
          <h3>Próximas acciones</h3>
          <ol>
            <li>Confirmar director asignado.</li>
            <li>Revisar observaciones pendientes.</li>
            <li>Programar tutoría de seguimiento.</li>
          </ol>
          <button type="button">Registrar acción</button>
        </article>
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

      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 1rem;
      }

      ul,
      ol {
        margin: 0.75rem 0 0;
        padding-left: 1.2rem;
        color: #4b5563;
      }

      button {
        margin-top: 1rem;
        border: none;
        border-radius: 0.6rem;
        padding: 0.55rem 0.9rem;
        background: linear-gradient(135deg, #0f7a3a 0%, #0c6a32 100%);
        color: #ffffff;
        cursor: pointer;
        font-weight: 600;
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
    `
  ]
})
export class VisualizarProyectoComponent implements OnInit {
  idProyecto: number | null = null;
  proyecto: SeguimientoProyecto | null = null;
  observaciones: ObservacionAdministrativa[] = [];

  constructor(
    private route: ActivatedRoute,
    private coordinadorService: CoordinadorService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const id = params.get('idProyecto');
      this.idProyecto = id ? Number(id) : null;
      this.cargarProyecto();
      this.cargarObservaciones();
    });
  }

  cargarProyecto(): void {
    if (!this.idProyecto) {
      this.proyecto = null;
      return;
    }
    this.coordinadorService.getSeguimiento().subscribe((data) => {
      this.proyecto = data.find((item) => item.idProyecto === this.idProyecto) ?? null;
    });
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
