import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  RolesService,
  PermisoDto,
  RolAppDto,
  RolAppCreateRequest,
  RolAppUpdateRequest,
} from '../../services/roles.service';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './roles.html',
  styleUrls: ['./roles.scss']
})
export class RolesComponent implements OnInit {

  // UI
  q = '';
  loading = false;
  errorMsg = '';

  modalOpen = false;
  editMode = false;
  selectedId: number | null = null;

  // data
  roles: RolAppDto[] = [];
  permisos: PermisoDto[] = [];

  // permisos seleccionados (IDs)
  selectedPermisos: number[] = [];
  permTouched = false;

  form!: FormGroup;

  constructor(
    private rolesService: RolesService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      descripcion: [''],
      activo: [true, Validators.required],
    });

    this.cargarTodo();
  }

  // ✅ trackBy correcto
  trackByRolId = (_: number, r: RolAppDto) => r.idRolApp;
  trackByPermisoId = (_: number, p: PermisoDto) => p.idPermiso;

  volver(): void {
    this.router.navigate(['/admin/usuarios']);
  }

  // getter filtrado
  get rolesFiltrados(): RolAppDto[] {
    const t = (this.q || '').trim().toLowerCase();
    if (!t) return this.roles;

    return this.roles.filter(r => {
      const perms = (r.permisos || []).join(', ').toLowerCase();
      const estado = (r.activo ? 'activo' : 'inactivo');
      return (
        `${r.idRolApp} ${r.nombre} ${r.descripcion || ''} ${perms} ${estado}`
          .toLowerCase()
          .includes(t)
      );
    });
  }

  cargarTodo(): void {
    this.errorMsg = '';
    this.loading = true;
    this.cdr.detectChanges();

    // permisos
    this.rolesService.listarPermisos().subscribe({
      next: (perms) => {
        this.permisos = (perms || []).filter(p => p.activo);
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
      }
    });

    // roles APP
    this.rolesService.listarRolesApp().subscribe({
      next: (roles) => {
        this.roles = [...(roles || [])];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar la lista de roles del aplicativo.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ---------------- MODAL ----------------
  openCreate(): void {
    this.modalOpen = true;
    this.editMode = false;
    this.selectedId = null;

    this.form.reset({ nombre: '', descripcion: '', activo: true });
    this.selectedPermisos = [];
    this.permTouched = false;

    this.cdr.detectChanges();
  }

  openEdit(r: RolAppDto): void {
    this.modalOpen = true;
    this.editMode = true;
    this.selectedId = r.idRolApp;

    // códigos -> ids para checks
    this.selectedPermisos = this.permisos
      .filter(p => (r.permisos || []).includes(p.codigo))
      .map(p => p.idPermiso);

    this.form.patchValue({
      nombre: r.nombre,
      descripcion: r.descripcion || '',
      activo: !!r.activo
    });

    this.permTouched = false;
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.modalOpen = false;
    this.cdr.detectChanges();
  }

  // ---------------- PERMISOS ----------------
  togglePermiso(idPermiso: number): void {
    this.permTouched = true;

    const exists = this.selectedPermisos.includes(idPermiso);
    this.selectedPermisos = exists
      ? this.selectedPermisos.filter(x => x !== idPermiso)
      : [...this.selectedPermisos, idPermiso];

    this.cdr.detectChanges();
  }

  isPermisoChecked(idPermiso: number): boolean {
    return this.selectedPermisos.includes(idPermiso);
  }

  // ---------------- GUARDAR ----------------
  guardar(): void {
    this.errorMsg = '';
    this.permTouched = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.detectChanges();
      return;
    }

    if (this.selectedPermisos.length === 0) {
      this.cdr.detectChanges();
      return;
    }

    const payloadBase = {
      nombre: (this.form.value.nombre || '').toString().trim().toUpperCase(),
      descripcion: (this.form.value.descripcion || '').toString().trim(),
      activo: !!this.form.value.activo,
    };

    // CREAR
    if (!this.editMode) {
      const body: RolAppCreateRequest = {
        ...payloadBase,
        permisos: [...this.selectedPermisos]
      };

      this.rolesService.crearRolApp(body).subscribe({
        next: () => {
          this.closeModal();
          this.cargarTodo();
        },
        error: () => {
          this.errorMsg = 'No se pudo crear el rol del aplicativo.';
          this.cdr.detectChanges();
        }
      });
      return;
    }

    // EDITAR
    if (this.selectedId == null) return;

    const id = this.selectedId;

    const bodyUp: RolAppUpdateRequest = {
      ...payloadBase
    };

    this.rolesService.editarRolApp(id, bodyUp).subscribe({
      next: () => {
        this.rolesService.asignarPermisosRolApp(id, { permisos: [...this.selectedPermisos] }).subscribe({
          next: () => {
            this.closeModal();
            this.cargarTodo();
          },
          error: () => {
            this.errorMsg = 'Se editó el rol, pero falló la asignación de permisos.';
            this.cdr.detectChanges();
          }
        });
      },
      error: () => {
        this.errorMsg = 'No se pudo editar el rol del aplicativo.';
        this.cdr.detectChanges();
      }
    });
  }

  // ---------------- ESTADO ----------------
  cambiarEstado(r: RolAppDto): void {
    const id = r.idRolApp;
    const nuevo = !r.activo;

    this.rolesService.cambiarEstadoRolApp(id, { activo: nuevo }).subscribe({
      next: () => {
        this.roles = this.roles.map(x =>
          x.idRolApp === id ? { ...x, activo: nuevo } : x
        );
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'No se pudo cambiar el estado del rol.';
        this.cdr.detectChanges();
      }
    });
  }

  // ---------------- UTIL ----------------
  permisosLabel(r: RolAppDto): string {
    return (r.permisos || []).join(', ');
  }
}
