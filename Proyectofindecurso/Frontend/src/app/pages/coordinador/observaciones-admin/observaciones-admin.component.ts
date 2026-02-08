import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoordinadorService, ObservacionAdministrativa } from '../../../services/coordinador';

@Component({
  selector: 'app-observaciones-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Observaciones administrativas</h1>
        <p>Registro de observaciones por proyecto, estudiante o director.</p>
      </header>

      <div class="card">
        <table>
          <thead>
          <tr>
            <th>Proyecto</th>
            <th>Tipo</th>
            <th>Responsable</th>
            <th>Fecha</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let observacion of observaciones">
            <td>{{ observacion.proyecto }}</td>
            <td><span class="tag">{{ observacion.tipo }}</span></td>
            <td>{{ observacion.creadoPor || 'Coordinación' }}</td>
            <td>{{ formatFecha(observacion.creadoEn) }}</td>
          </tr>
          <tr *ngIf="observaciones.length === 0">
            <td colspan="4" class="empty">No hay observaciones registradas.</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Registrar observación</h3>
        <div class="form">
          <div class="field">
            <label>Proyecto / estudiante / director</label>
            <input type="text" placeholder="ID de proyecto" [(ngModel)]="formulario.idProyecto" />
          </div>
          <div class="field">
            <label>Tipo</label>
            <select [(ngModel)]="formulario.tipo">
              <option value="Retraso">Retraso</option>
              <option value="Incumplimiento">Incumplimiento</option>
              <option value="Administrativo">Administrativo</option>
            </select>
          </div>
          <div class="field">
            <label>Detalle</label>
            <textarea rows="3" placeholder="Detalle de la observación" [(ngModel)]="formulario.detalle"></textarea>
          </div>
          <button type="button" (click)="guardarObservacion()">Guardar observación</button>
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
        background: #e9f6ef;
        color: #0f7a3a;
        font-weight: 600;
        font-size: 0.8rem;
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
      select,
      textarea {
        border: 1px solid #e5e7eb;
        border-radius: 0.6rem;
        padding: 0.5rem 0.6rem;
        font-size: 0.9rem;
      }

      button {
        border: none;
        border-radius: 0.6rem;
        padding: 0.55rem 0.9rem;
        background: linear-gradient(135deg, #0f7a3a 0%, #0c6a32 100%);
        color: #ffffff;
        cursor: pointer;
        font-weight: 600;
      }

      .empty {
        text-align: center;
        color: #6b7280;
        padding: 1rem 0;
      }
    `
  ]
})
export class ObservacionesAdminComponent implements OnInit {
  observaciones: ObservacionAdministrativa[] = [];
  formulario: { idProyecto: number | null; tipo: string; detalle: string } = {
    idProyecto: null,
    tipo: 'Retraso',
    detalle: ''
  };

  constructor(private coordinadorService: CoordinadorService) {}

  ngOnInit(): void {
    this.cargarObservaciones();
  }

  cargarObservaciones(): void {
    this.coordinadorService.getObservaciones().subscribe((data) => {
      this.observaciones = data;
    });
  }

  guardarObservacion(): void {
    if (!this.formulario.idProyecto || !this.formulario.detalle.trim()) {
      return;
    }
    this.coordinadorService
      .crearObservacion({
        idProyecto: this.formulario.idProyecto,
        tipo: this.formulario.tipo,
        detalle: this.formulario.detalle,
        creadoPor: 'Coordinación'
      })
      .subscribe(() => {
        this.formulario = { idProyecto: null, tipo: 'Retraso', detalle: '' };
        this.cargarObservaciones();
      });
  }

  formatFecha(fecha?: string | null): string {
    if (!fecha) {
      return 'Sin fecha';
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
