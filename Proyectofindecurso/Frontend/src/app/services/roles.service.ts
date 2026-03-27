
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/* ========= DTOs ========= */

export interface PermisoDto {
  idPermiso: number;
  codigo: string;
  descripcion: string;
  activo: boolean;
}

export interface RolSistemaDto {
  idRol: number;
  nombreRol: string;
}

/** Rol del aplicativo */
export interface RolAppDto {
  idRolApp: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  permisos: string[];
}

export interface RolAppCreateRequest {
  nombre: string;
  descripcion?: string;
  activo: boolean;
  permisos: number[];
  idRolBase: number;        // ← FIX Error 2: obligatorio al crear
}

export interface RolAppUpdateRequest {
  nombre?: string;
  descripcion?: string;
  activo?: boolean;
  idRolBase?: number | null; // ← FIX Error 2: opcional al editar
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

  private readonly baseUrl = environment.apiUrl;

  private readonly rolAppUrl     = `${this.baseUrl}/rol-app`;
  private readonly permisosUrl   = `${this.baseUrl}/permisos`;
  private readonly rolesSistUrl  = `${this.baseUrl}/roles-sistema`;

  constructor(private http: HttpClient) {}

  /* ====== roles_sistema (para el select "Rol base") ====== */
  listarRolesSistema(): Observable<RolSistemaDto[]> {
    return this.http.get<RolSistemaDto[]>(this.rolesSistUrl);
  }

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
}
