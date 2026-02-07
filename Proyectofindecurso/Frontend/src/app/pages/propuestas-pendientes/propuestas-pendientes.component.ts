import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-propuestas-pendientes',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Propuestas pendientes</h1>
        <p>Listado de propuestas que esperan revisión de comité y asignación de tutor.</p>
      </header>

      <div class="card">
        <table>
          <thead>
            <tr>
              <th>Estudiante</th>
              <th>Programa</th>
              <th>Fecha</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>María Intriago</td>
              <td>Ingeniería en Sistemas</td>
              <td>12/05/2024</td>
              <td><span class="status">Pendiente</span></td>
            </tr>
            <tr>
              <td>Carlos Mena</td>
              <td>Contabilidad</td>
              <td>10/05/2024</td>
              <td><span class="status">En evaluación</span></td>
            </tr>
            <tr>
              <td>Andrea Ruiz</td>
              <td>Administración</td>
              <td>08/05/2024</td>
              <td><span class="status">Pendiente</span></td>
            </tr>
          </tbody>
        </table>
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

      table {
        width: 100%;
        border-collapse: collapse;
      }

      th,
      td {
        text-align: left;
        padding: 0.75rem 0.4rem;
        border-bottom: 1px solid #e5e7eb;
        font-size: 0.95rem;
      }

      th {
        color: #0f7a3a;
        font-weight: 600;
      }

      .status {
        display: inline-flex;
        padding: 0.3rem 0.6rem;
        border-radius: 999px;
        background: #e9f6ef;
        color: #0f7a3a;
        font-weight: 600;
        font-size: 0.8rem;
      }
    `
  ]
})
export class PropuestasPendientesComponent {}
