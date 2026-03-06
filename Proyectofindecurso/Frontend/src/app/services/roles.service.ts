
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

export interface RolAppDto {
  idRolApp: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  permisos: string[];
  idRolBase?: number | null;
  rolBd?: string | null;
}

export interface RolBdDto {
  rolBd: string;
  rolApp: string | null;
  idRolApp: number | null;
  rolBase: string | null;
}

// ✅ Viene dinámicamente de la BD (tabla roles_sistema)
export interface RolSistemaDto {
  idRol: number;
  nombreRol: string;
}

export interface RolAppCreateRequest {
  nombre: string;
  descripcion?: string;
  activo: boolean;
  permisos: number[];
  idRolBase?: number | null;
}

export interface RolAppUpdateRequest {
  nombre?: string;
  descripcion?: string;
  activo?: boolean;
  idRolBase?: number | null;
}

export interface EstadoRequest {
  activo: boolean;
}

export interface RolPermisosRequest {
  permisos: number[];
}

/* ========= SERVICE ========= */

@Injectable({ providedIn: 'root' })
export class RolesService {

  private readonly baseUrl = 'http://localhost:8080';
  private readonly rolAppUrl = `${this.baseUrl}/rol-app`;
  private readonly permisosUrl = `${this.baseUrl}/permisos`;
  private readonly rolesBdUrl = `${this.baseUrl}/roles-bd`;
  private readonly rolesSistemaUrl = `${this.baseUrl}/roles-sistema`; // ✅ NUEVO

  constructor(private http: HttpClient) {}

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

  listarPermisos(): Observable<PermisoDto[]> {
    return this.http.get<PermisoDto[]>(this.permisosUrl);
  }

  listarRolesBd(): Observable<RolBdDto[]> {
    return this.http.get<RolBdDto[]>(this.rolesBdUrl);
  }

  // ✅ NUEVO: carga roles_sistema desde la BD (dinámico, sin hardcodeo)
  listarRolesSistema(): Observable<RolSistemaDto[]> {
    return this.http.get<RolSistemaDto[]>(this.rolesSistemaUrl);
  }
}
