import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>{{ title }}</h1>
        <p>Sección en construcción para el módulo seleccionado.</p>
      </header>

      <div class="card">
        <h3>Próximos pasos</h3>
        <ul>
          <li>Definir flujos y responsables.</li>
          <li>Configurar permisos por rol.</li>
          <li>Integrar formularios y validaciones.</li>
        </ul>
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

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
      }

      ul {
        margin: 0.8rem 0 0;
        padding-left: 1.2rem;
        color: #4b5563;
      }
    `
  ]
})
export class PlaceholderPageComponent {
  title = 'Módulo';

  constructor(private readonly route: ActivatedRoute) {
    this.title = this.route.snapshot.data['title'] ?? this.title;
  }
}
