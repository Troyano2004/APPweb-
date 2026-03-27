import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EnviarCodigoRequest,
  VerificarCodigoRequest,
  SolicitudRegistroRequest,
  SolicitudRegistroResponse,
  CarreraItem
} from './model';

@Injectable({ providedIn: 'root' })
export class SolicitudRegistroService {
  private readonly baseUrl = `${environment.apiUrl}/api/solicitud-registro`;

  constructor(private http: HttpClient) {}

  // PASO 1: enviar correo (tu backend lo tiene con @RequestParam)
  enviarCorreo(correo: string): Observable<SolicitudRegistroResponse> {
    const params = new HttpParams().set('correo', correo);
    return this.http.post<SolicitudRegistroResponse>(`${this.baseUrl}/correo`, null, { params });
  }

  // PASO 2: verificar código
  verificarCodigo(req: VerificarCodigoRequest): Observable<SolicitudRegistroResponse> {
    return this.http.post<SolicitudRegistroResponse>(`${this.baseUrl}/verificar`, req);
  }

  // PASO 3: enviar datos
  enviarDatos(req: SolicitudRegistroRequest): Observable<SolicitudRegistroResponse> {
    return this.http.post<SolicitudRegistroResponse>(`${this.baseUrl}/datos`, req);
  }

  // (Opcional) cargar carreras si tienes endpoint de catálogo
  // Ajusta la URL a tu backend real
  listarCarreras(): Observable<CarreraItem[]> {
    return this.http.get<CarreraItem[]>(`${environment.apiUrl}/api/catalogos/carreras`);
  }
}
