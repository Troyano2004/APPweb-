import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoordinadorService, SeguimientoProyecto } from '../../../services/coordinador';

@Component({
  selector: 'app-validacion-coordinacion',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Validación administrativa</h1>
        <p>Validar proyectos que cumplieron los requisitos institucionales.</p>
      </header>

      <div class="card">
        <table>
          <thead>
          <tr>
            <th>Proyecto</th>
            <th>Director</th>
            <th>Documento completo</th>
            <th>Tutorías</th>
            <th>Acción</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let proyecto of proyectos">
            <td>{{ proyecto.tituloProyecto }}</td>
            <td>{{ proyecto.director || 'Sin director' }}</td>
            <td>
                <span class="tag" [class.ok]="esProyectoCompleto(proyecto)" [class.warn]="!esProyectoCompleto(proyecto)">
                  {{ esProyectoCompleto(proyecto) ? 'Completo' : 'Pendiente' }}
                </span>
            </td>
            <td>
                <span class="tag" [class.ok]="esProyectoCompleto(proyecto)" [class.warn]="!esProyectoCompleto(proyecto)">
                  {{ esProyectoCompleto(proyecto) ? 'Registradas' : 'Incompleto' }}
                </span>
            </td>
            <td>
              <button type="button" (click)="validarProyecto(proyecto)" [disabled]="!esProyectoCompleto(proyecto)">
                Validar
              </button>
            </td>
          </tr>
          <tr *ngIf="proyectos.length === 0">
            <td colspan="5" class="empty">No hay proyectos pendientes de validación.</td>
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

      button:disabled {
        background: #d1d5db;
        cursor: not-allowed;
      }

      .empty {
        text-align: center;
        color: #6b7280;
        padding: 1rem 0;
      }
    `
  ]
})
export class ValidacionComponent implements OnInit {
  proyectos: SeguimientoProyecto[] = [];

  constructor(private coordinadorService: CoordinadorService) {}

  ngOnInit(): void {
    this.cargarProyectos();
  }

  cargarProyectos(): void {
    this.coordinadorService.getSeguimiento().subscribe((data) => {
      this.proyectos = data;
    });
  }

  esProyectoCompleto(proyecto: SeguimientoProyecto): boolean {
    return (proyecto.avance ?? 0) >= 100;
  }

  validarProyecto(proyecto: SeguimientoProyecto): void {
    this.coordinadorService.validarProyecto(proyecto.idProyecto).subscribe(() => {
      this.cargarProyectos();
    });
  }
}
