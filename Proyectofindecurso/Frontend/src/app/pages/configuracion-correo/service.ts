import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfiguracionCorreoDto } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConfiguracionCorreoService {

  private readonly base = `${environment.apiUrl}/api/configuracion-correo`;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<ConfiguracionCorreoDto[]> {
    return this.http.get<ConfiguracionCorreoDto[]>(this.base); // ← GET /api/configuracion-correo ✅
  }

  obtenerActiva(): Observable<ConfiguracionCorreoDto> {
    return this.http.get<ConfiguracionCorreoDto>(`${this.base}/activa`);
  }

  crear(dto: ConfiguracionCorreoDto): Observable<ConfiguracionCorreoDto> {
    return this.http.post<ConfiguracionCorreoDto>(this.base, dto);
  }

  editar(id: number, dto: ConfiguracionCorreoDto): Observable<ConfiguracionCorreoDto> {
    return this.http.put<ConfiguracionCorreoDto>(`${this.base}/${id}`, dto);
  }

  activar(id: number): Observable<ConfiguracionCorreoDto> {
    return this.http.patch<ConfiguracionCorreoDto>(`${this.base}/${id}/activar`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
