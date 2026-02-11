import {
  Component,
  OnInit,
  AfterViewInit,
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  AdminUsuariosService,
  UsuarioCreateRequest,
  UsuarioDTO,
  UsuarioUpdateRequest
} from '../../services/admin-usuarios.service';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-usuarios.html',
  styleUrls: ['./admin-usuarios.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class AdminUsuariosComponent implements OnInit, AfterViewInit {

  // ====== UI ======
  filtro = '';
  cargando = false;
  errorMsg = '';

  modalAbierto = false;
  modoEdicion = false;

  // ====== DATA ======
  usuarios: UsuarioDTO[] = [];

  // roles para select (si lo estás usando fijo)
  rolesUsuario: string[] = ['ADMIN', 'DOCENTE', 'ESTUDIANTE'];

  // ====== FORM USUARIO ======
  formCreate: UsuarioCreateRequest = {
    cedula: '',
    correoInstitucional: '',
    username: '',
    password: '',
    nombres: '',
    apellidos: '',
    rol: 'ESTUDIANTE',
    activo: true
  };

  formUpdate: UsuarioUpdateRequest = {
    nombres: '',
    apellidos: '',
    rol: 'ESTUDIANTE',
    activo: true,
    password: ''
  };

  usuarioEditandoId: number | null = null;

  constructor(
    private usuariosService: AdminUsuariosService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Intento 1: carga normal
    this.recargar();
  }

  ngAfterViewInit(): void {
    // Intento 2 (muy útil en apps zoneless / timing): fuerza otra carga microtask
    queueMicrotask(() => this.recargar());
  }

  // ====== GETTER FILTRADO ======
  get usuariosFiltrados(): UsuarioDTO[] {
    const q = (this.filtro || '').toLowerCase().trim();
    if (!q) return this.usuarios;

    return this.usuarios.filter(u =>
      `${u.idUsuario} ${u.username} ${u.nombres} ${u.apellidos} ${u.rol} ${u.activo ? 'activo' : 'inactivo'}`
        .toLowerCase()
        .includes(q)
    );
  }

  // ====== USUARIOS ======
  recargar(): void {
    this.errorMsg = '';
    this.cargando = true;

    // (clave) si está zoneless, el cambio no se refleja sin detectChanges()
    this.cdr.detectChanges();

    this.usuariosService.listar().subscribe({
      next: (data) => {
        this.usuarios = [...(data || [])];   // nueva referencia (mejor para refrescar UI)
        this.cargando = false;

        // ✅ fuerza refresco visual sin necesidad de click
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar la lista de usuarios.';
        this.cargando = false;

        this.cdr.detectChanges();
      }
    });
  }

  toggleActivo(u: UsuarioDTO): void {
    const nuevoEstado = !u.activo;

    this.usuariosService.cambiarEstado(u.idUsuario, nuevoEstado).subscribe({
      next: () => {
        u.activo = nuevoEstado;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'No se pudo cambiar el estado del usuario.';
        this.cdr.detectChanges();
      }
    });
  }

  abrirNuevo(): void {
    this.modoEdicion = false;
    this.usuarioEditandoId = null;
    this.modalAbierto = true;

    this.formCreate = {
      cedula: '',
      correoInstitucional: '',
      username: '',
      password: '',
      nombres: '',
      apellidos: '',
      rol: 'ESTUDIANTE',
      activo: true
    };

    this.cdr.detectChanges();
  }

  abrirEditar(u: UsuarioDTO): void {
    this.modoEdicion = true;
    this.usuarioEditandoId = u.idUsuario;
    this.modalAbierto = true;

    this.formUpdate = {
      nombres: u.nombres,
      apellidos: u.apellidos,
      rol: (u.rol || 'ESTUDIANTE') as any,  // si tu DTO viene string, esto lo soporta
      activo: !!u.activo,
      password: ''
    };

    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.cdr.detectChanges();
  }

  guardar(): void {
    this.errorMsg = '';

    // ===== CREATE =====
    if (!this.modoEdicion) {
      const body: UsuarioCreateRequest = {
        ...this.formCreate,
        rol: (this.formCreate.rol || 'ESTUDIANTE').toString().toUpperCase(),
        activo: !!this.formCreate.activo
      };

      this.usuariosService.crear(body).subscribe({
        next: () => {
          this.cerrarModal();
          this.recargar();
        },
        error: () => {
          this.errorMsg = 'No se pudo crear el usuario.';
          this.cdr.detectChanges();
        }
      });
      return;
    }

    // ===== UPDATE =====
    if (this.usuarioEditandoId == null) return;

    const bodyUp: UsuarioUpdateRequest = {
      ...this.formUpdate,
      rol: (this.formUpdate.rol || 'ESTUDIANTE').toString().toUpperCase(),
      activo: !!this.formUpdate.activo
    };

    this.usuariosService.editar(this.usuarioEditandoId, bodyUp).subscribe({
      next: () => {
        this.cerrarModal();
        this.recargar();
      },
      error: () => {
        this.errorMsg = 'No se pudo editar el usuario.';
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void {
    window.history.back();
  }
}
