import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-reportes-coordinacion',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Reportes de coordinación</h1>
        <p>Generación de reportes institucionales para Titulación II.</p>
      </header>

      <div class="grid">
        <article class="card">
          <h3>Proyectos por estado</h3>
          <p>Distribución general y exportación.</p>
          <button type="button">Exportar PDF</button>
          <button type="button">Exportar Excel</button>
        </article>
        <article class="card">
          <h3>Directores y carga académica</h3>
          <p>Seguimiento de asignaciones y carga.</p>
          <button type="button">Exportar PDF</button>
          <button type="button">Exportar Excel</button>
        </article>
        <article class="card">
          <h3>Proyectos atrasados</h3>
          <p>Listado de alertas por incumplimientos.</p>
          <button type="button">Exportar PDF</button>
          <button type="button">Exportar Excel</button>
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
        margin: 0 0 0.35rem;
      }

      .page-header p {
        margin: 0;
        color: #6b7280;
      }

      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 1rem;
      }

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
        display: grid;
        gap: 0.75rem;
      }

      button {
        border: none;
        border-radius: 0.6rem;
        padding: 0.5rem 0.9rem;
        background: linear-gradient(135deg, #0f7a3a 0%, #0c6a32 100%);
        color: #ffffff;
        cursor: pointer;
        font-weight: 600;
      }
    `
  ]
})
export class ReportesCoordinacionComponent {}
