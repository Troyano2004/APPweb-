import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-documento-secciones',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Documento por secciones</h1>
        <p>Gestión del documento de titulación dividido por capítulos y entregables.</p>
      </header>

      <div class="sections">
        <article class="card">
          <h3>Capítulo 1: Planteamiento</h3>
          <p>Objetivo, problema y justificación.</p>
          <button type="button">Editar sección</button>
        </article>
        <article class="card">
          <h3>Capítulo 2: Marco teórico</h3>
          <p>Referentes académicos y estado del arte.</p>
          <button type="button">Editar sección</button>
        </article>
        <article class="card">
          <h3>Capítulo 3: Metodología</h3>
          <p>Diseño, alcance y técnicas de investigación.</p>
          <button type="button">Editar sección</button>
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

      .sections {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 1rem;
      }

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .card h3 {
        margin: 0;
      }

      .card p {
        margin: 0;
        color: #6b7280;
      }

      button {
        align-self: flex-start;
        border: none;
        color: #ffffff;
        background: linear-gradient(135deg, #0f7a3a 0%, #0c6a32 100%);
        padding: 0.45rem 0.9rem;
        border-radius: 0.6rem;
        cursor: pointer;
        box-shadow: 0 8px 16px rgba(15, 122, 58, 0.2);
      }
    `
  ]
})
export class DocumentoSeccionesComponent {}
