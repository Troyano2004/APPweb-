import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificacionDto {
  tipo:    'EXITO' | 'ALERTA' | 'INFO';
  titulo:  string;
  mensaje: string;
  fecha:   string;
}

@Injectable({ providedIn: 'root' })
export class NotificacionesService {

  private readonly API = 'http://localhost:8080/api/notificaciones';

  constructor(private http: HttpClient) {}

  obtener(idEstudiante: number): Observable<NotificacionDto[]> {
    return this.http.get<NotificacionDto[]>(`${this.API}/${idEstudiante}`, { withCredentials: true });
  }
}
