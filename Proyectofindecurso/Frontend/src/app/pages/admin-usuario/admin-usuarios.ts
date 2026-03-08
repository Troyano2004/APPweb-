
import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  AdminUsuariosService,
  UsuarioCreateRequest,
  UsuarioDTO,
  UsuarioUpdateRequest,
  RolAppDTO
} from '../../services/admin-usuarios.service';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-usuarios.html',
  styleUrls: ['./admin-usuarios.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class AdminUsuariosComponent implements OnInit {

  // ===== UI =====
  filtro    = '';
  cargando  = false;
  errorMsg  = '';
  warnMsg   = '';

  modalAbierto = false;
  modoEdicion  = false;

  // ===== DATA =====
  usuarios: UsuarioDTO[]  = [];
  rolesApp: RolAppDTO[]   = [];

  // ===== FORMS =====
  // El rol principal es obligatorio — define el tipo de usuario
  createRolPrincipalId: number | null = null;
  updateRolPrincipalId: number | null = null;

  // Roles adicionales seleccionados (pueden ser de cualquier tipo)
  createRolesExtra: number[] = [];
  updateRolesExtra: number[] = [];

  formCreate = {
    cedula: '',
    correoInstitucional: '',
    username: '',
    passwordApp: '',
    nombres: '',
    apellidos: '',
    activo: true
  };

  formUpdate = {
    nombres: '',
    apellidos: '',
    activo: true,
    password: ''
  };

  usuarioEditandoId: number | null = null;

  constructor(
    private usuariosService: AdminUsuariosService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarRolesApp();
    this.recargar();
  }

  // ===== FILTRO =====
  get usuariosFiltrados(): UsuarioDTO[] {
    const q = (this.filtro || '').toLowerCase().trim();
    if (!q) return this.usuarios;
    return this.usuarios.filter(u => {
      const txt = `${u.idUsuario} ${u.username} ${u.nombres} ${u.apellidos} ${u.rolApp ?? ''} ${u.rolesApp ?? ''} ${u.activo ? 'activo' : 'inactivo'}`.toLowerCase();
      return txt.includes(q);
    });
  }

  // ===== helpers =====
  private rolById(id: number | null | undefined): RolAppDTO | undefined {
    if (id == null) return undefined;
    return this.rolesApp.find(r => r.idRolApp === id);
  }

  private buildIdsRolApp(principalId: number, extras: number[]): number[] {
    const cleanExtras = (extras || []).filter(x => x !== principalId);
    return Array.from(new Set([principalId, ...cleanExtras]));
  }

  // ===== CARGAS =====
  cargarRolesApp(): void {
    this.usuariosService.listarRolesApp().subscribe({
      next: (data) => {
        this.rolesApp = (data || []).filter(r => r.activo !== false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.rolesApp = [];
        this.cdr.detectChanges();
      }
    });
  }

  recargar(): void {
    this.errorMsg = '';
    this.warnMsg  = '';
    this.cargando = true;
    this.cdr.detectChanges();

    this.usuariosService.listar().subscribe({
      next: (data) => {
        this.usuarios = [...(data || [])];
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar la lista de usuarios.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ===== VALIDACIÓN UI =====
  // ✅ CORREGIDO: se elimina la restricción de "roles incompatibles"
  // El backend (sp_crear_usuario_v3) ya maneja combinaciones de roles distintos
  // creando automáticamente un rol compuesto en PostgreSQL.
  puedeGuardar(): boolean {
    if (!this.modalAbierto) return false;
    if (!this.rolesApp || this.rolesApp.length === 0) return false;

    this.warnMsg = '';

    if (!this.modoEdicion) {
      // CREATE: campos obligatorios
      if (!this.formCreate.cedula.trim()
        || !this.formCreate.correoInstitucional.trim()
        || !this.formCreate.username.trim()
        || !this.formCreate.passwordApp.trim()
        || !this.formCreate.nombres.trim()
        || !this.formCreate.apellidos.trim()
        || this.createRolPrincipalId == null
      ) return false;

      return true;
    }

    // UPDATE
    if (this.usuarioEditandoId == null)    return false;
    if (this.updateRolPrincipalId == null) return false;

    return true;
  }

  // ===== ACCIONES =====
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
    this.errorMsg = '';
    this.warnMsg  = '';
    this.modoEdicion      = false;
    this.usuarioEditandoId = null;
    this.modalAbierto     = true;
    this.formCreate = {
      cedula: '', correoInstitucional: '', username: '',
      passwordApp: '', nombres: '', apellidos: '', activo: true
    };
    this.createRolPrincipalId = null;
    this.createRolesExtra     = [];
    this.cdr.detectChanges();
  }

  abrirEditar(u: UsuarioDTO): void {
    this.errorMsg = '';
    this.warnMsg  = '';
    this.modoEdicion      = true;
    this.usuarioEditandoId = u.idUsuario;
    this.modalAbierto     = true;

    const ids      = (u.idsRolApp || []).slice();
    const principal = (u.idRolApp ?? null) ?? (ids.length ? ids[0] : null);
    const extras    = ids.filter(x => x !== principal);

    this.updateRolPrincipalId = principal;
    this.updateRolesExtra     = extras;

    this.formUpdate = {
      nombres: u.nombres ?? '', apellidos: u.apellidos ?? '',
      activo: !!u.activo, password: ''
    };
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.cdr.detectChanges();
  }

  // ===== roles extras =====
  toggleExtraRoleCreate(idRolApp: number): void {
    const exists = this.createRolesExtra.includes(idRolApp);
    this.createRolesExtra = exists
      ? this.createRolesExtra.filter(x => x !== idRolApp)
      : [...this.createRolesExtra, idRolApp];
    this.warnMsg = '';
    this.cdr.detectChanges();
  }

  toggleExtraRoleUpdate(idRolApp: number): void {
    const exists = this.updateRolesExtra.includes(idRolApp);
    this.updateRolesExtra = exists
      ? this.updateRolesExtra.filter(x => x !== idRolApp)
      : [...this.updateRolesExtra, idRolApp];
    this.warnMsg = '';
    this.cdr.detectChanges();
  }

  isExtraSelectedCreate(id: number): boolean {
    return this.createRolesExtra.includes(id);
  }

  isExtraSelectedUpdate(id: number): boolean {
    return this.updateRolesExtra.includes(id);
  }

  get selectedNamesCreate(): string[] {
    if (this.createRolPrincipalId == null) return [];
    return this.buildIdsRolApp(this.createRolPrincipalId, this.createRolesExtra)
      .map(id => this.rolById(id)?.nombre || `ROL_${id}`);
  }

  get selectedNamesUpdate(): string[] {
    if (this.updateRolPrincipalId == null) return [];
    return this.buildIdsRolApp(this.updateRolPrincipalId, this.updateRolesExtra)
      .map(id => this.rolById(id)?.nombre || `ROL_${id}`);
  }

  // ===== GUARDAR =====
  guardar(): void {
    this.errorMsg = '';
    this.warnMsg  = '';

    // ===== CREATE =====
    if (!this.modoEdicion) {
      if (this.createRolPrincipalId == null) {
        this.errorMsg = 'Seleccione un rol principal.';
        this.cdr.detectChanges();
        return;
      }

      const idsRolApp = this.buildIdsRolApp(this.createRolPrincipalId, this.createRolesExtra);

      const body: UsuarioCreateRequest = {
        cedula:                this.formCreate.cedula.trim(),
        correoInstitucional:   this.formCreate.correoInstitucional.trim(),
        username:              this.formCreate.username.trim(),
        passwordApp:           this.formCreate.passwordApp,
        nombres:               this.formCreate.nombres.trim(),
        apellidos:             this.formCreate.apellidos.trim(),
        activo:                !!this.formCreate.activo,
        idsRolApp
      };

      this.cargando = true;
      this.cdr.detectChanges();

      this.usuariosService.crear(body).subscribe({
        next: () => {
          this.cargando = false;
          this.cerrarModal();
          this.recargar();
        },
        error: (err: any) => {
          this.cargando = false;
          this.errorMsg = err?.error?.message || 'No se pudo crear el usuario.';
          this.cdr.detectChanges();
        }
      });
      return;
    }

    // ===== UPDATE =====
    if (this.usuarioEditandoId == null) return;
    if (this.updateRolPrincipalId == null) {
      this.errorMsg = 'Seleccione un rol principal.';
      this.cdr.detectChanges();
      return;
    }

    const idsRolApp = this.buildIdsRolApp(this.updateRolPrincipalId, this.updateRolesExtra);

    const bodyUp: UsuarioUpdateRequest = {
      nombres:   (this.formUpdate.nombres  || '').trim(),
      apellidos: (this.formUpdate.apellidos || '').trim(),
      activo:    !!this.formUpdate.activo,
      password:  (this.formUpdate.password  || '').trim() === '' ? '' : this.formUpdate.password,
      idsRolApp
    };

    this.cargando = true;
    this.cdr.detectChanges();

    this.usuariosService.editar(this.usuarioEditandoId, bodyUp).subscribe({
      next: () => {
        this.cargando = false;
        this.cerrarModal();
        this.recargar();
      },
      error: (err: any) => {
        this.cargando = false;
        this.errorMsg = err?.error?.message || 'No se pudo editar el usuario.';
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void {
    window.history.back();
  }
}
