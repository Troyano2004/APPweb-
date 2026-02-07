import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardDetalle, DashboardService, DashboardResumen } from '../../services/DashboardService';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1>Resumen general</h1>
        <p>Panel principal del proceso de titulación con métricas clave y alertas.</p>
      </div>

      <div class="status" *ngIf="loading">Cargando resumen...</div>
      <div class="status error" *ngIf="error">{{ error }}</div>

      <div class="stats" *ngIf="!loading && resumen">
        <article class="card">
          <h3>Propuestas en revisión</h3>
          <strong>{{ resumen.propuestasPendientes }}</strong>
          <span>Estado EN_REVISION</span>
        </article>
        <article class="card">
          <h3>Tutorías activas</h3>
          <strong>{{ resumen.tutoriasActivas }}</strong>
          <span>Registro actual</span>
        </article>
        <article class="card">
          <h3>Proyectos aprobados</h3>
          <strong>{{ resumen.proyectosAprobados }}</strong>
          <span>Estado FINALIZADO</span>
        </article>
        <article class="card">
          <h3>Documentos pendientes</h3>
          <strong>{{ resumen.documentosPendientes }}</strong>
          <span>Correcciones requeridas</span>
        </article>
      </div>

      <div class="grid">
        <article class="card large">
          <h3>Alertas rápidas</h3>
          <ul *ngIf="detalle?.alertas?.length; else emptyAlertas">
            <li *ngFor="let alerta of detalle?.alertas">{{ alerta.mensaje }}</li>
          </ul>
          <ng-template #emptyAlertas>
            <p class="empty">Sin alertas disponibles.</p>
          </ng-template>
        </article>
        <article class="card large">
          <h3>Actividades recientes</h3>
          <ul *ngIf="detalle?.actividades?.length; else emptyActividades">
            <li *ngFor="let actividad of detalle?.actividades">
              {{ actividad.mensaje }}
              <span *ngIf="actividad.fecha">({{ actividad.fecha | date: 'short' }})</span>
            </li>
          </ul>
          <ng-template #emptyActividades>
            <p class="empty">Sin actividades recientes.</p>
          </ng-template>
        </article>
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

      .page-header h1 {
        margin: 0 0 0.4rem;
        font-size: 1.6rem;
      }

      .page-header p {
        margin: 0;
        color: #6b7280;
      }

      .status {
        padding: 0.75rem 1rem;
        border-radius: 0.75rem;
        background: #ffffff;
        border: 1px solid #e5e7eb;
        color: #1f2937;
        font-weight: 600;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
      }

      .status.error {
        color: #b42318;
        border-color: #fecaca;
        background: #fff5f5;
      }

      .stats {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
      }

      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 1rem;
      }

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
      }

      .card strong {
        display: block;
        font-size: 2rem;
        margin: 0.5rem 0;
        color: #0f7a3a;
      }

      .card span {
        color: #6b7280;
        font-size: 0.85rem;
      }

      .card.large ul {
        margin: 0.8rem 0 0;
        padding-left: 1.2rem;
        color: #4b5563;
      }

      .empty {
        margin: 0.8rem 0 0;
        color: #6b7280;
        font-size: 0.9rem;
      }
    `
  ]
})
export class DashboardComponent implements OnInit {
  resumen: DashboardResumen | null = null;
  detalle: DashboardDetalle | null = null;
  loading = false;
  error = '';

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.cargarResumen();
  }

  cargarResumen(): void {
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
      next: (detalle) => {
        this.detalle = detalle;
      },
      error: () => {
        this.detalle = { alertas: [], actividades: [] };
      }
    });
  }
}
