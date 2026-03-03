
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/* ========= DTOs ========= */

export interface RolAppDTO {
  idRolApp: number;
  nombre: string;
  descripcion?: string | null;
  activo?: boolean | null;

  idRolBase?: number | null;
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

  // ✅ backend espera esto
  passwordApp: string;

  nombres: string;
  apellidos: string;

  idsRolApp: number[];
  activo: boolean;
}

export interface UsuarioUpdateRequest {
  nombres?: string;
  apellidos?: string;

  idsRolApp?: number[] | null;

  activo?: boolean;

  password?: string; // "" => no cambiar
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

  // ===== ROLES APP =====
  listarRolesApp(): Observable<RolAppDTO[]> {
    return this.http.get<RolAppDTO[]>(`${this.baseUrl}/rol-app`);
  }
}
