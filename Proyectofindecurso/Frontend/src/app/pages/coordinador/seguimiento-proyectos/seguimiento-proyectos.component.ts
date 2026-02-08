import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoordinadorService, SeguimientoProyecto } from '../../../services/coordinador';

@Component({
  selector: 'app-seguimiento-proyectos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Seguimiento de proyectos</h1>
        <p>Vista general del avance y estado administrativo de Titulación II.</p>
      </header>

      <div class="filters card">
        <div class="field">
          <label>Estado</label>
          <select [(ngModel)]="estadoFiltro">
            <option value="Todos">Todos</option>
            <option *ngFor="let estado of estadosDisponibles" [value]="estado">{{ estado }}</option>
          </select>
        </div>
        <div class="field">
          <label>Buscar</label>
          <input
            type="text"
            placeholder="Nombre del estudiante o título"
            [(ngModel)]="textoFiltro"
          />
        </div>
        <button type="button" (click)="aplicarFiltros()">Aplicar filtros</button>
      </div>

      <div class="card">
        <table>
          <thead>
            <tr>
              <th>Estudiante</th>
              <th>Título del proyecto</th>
              <th>Director</th>
              <th>Estado</th>
              <th>Última revisión</th>
              <th>Avance</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let seguimiento of seguimientosFiltrados">
              <td>{{ seguimiento.estudiante }}</td>
              <td>{{ seguimiento.tituloProyecto }}</td>
              <td>{{ seguimiento.director || 'Sin director' }}</td>
              <td>
                <span class="status" [class.warning]="esEstadoPendiente(seguimiento.estado)">
                  {{ seguimiento.estado || 'Sin estado' }}
                </span>
              </td>
              <td>{{ formatFecha(seguimiento.ultimaRevision) }}</td>
              <td>{{ seguimiento.avance ?? 0 }}%</td>
              <td class="actions">
                <button type="button">Ver proyecto</button>
                <button type="button">Historial</button>
                <button type="button">Tutorías</button>
                <button type="button">Observación</button>
              </td>
            </tr>
            <tr *ngIf="seguimientosFiltrados.length === 0">
              <td colspan="7" class="empty">No hay proyectos para mostrar.</td>
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
        margin: 0 0 0.4rem;
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

      .filters {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 1rem;
        align-items: end;
      }

      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }

      label {
        font-weight: 600;
        color: #0f7a3a;
      }

      input,
      select {
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

      table {
        width: 100%;
        border-collapse: collapse;
      }

      th,
      td {
        text-align: left;
        padding: 0.75rem 0.4rem;
        border-bottom: 1px solid #e5e7eb;
        font-size: 0.92rem;
      }

      th {
        color: #0f7a3a;
      }

      .status {
        display: inline-flex;
        padding: 0.25rem 0.55rem;
        border-radius: 999px;
        background: #e9f6ef;
        color: #0f7a3a;
        font-weight: 600;
      }

      .status.warning {
        background: #fef3c7;
        color: #92400e;
      }

      .actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .actions button {
        background: #ffffff;
        color: #0f7a3a;
        border: 1px solid #d1d5db;
        box-shadow: none;
      }

      .empty {
        text-align: center;
        color: #6b7280;
        padding: 1rem 0;
      }
    `
  ]
})
export class SeguimientoProyectosComponent implements OnInit {
  seguimientos: SeguimientoProyecto[] = [];
  seguimientosFiltrados: SeguimientoProyecto[] = [];
  estadoFiltro = 'Todos';
  textoFiltro = '';

  constructor(private coordinadorService: CoordinadorService) {}

  get estadosDisponibles(): string[] {
    const estados = new Set(this.seguimientos.map((item) => item.estado).filter(Boolean) as string[]);
    return Array.from(estados);
  }

  ngOnInit(): void {
    this.cargarSeguimiento();
  }

  cargarSeguimiento(): void {
    this.coordinadorService.getSeguimiento().subscribe((data) => {
      this.seguimientos = data;
      this.aplicarFiltros();
    });
  }

  aplicarFiltros(): void {
    const texto = this.textoFiltro.trim().toLowerCase();
    this.seguimientosFiltrados = this.seguimientos.filter((item) => {
      const coincideEstado = this.estadoFiltro === 'Todos' || item.estado === this.estadoFiltro;
      const coincideTexto =
        !texto ||
        item.estudiante.toLowerCase().includes(texto) ||
        item.tituloProyecto.toLowerCase().includes(texto);
      return coincideEstado && coincideTexto;
    });
  }

  esEstadoPendiente(estado?: string | null): boolean {
    if (!estado) {
      return true;
    }
    const estadoLower = estado.toLowerCase();
    return estadoLower.includes('pendiente') || estadoLower.includes('revisión') || estadoLower.includes('atras');
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
