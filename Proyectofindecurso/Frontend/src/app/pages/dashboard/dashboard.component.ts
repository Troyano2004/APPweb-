import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

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

      <div class="stats">
        <article class="card">
          <h3>Propuestas en revisión</h3>
          <strong>18</strong>
          <span>Últimos 7 días</span>
        </article>
        <article class="card">
          <h3>Tutorías activas</h3>
          <strong>42</strong>
          <span>Periodo vigente</span>
        </article>
        <article class="card">
          <h3>Proyectos aprobados</h3>
          <strong>7</strong>
          <span>Este mes</span>
        </article>
        <article class="card">
          <h3>Documentos pendientes</h3>
          <strong>12</strong>
          <span>Secciones críticas</span>
        </article>
      </div>

      <div class="grid">
        <article class="card large">
          <h3>Alertas rápidas</h3>
          <ul>
            <li>3 propuestas requieren asignación de tutor.</li>
            <li>2 documentos en revisión final sin comentarios.</li>
            <li>1 legalización pendiente de firma.</li>
          </ul>
        </article>
        <article class="card large">
          <h3>Actividades recientes</h3>
          <ul>
            <li>Se aprobó la propuesta de Est. A-014.</li>
            <li>Se actualizó el checklist legal del proyecto B-122.</li>
            <li>Se registró una nueva tutoría para Est. C-218.</li>
          </ul>
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
    `
  ]
})
export class DashboardComponent {}
