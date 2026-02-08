import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-tutorias-control',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Control de tutorías</h1>
        <p>Seguimiento del cumplimiento de cronograma y registros.</p>
      </header>

      <div class="card">
        <table>
          <thead>
            <tr>
              <th>Proyecto</th>
              <th>Tutorías registradas</th>
              <th>Cumplimiento</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Gestión de calidad educativa</td>
              <td>5</td>
              <td><span class="tag ok">En regla</span></td>
              <td>
                <button type="button">Ver tutorías</button>
              </td>
            </tr>
            <tr>
              <td>Modelo de evaluación institucional</td>
              <td>1</td>
              <td><span class="tag warn">Incumplimiento</span></td>
              <td>
                <button type="button">Registrar incumplimiento</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Registrar observación administrativa</h3>
        <div class="form">
          <div class="field">
            <label>Proyecto</label>
            <input type="text" placeholder="Seleccionar proyecto" />
          </div>
          <div class="field">
            <label>Observación</label>
            <textarea rows="3" placeholder="Describir incumplimiento"></textarea>
          </div>
          <button type="button">Guardar observación</button>
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
        padding: 0.7rem 0.4rem;
        border-bottom: 1px solid #e5e7eb;
      }

      th {
        color: #0f7a3a;
      }

      .tag {
        display: inline-flex;
        padding: 0.25rem 0.6rem;
        border-radius: 999px;
        font-weight: 600;
        font-size: 0.8rem;
      }

      .tag.ok {
        background: #e9f6ef;
        color: #0f7a3a;
      }

      .tag.warn {
        background: #fef3c7;
        color: #92400e;
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

      .form {
        display: grid;
        gap: 1rem;
      }

      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }

      input,
      textarea {
        border: 1px solid #e5e7eb;
        border-radius: 0.6rem;
        padding: 0.5rem 0.6rem;
        font-size: 0.9rem;
      }
    `
  ]
})
export class TutoriasControlComponent {}
