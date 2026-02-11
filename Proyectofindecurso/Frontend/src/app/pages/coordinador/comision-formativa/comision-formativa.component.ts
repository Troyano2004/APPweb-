import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogoCarrera, ComisionFormativa, CoordinadorService, DirectorCarga } from '../../../services/coordinador';

interface MiembroSeleccionado {
  idDocente: number | null;
  cargo: string;
}

@Component({
  selector: 'app-comision-formativa',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Comisión formativa</h1>
        <p>Conformación y asignación de comisión para proyectos.</p>
      </header>

      <div class="card">
        <h3>Comisiones registradas</h3>
        <table>
          <thead>
          <tr>
            <th>Carrera</th>
            <th>Período</th>
            <th>Estado</th>
            <th>Miembros</th>
            <th>Acciones</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let comision of comisiones">
            <td>{{ comision.carrera }}</td>
            <td>{{ comision.periodoAcademico }}</td>
            <td>
                <span class="tag" [class.ok]="comision.estado === 'ACTIVA'" [class.warn]="comision.estado !== 'ACTIVA'">
                  {{ comision.estado }}
                </span>
            </td>
            <td>{{ mostrarMiembros(comision) }}</td>
            <td>
              <button type="button" class="danger" (click)="eliminarComision(comision)" [disabled]="guardando">
                Eliminar
              </button>
            </td>
          </tr>
          <tr *ngIf="comisiones.length === 0">
            <td colspan="5" class="empty">No hay comisiones registradas.</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Crear comisión formativa</h3>
        <p class="help" *ngIf="carreraCoordinador">Carrera del coordinador: <strong>{{ carreraCoordinador.nombre }}</strong></p>
        <p *ngIf="mensaje" class="message">{{ mensaje }}</p>
        <p *ngIf="error" class="error">{{ error }}</p>

        <div class="form-grid">
          <div class="field">
            <label>Carrera</label>
            <input type="text" [value]="carreraCoordinador?.nombre || 'No disponible'" disabled />
          </div>
          <div class="field">
            <label>Periodo académico</label>
            <input type="text" placeholder="Ej. 2024-1" [(ngModel)]="nuevaComision.periodoAcademico" />
          </div>
          <div class="field">
            <label>Estado</label>
            <select [(ngModel)]="nuevaComision.estado">
              <option value="ACTIVA">ACTIVA</option>
              <option value="INACTIVA">INACTIVA</option>
            </select>
          </div>
        </div>

        <h4>Docentes de la comisión</h4>
        <div class="member-row" *ngFor="let miembro of miembrosSeleccionados; let i = index">
          <select [(ngModel)]="miembro.idDocente">
            <option [ngValue]="null">Seleccione docente</option>
            <option *ngFor="let docente of docentesDisponibles" [ngValue]="docente.idDocente">
              {{ docente.director }}
            </option>
          </select>

          <select [(ngModel)]="miembro.cargo">
            <option value="PRESIDENTE">PRESIDENTE</option>
            <option value="SECRETARIO">SECRETARIO</option>
            <option value="VOCAL">VOCAL</option>
          </select>

          <button type="button" class="danger" (click)="quitarMiembro(i)" [disabled]="miembrosSeleccionados.length === 1 || guardando">
            Quitar
          </button>
        </div>

        <div class="actions">
          <button type="button" class="secondary" (click)="agregarMiembro()" [disabled]="guardando">
            + Agregar docente
          </button>

          <button type="button" (click)="guardarComision()" [disabled]="guardando || !carreraCoordinador?.idCarrera">
            {{ guardando ? 'Guardando...' : 'Guardar comisión' }}
          </button>
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

      .form-grid {
        display: grid;
        gap: 1rem;
        margin-top: 0.75rem;
      }

      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }

      .member-row {
        display: grid;
        grid-template-columns: 1.6fr 1fr auto;
        gap: 0.6rem;
        margin-bottom: 0.6rem;
      }

      input,
      textarea,
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

      .actions {
        display: flex;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .secondary {
        background: #ffffff;
        color: #0f7a3a;
        border: 1px solid #d1d5db;
      }

      button:disabled {
        cursor: not-allowed;
        opacity: 0.65;
      }

      .danger {
        background: #fee2e2;
        color: #991b1b;
        border: 1px solid #fecaca;
      }

      .empty {
        text-align: center;
        color: #6b7280;
        padding: 1rem 0;
      }

      .help {
        margin: 0.2rem 0 0.8rem;
        color: #374151;
      }

      .message {
        color: #0f7a3a;
        font-weight: 600;
      }

      .error {
        color: #b91c1c;
        font-weight: 600;
      }
    `
  ]
})
export class ComisionFormativaComponent implements OnInit {
  comisiones: ComisionFormativa[] = [];
  carreras: CatalogoCarrera[] = [];
  docentesDisponibles: DirectorCarga[] = [];
  carreraCoordinador: CatalogoCarrera | null = null;

  nuevaComision: { idCarrera: number | null; periodoAcademico: string; estado: string } = {
    idCarrera: null,
    periodoAcademico: '',
    estado: 'ACTIVA'
  };
  miembrosSeleccionados: MiembroSeleccionado[] = [{ idDocente: null, cargo: 'PRESIDENTE' }];
  guardando = false;
  mensaje = '';
  error = '';

  constructor(private coordinadorService: CoordinadorService) {}

  ngOnInit(): void {
    this.cargarCarreraCoordinador();
    this.cargarDocentes();
    this.cargarComisiones();
  }

  cargarCarreraCoordinador(): void {
    this.coordinadorService.getCarreras().subscribe({
      next: (data) => {
        this.carreras = data;
        this.carreraCoordinador = data.length > 0 ? data[0] : null;
        this.nuevaComision.idCarrera = this.carreraCoordinador?.idCarrera ?? null;
      },
      error: () => {
        this.error = 'No se pudo cargar la carrera del coordinador.';
      }
    });
  }

  cargarDocentes(): void {
    this.coordinadorService.getCargaDirectores().subscribe({
      next: (data) => {
        this.docentesDisponibles = data;
      },
      error: () => {
        this.error = 'No se pudo cargar el listado de docentes.';
      }
    });
  }

  cargarComisiones(): void {
    this.coordinadorService.getComisiones().subscribe((data) => {
      this.comisiones = data;
    });
  }

  mostrarMiembros(comision: ComisionFormativa): string {
    if (!comision.miembros || comision.miembros.length === 0) {
      return '--';
    }
    return comision.miembros.map((miembro) => `${miembro.docente} (${miembro.cargo})`).join(', ');
  }

  agregarMiembro(): void {
    this.miembrosSeleccionados.push({ idDocente: null, cargo: 'VOCAL' });
  }

  quitarMiembro(index: number): void {
    if (this.miembrosSeleccionados.length === 1) {
      return;
    }
    this.miembrosSeleccionados.splice(index, 1);
  }

  guardarComision(): void {
    this.mensaje = '';
    this.error = '';

    if (!this.carreraCoordinador?.idCarrera) {
      this.error = 'No se encontró la carrera del coordinador.';
      return;
    }

    if (!this.nuevaComision.periodoAcademico.trim()) {
      this.error = 'Ingrese el periodo académico.';
      return;
    }

    const miembros = this.parsearMiembros();
    if (miembros === null) {
      return;
    }

    this.guardando = true;

    this.coordinadorService
      .crearComision({
        idCarrera: this.carreraCoordinador.idCarrera,
        periodoAcademico: this.nuevaComision.periodoAcademico,
        estado: this.nuevaComision.estado
      })
      .subscribe({
        next: (comision) => {
          if (miembros.length === 0) {
            this.finalizarGuardadoConExito('Comisión guardada correctamente.');
            return;
          }

          this.coordinadorService.asignarMiembros(comision.idComision, miembros).subscribe({
            next: () => {
              this.finalizarGuardadoConExito('Comisión y miembros guardados correctamente.');
            },
            error: () => {
              this.guardando = false;
              this.error = 'Se guardó la comisión, pero falló la asignación de miembros.';
              this.cargarComisiones();
            }
          });
        },
        error: () => {
          this.guardando = false;
          this.error = 'No se pudo guardar la comisión. Verifique los datos.';
        }
      });
  }

  eliminarComision(comision: ComisionFormativa): void {
    this.mensaje = '';
    this.error = '';
    this.guardando = true;

    this.coordinadorService.eliminarComision(comision.idComision).subscribe({
      next: () => {
        this.guardando = false;
        this.mensaje = 'Comisión eliminada correctamente.';
        this.cargarComisiones();
      },
      error: () => {
        this.guardando = false;
        this.error = 'No se pudo eliminar la comisión.';
      }
    });
  }

  parsearMiembros(): Array<{ idDocente: number; cargo: string }> | null {
    const miembrosValidos = this.miembrosSeleccionados
      .filter((miembro) => miembro.idDocente && miembro.cargo)
      .map((miembro) => ({
        idDocente: Number(miembro.idDocente),
        cargo: miembro.cargo.trim().toUpperCase()
      }));

    if (miembrosValidos.length === 0) {
      return [];
    }

    const ids = miembrosValidos.map((m) => m.idDocente);
    if (new Set(ids).size !== ids.length) {
      this.error = 'No se puede repetir el mismo docente en la comisión.';
      return null;
    }

    return miembrosValidos;
  }

  finalizarGuardadoConExito(mensaje: string): void {
    this.guardando = false;
    this.mensaje = mensaje;
    this.nuevaComision.periodoAcademico = '';
    this.nuevaComision.estado = 'ACTIVA';
    this.miembrosSeleccionados = [{ idDocente: null, cargo: 'PRESIDENTE' }];
    this.cargarComisiones();
  }
}
