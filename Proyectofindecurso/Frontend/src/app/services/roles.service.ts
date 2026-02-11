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
  idRolApp: number;              // ✅ debe llegar así del backend
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
}

export interface RolAppUpdateRequest {
  nombre?: string;
  descripcion?: string;
  activo?: boolean;
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
}
