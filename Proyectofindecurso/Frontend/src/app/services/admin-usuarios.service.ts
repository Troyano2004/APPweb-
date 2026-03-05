
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/* ========= DTOs ========= */

export interface RolAppDTO {
  idRolApp: number;
  nombre: string;
  descripcion?: string | null;
  activo?: boolean | null;

  // ✅ NUEVO: id del rol base de BD (1=ADMIN, 2=DOCENTE, 3=ESTUDIANTE, etc.)
  idRolBase?: number | null;

  // ✅ NUEVO: nombre del rol en pg_roles (ej: rol_admin)
  rolBd?: string | null;
}

/** Rol físico de la BD (pg_roles), mostrado dinámicamente */
export interface RolBdDTO {
  rolBd: string;        // ej: rol_admin
  rolApp: string | null; // ej: ROLE_ADMIN
  idRolApp: number | null;
  rolBase: string | null; // ej: ADMIN
}

export interface UsuarioDTO {
  idUsuario: number;
  username: string;
  nombres: string;
  apellidos: string;

  rolApp?: string | null;
  idRolApp?: number | null;

  rolesApp?: string | null;
  idsRolApp?: number[];

  activo: boolean;
}

export interface UsuarioCreateRequest {
  cedula: string;
  correoInstitucional: string;
  username: string;
  passwordApp: string;
  nombres: string;
  apellidos: string;
  idsRolApp: number[];
  // ✅ NUEVO: rol principal (obligatorio para sp_crear_usuario_v4)
  idRolAppPrincipal: number;
  activo: boolean;
}

export interface UsuarioUpdateRequest {
  nombres?: string;
  apellidos?: string;
  idsRolApp?: number[] | null;
  activo?: boolean;
  password?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminUsuariosService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // ===== USUARIOS =====
  listar(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.baseUrl}/admin/usuarios`);
  }

  crear(body: UsuarioCreateRequest): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.baseUrl}/admin/usuarios`, body);
  }

  editar(idUsuario: number, body: UsuarioUpdateRequest): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.baseUrl}/admin/usuarios/${idUsuario}`, body);
  }

  cambiarEstado(idUsuario: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/admin/usuarios/${idUsuario}/estado`, { activo });
  }

  // ===== ROLES APP (con idRolBase y rolBd) =====
  listarRolesApp(): Observable<RolAppDTO[]> {
    return this.http.get<RolAppDTO[]>(`${this.baseUrl}/rol-app`);
  }

  // ✅ NUEVO: Roles físicos de la BD (pg_roles) - vista dinámica
  listarRolesBd(): Observable<RolBdDTO[]> {
    return this.http.get<RolBdDTO[]>(`${this.baseUrl}/roles-bd`);
  }
}
