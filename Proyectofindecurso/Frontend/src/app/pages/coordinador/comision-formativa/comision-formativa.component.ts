import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComisionFormativa, CoordinadorService } from '../../../services/coordinador';

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
        <h3>Detalle de comisión por proyecto</h3>
        <table>
          <thead>
            <tr>
              <th>Proyecto</th>
              <th>Estado</th>
              <th>Miembros</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let comision of comisiones">
              <td>{{ comision.carrera }}</td>
              <td>
                <span class="tag" [class.ok]="comision.estado === 'Conformada'" [class.warn]="comision.estado !== 'Conformada'">
                  {{ comision.estado }}
                </span>
              </td>
              <td>{{ mostrarMiembros(comision) }}</td>
              <td>
                <button type="button" (click)="seleccionarComision(comision)">
                  {{ comision.estado === 'Conformada' ? 'Ver detalle' : 'Conformar' }}
                </button>
              </td>
            </tr>
            <tr *ngIf="comisiones.length === 0">
              <td colspan="4" class="empty">No hay comisiones registradas.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Crear comisión formativa</h3>
        <div class="form">
          <div class="field">
            <label>Carrera</label>
            <input type="text" placeholder="Id de carrera" [(ngModel)]="nuevaComision.idCarrera" />
          </div>
          <div class="field">
            <label>Periodo académico</label>
            <input type="text" placeholder="Ej. 2024-1" [(ngModel)]="nuevaComision.periodoAcademico" />
          </div>
          <div class="field">
            <label>Estado</label>
            <input type="text" placeholder="Ej. Conformada" [(ngModel)]="nuevaComision.estado" />
          </div>
          <div class="field">
            <label>Miembros (idDocente:cargo)</label>
            <textarea rows="3" placeholder="Ej. 12:Presidente, 44:Vocal" [(ngModel)]="miembrosTexto"></textarea>
          </div>
          <button type="button" (click)="guardarComision()">Guardar comisión</button>
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

      .form {
        display: grid;
        gap: 1rem;
        margin-top: 0.75rem;
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
export class ComisionFormativaComponent implements OnInit {
  comisiones: ComisionFormativa[] = [];
  nuevaComision: { idCarrera: number | null; periodoAcademico: string; estado: string } = {
    idCarrera: null,
    periodoAcademico: '',
    estado: 'No conformada'
  };
  miembrosTexto = '';

  constructor(private coordinadorService: CoordinadorService) {}

  ngOnInit(): void {
    this.cargarComisiones();
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
    return comision.miembros.map((miembro) => miembro.docente).join(', ');
  }

  seleccionarComision(comision: ComisionFormativa): void {
    this.nuevaComision = {
      idCarrera: null,
      periodoAcademico: comision.periodoAcademico,
      estado: comision.estado
    };
  }

  guardarComision(): void {
    if (!this.nuevaComision.idCarrera || !this.nuevaComision.periodoAcademico.trim()) {
      return;
    }
    this.coordinadorService
      .crearComision({
        idCarrera: this.nuevaComision.idCarrera,
        periodoAcademico: this.nuevaComision.periodoAcademico,
        estado: this.nuevaComision.estado
      })
      .subscribe((comision) => {
        this.asignarMiembrosSiAplica(comision.idComision);
      });
  }

  asignarMiembrosSiAplica(idComision: number): void {
    const miembros = this.parsearMiembros();
    if (miembros.length === 0) {
      this.resetFormulario();
      this.cargarComisiones();
      return;
    }
    this.coordinadorService.asignarMiembros(idComision, miembros).subscribe(() => {
      this.resetFormulario();
      this.cargarComisiones();
    });
  }

  parsearMiembros(): Array<{ idDocente: number; cargo: string }> {
    if (!this.miembrosTexto.trim()) {
      return [];
    }
    return this.miembrosTexto
      .split(',')
      .map((item) => item.trim())
      .map((item) => {
        const [idDocente, cargo] = item.split(':').map((parte) => parte.trim());
        return { idDocente: Number(idDocente), cargo };
      })
      .filter((miembro) => miembro.idDocente && miembro.cargo);
  }

  resetFormulario(): void {
    this.nuevaComision = { idCarrera: null, periodoAcademico: '', estado: 'No conformada' };
    this.miembrosTexto = '';
  }
}
