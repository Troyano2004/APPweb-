import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SolicitudPendiente } from './model';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiUrl}/api/solicitud-registro`;

@Injectable({ providedIn: 'root' })
export class GestionSolicitudesService {

  constructor(private http: HttpClient) {}

  listarPendientes(): Observable<SolicitudPendiente[]> {
    return this.http.get<SolicitudPendiente[]>(`${BASE}/pendientes`);
  }

  aprobar(idSolicitud: number): Observable<any> {
    return this.http.post(`${BASE}/${idSolicitud}/aprobar`, null);
  }

  rechazar(idSolicitud: number, motivo: string): Observable<any> {
    const params = new HttpParams().set('motivo', motivo);
    return this.http.post(`${BASE}/${idSolicitud}/rechazar`, null, { params });
  }
}
