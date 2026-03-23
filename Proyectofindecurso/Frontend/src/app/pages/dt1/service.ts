import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Dt1Detalle, Dt1Enviado, Dt1RevisionRequest } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class Dt1ApiService {
  private base = environment.apiUrl + '/api/dt1';

  constructor(private http: HttpClient) {}

  // OJO: tu backend usa /lista/{idDocente}
  lista(idDocente: number): Observable<Dt1Enviado[]> {
    return this.http.get<Dt1Enviado[]>(`${this.base}/lista/${idDocente}`);
  }

  detalle(idAnteproyecto: number, idDocente: number): Observable<Dt1Detalle> {
    return this.http.get<Dt1Detalle>(`${this.base}/detalle/${idAnteproyecto}/${idDocente}`);
  }

  revisar(data: Dt1RevisionRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/revisar`, data);
  }

  pdf(idAnteproyecto: number, idDocente: number): Observable<Blob> {
    return this.http.get(`${this.base}/pdf/${idAnteproyecto}/${idDocente}`, {
      responseType: 'blob'
    });
  }
}
