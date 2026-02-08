import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CoordinadorService, DirectorCarga, EstudianteSinDirector } from '../../../services/coordinador';

@Component({
  selector: 'app-directores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Asignación y control de directores</h1>
        <p>Gestione estudiantes sin director y la carga de directores.</p>
      </header>

      <div class="grid">
        <article class="card">
          <h3>Estudiantes sin director</h3>
          <ul>
            <li *ngFor="let estudiante of estudiantesSinDirector">
              <strong>{{ estudiante.estudiante }}</strong>
              <span>{{ estudiante.carrera || 'Carrera no registrada' }}</span>
              <button type="button" (click)="seleccionarEstudiante(estudiante)">Asignar director</button>
            </li>
            <li *ngIf="estudiantesSinDirector.length === 0" class="empty">
              No hay estudiantes sin director asignado.
            </li>
          </ul>
        </article>
        <article class="card">
          <h3>Carga de directores</h3>
          <table>
            <thead>
              <tr>
                <th>Director</th>
                <th>Proyectos</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let director of cargaDirectores">
                <td>{{ director.director }}</td>
                <td>{{ director.proyectosAsignados }}</td>
                <td><button type="button" (click)="seleccionarDirector(director)">Reasignar</button></td>
              </tr>
              <tr *ngIf="cargaDirectores.length === 0">
                <td colspan="3" class="empty">No hay carga de directores disponible.</td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>

      <div class="card">
        <h3>Cambiar director</h3>
        <div class="form">
          <div class="field">
            <label>Estudiante / proyecto</label>
            <input
              type="text"
              placeholder="Seleccionar estudiante"
              [(ngModel)]="asignacion.idDocumento"
            />
          </div>
          <div class="field">
            <label>Nuevo director</label>
            <input
              type="text"
              placeholder="Seleccionar director"
              [(ngModel)]="asignacion.idDocente"
            />
          </div>
          <div class="field">
            <label>Motivo del cambio</label>
            <textarea rows="3" placeholder="Describir motivo" [(ngModel)]="asignacion.motivo"></textarea>
          </div>
          <button type="button" (click)="guardarAsignacion()">Guardar cambio</button>
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

      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 1rem;
      }

      .card {
        background: #ffffff;
        border: 1px solid #e5e7eb;
        border-radius: 0.75rem;
        padding: 1.2rem;
        box-shadow: 0 12px 24px rgba(15, 122, 58, 0.08);
      }

      ul {
        list-style: none;
        padding: 0;
        margin: 0.8rem 0 0;
        display: grid;
        gap: 0.75rem;
      }

      li {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }

      li span {
        color: #6b7280;
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

      table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 0.6rem;
      }

      th,
      td {
        text-align: left;
        padding: 0.6rem 0.4rem;
        border-bottom: 1px solid #e5e7eb;
      }

      th {
        color: #0f7a3a;
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

      .empty {
        color: #6b7280;
        font-size: 0.9rem;
      }
    `
  ]
})
export class DirectoresComponent implements OnInit {
  estudiantesSinDirector: EstudianteSinDirector[] = [];
  cargaDirectores: DirectorCarga[] = [];
  asignacion: { idDocumento: number | null; idDocente: number | null; motivo: string } = {
    idDocumento: null,
    idDocente: null,
    motivo: ''
  };

  constructor(private coordinadorService: CoordinadorService) {}

  ngOnInit(): void {
    this.cargarListas();
  }

  cargarListas(): void {
    this.coordinadorService.getEstudiantesSinDirector().subscribe((data) => {
      this.estudiantesSinDirector = data;
    });
    this.coordinadorService.getCargaDirectores().subscribe((data) => {
      this.cargaDirectores = data;
    });
  }

  seleccionarEstudiante(estudiante: EstudianteSinDirector): void {
    this.asignacion.idDocumento = estudiante.idDocumento;
  }

  seleccionarDirector(director: DirectorCarga): void {
    this.asignacion.idDocente = director.idDocente;
  }

  guardarAsignacion(): void {
    if (!this.asignacion.idDocumento || !this.asignacion.idDocente || !this.asignacion.motivo.trim()) {
      return;
    }
    this.coordinadorService
      .asignarDirector({
        idDocumento: this.asignacion.idDocumento,
        idDocente: this.asignacion.idDocente,
        motivo: this.asignacion.motivo
      })
      .subscribe(() => {
        this.asignacion = { idDocumento: null, idDocente: null, motivo: '' };
        this.cargarListas();
      });
  }
}
