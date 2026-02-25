import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogosBasicosService, PeriodoTitulacion } from '../../../services/catalogos-basicos.service';

@Component({
  selector: 'app-periodo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1>Gestión de Períodos Académicos</h1>
        <button class="btn-primary" (click)="abrirModal(null)">+ Nuevo Período</button>
      </div>

      <div class="alert alert-info" *ngIf="loading">Cargando...</div>
      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
      <div class="alert alert-success" *ngIf="mensaje">{{ mensaje }}</div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Descripción</th>
              <th>Fecha Inicio</th>
              <th>Fecha Fin</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of items">
              <td>{{ item.idPeriodo }}</td>
              <td>{{ item.descripcion }}</td>
              <td>{{ item.fechaInicio }}</td>
              <td>{{ item.fechaFin }}</td>
              <td>
                <span class="badge" [class.badge-active]="item.activo" [class.badge-inactive]="!item.activo">
                  {{ item.activo ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <button class="btn-small btn-edit" (click)="abrirModal(item)">Editar</button>
                <button class="btn-small" [class.btn-success]="!item.activo" [class.btn-warning]="item.activo"
                        (click)="cambiarEstado(item)">
                  {{ item.activo ? 'Desactivar' : 'Activar' }}
                </button>
                <button class="btn-small btn-delete" (click)="eliminar(item.idPeriodo!)">Eliminar</button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0 && !loading">
              <td colspan="6" class="text-center">No hay registros</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="modal" *ngIf="modalAbierto" (click)="cerrarModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2>{{ editando ? 'Editar' : 'Nuevo' }} Período</h2>
            <button class="close" (click)="cerrarModal()">&times;</button>
          </div>

          <form (ngSubmit)="guardar()" class="modal-body">
            <div class="form-group">
              <label>Descripción <span class="required">*</span></label>
              <input type="text" [(ngModel)]="formulario.descripcion" name="descripcion"
                     placeholder="Ej: 2024-2025 Semestre I" required class="form-control">
            </div>

            <div class="form-group">
              <label>Fecha Inicio <span class="required">*</span></label>
              <input type="date" [(ngModel)]="formulario.fechaInicio" name="fechaInicio" required class="form-control">
            </div>

            <div class="form-group">
              <label>Fecha Fin <span class="required">*</span></label>
              <input type="date" [(ngModel)]="formulario.fechaFin" name="fechaFin" required class="form-control">
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="formulario.activo" name="activo">
                <span>Período Activo</span>
              </label>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn-secondary" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn-primary" [disabled]="guardando">
                {{ guardando ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .page { padding: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .table-container { background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f9fafb; padding: 0.75rem; text-align: left; font-weight: 600; }
    td { padding: 0.75rem; border-top: 1px solid #e5e7eb; }
    .btn-primary { background: #0f7a3a; color: white; padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; }
    .btn-secondary { background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; }
    .btn-small { padding: 0.25rem 0.75rem; font-size: 0.875rem; margin-right: 0.5rem; border: none; border-radius: 4px; cursor: pointer; }
    .btn-edit { background: #3b82f6; color: white; }
    .btn-delete { background: #ef4444; color: white; }
    .btn-success { background: #10b981; color: white; }
    .btn-warning { background: #f59e0b; color: white; }
    .badge { padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
    .badge-active { background: #d1fae5; color: #065f46; }
    .badge-inactive { background: #fee2e2; color: #991b1b; }
    .alert { padding: 1rem; border-radius: 6px; margin-bottom: 1rem; }
    .alert-info { background: #dbeafe; color: #1e40af; }
    .alert-danger { background: #fee2e2; color: #991b1b; }
    .alert-success { background: #d1fae5; color: #065f46; }
    .modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-content { background: white; border-radius: 8px; width: 90%; max-width: 600px; max-height: 90vh; overflow-y: auto; }
    .modal-header { padding: 1.5rem; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; }
    .close { background: none; border: none; font-size: 1.5rem; cursor: pointer; }
    .modal-body { padding: 1.5rem; }
    .modal-footer { padding: 1rem 1.5rem; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; gap: 0.5rem; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.5rem; font-weight: 500; }
    .form-control { width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 4px; }
    .checkbox-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
    .checkbox-label input[type="checkbox"] { width: auto; }
    .required { color: #ef4444; }
  `]
})
export class PeriodoComponent implements OnInit {
  items: PeriodoTitulacion[] = [];
  formulario: PeriodoTitulacion = this.nuevoFormulario();
  modalAbierto = false;
  editando = false;
  loading = false;
  guardando = false;
  error = '';
  mensaje = '';

  constructor(private service: CatalogosBasicosService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading = true;
    this.service.listarPeriodos().subscribe({
      next: (data) => { this.items = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar'; this.loading = false; }
    });
  }

  abrirModal(item: PeriodoTitulacion | null): void {
    this.formulario = item ? { ...item } : this.nuevoFormulario();
    this.editando = !!item;
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
  }

  guardar(): void {
    if (!this.formulario.descripcion || !this.formulario.fechaInicio || !this.formulario.fechaFin) {
      this.error = 'Complete campos obligatorios';
      return;
    }
    this.guardando = true;
    const op = this.editando
      ? this.service.actualizarPeriodo(this.formulario.idPeriodo!, this.formulario)
      : this.service.crearPeriodo(this.formulario);
    op.subscribe({
      next: () => {
        this.mensaje = 'Guardado correctamente';
        this.cerrarModal();
        this.cargar();
        this.guardando = false;
        setTimeout(() => this.mensaje = '', 3000);
      },
      error: () => { this.error = 'Error al guardar'; this.guardando = false; }
    });
  }

  cambiarEstado(item: PeriodoTitulacion): void {
    const op = item.activo
      ? this.service.desactivarPeriodo(item.idPeriodo!)
      : this.service.activarPeriodo(item.idPeriodo!);
    op.subscribe({
      next: () => {
        this.mensaje = `Período ${item.activo ? 'desactivado' : 'activado'}`;
        this.cargar();
        setTimeout(() => this.mensaje = '', 3000);
      },
      error: () => { this.error = 'Error al cambiar estado'; }
    });
  }

  eliminar(id: number): void {
    if (!confirm('¿Eliminar?')) return;
    this.service.eliminarPeriodo(id).subscribe({
      next: () => { this.mensaje = 'Eliminado'; this.cargar(); setTimeout(() => this.mensaje = '', 3000); },
      error: () => { this.error = 'Error al eliminar'; }
    });
  }

  private nuevoFormulario(): PeriodoTitulacion {
    return { descripcion: '', fechaInicio: '', fechaFin: '', activo: false };
  }
}
