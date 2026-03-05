
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/* ========= DTOs ========= */

export interface PermisoDto {
  idPermiso: number;
  codigo: string;
  descripcion: string;
  activo: boolean;
}

/** Rol del aplicativo */
export interface RolAppDto {
  idRolApp: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  permisos: string[];

  // ✅ NUEVO: vinculación con rol base de BD
  idRolBase?: number | null;
  rolBd?: string | null;       // nombre en pg_roles, ej: rol_admin
}

/** Rol físico de la BD (pg_roles) - mostrado dinámicamente */
export interface RolBdDto {
  rolBd: string;               // ej: rol_admin
  rolApp: string | null;       // ej: ROLE_ADMIN
  idRolApp: number | null;
  rolBase: string | null;      // ej: ADMIN
}

export interface RolAppCreateRequest {
  nombre: string;
  descripcion?: string;
  activo: boolean;
  permisos: number[];
  // ✅ NUEVO: id del rol base de BD
  idRolBase?: number | null;
}

export interface RolAppUpdateRequest {
  nombre?: string;
  descripcion?: string;
  activo?: boolean;
  // ✅ NUEVO: id del rol base de BD
  idRolBase?: number | null;
}

export interface EstadoRequest {
  activo: boolean;
}

export interface RolPermisosRequest {
  permisos: number[];
}

/* ========= Catálogo de roles_sistema (para el select en el form) ========= */
export interface RolSistemaDto {
  idRol: number;
  nombreRol: string;   // ej: ADMIN
  nombreRolBd: string; // ej: rol_admin
}

/* ========= SERVICE ========= */

@Injectable({ providedIn: 'root' })
export class RolesService {

  private readonly baseUrl = 'http://localhost:8080';
  private readonly rolAppUrl = `${this.baseUrl}/rol-app`;
  private readonly permisosUrl = `${this.baseUrl}/permisos`;
  private readonly rolesBdUrl = `${this.baseUrl}/roles-bd`;

  constructor(private http: HttpClient) {}

  /* ====== rol_app ====== */

  listarRolesApp(): Observable<RolAppDto[]> {
    return this.http.get<RolAppDto[]>(this.rolAppUrl);
  }

  crearRolApp(body: RolAppCreateRequest): Observable<RolAppDto> {
    return this.http.post<RolAppDto>(this.rolAppUrl, body);
  }

  editarRolApp(id: number, body: RolAppUpdateRequest): Observable<RolAppDto> {
    return this.http.put<RolAppDto>(`${this.rolAppUrl}/${id}`, body);
  }

  cambiarEstadoRolApp(id: number, body: EstadoRequest): Observable<RolAppDto> {
    return this.http.patch<RolAppDto>(`${this.rolAppUrl}/${id}/estado`, body);
  }

  asignarPermisosRolApp(id: number, body: RolPermisosRequest): Observable<RolAppDto> {
    return this.http.post<RolAppDto>(`${this.rolAppUrl}/${id}/permisos`, body);
  }

  /* ====== permisos ====== */

  listarPermisos(): Observable<PermisoDto[]> {
    return this.http.get<PermisoDto[]>(this.permisosUrl);
  }

  /* ====== roles BD dinámicos ====== */

  listarRolesBd(): Observable<RolBdDto[]> {
    return this.http.get<RolBdDto[]>(this.rolesBdUrl);
  }
}
