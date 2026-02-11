import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UsuarioDTO {
  idUsuario: number;
  cedula: string;
  correoInstitucional: string;
  username: string;
  nombres: string;
  apellidos: string;
  rol: string;      // ✅ STRING (no objeto)
  activo: boolean;
}

export interface UsuarioCreateRequest {
  cedula: string;
  correoInstitucional: string;
  username: string;
  password: string;
  nombres: string;
  apellidos: string;
  rol: string;      // ✅ STRING
  activo: boolean;
}

export interface UsuarioUpdateRequest {
  nombres?: string;
  apellidos?: string;
  rol?: string;     // ✅ STRING
  activo?: boolean;
  password?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminUsuariosService {

  private baseUrl = 'http://localhost:8080'; // ajusta si es otro

  constructor(private http: HttpClient) {}

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
    // ✅ Enviamos JSON {activo:true/false} como tu DTO UsuarioEstadoRequest
    return this.http.patch<void>(`${this.baseUrl}/admin/usuarios/${idUsuario}/estado`, { activo });
  }
}
