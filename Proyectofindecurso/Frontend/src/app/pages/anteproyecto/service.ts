import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Anteproyecto, AnteproyectoVersion, AnteproyectoVersionRequest } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AnteproyectoService {
  private base = `${environment.apiUrl}/api/anteproyectos`;

  constructor(private http: HttpClient) {}

  miAnteproyecto(idEstudiante: number): Observable<Anteproyecto> {
    return this.http.get<Anteproyecto>(`${this.base}/mi-anteproyecto/${idEstudiante}`);
  }

  versiones(idAnteproyecto: number): Observable<AnteproyectoVersion[]> {
    return this.http.get<AnteproyectoVersion[]>(`${this.base}/${idAnteproyecto}/versiones`);
  }

  guardarBorrador(idAnteproyecto: number, req: AnteproyectoVersionRequest) {
    return this.http.post<AnteproyectoVersion>(`${this.base}/${idAnteproyecto}/versiones/borrador`, req);
  }

  enviarRevision(idAnteproyecto: number, req: AnteproyectoVersionRequest) {
    return this.http.post<AnteproyectoVersion>(`${this.base}/${idAnteproyecto}/versiones/enviar`, req);
  }

  ultimaVersion(idAnteproyecto: number): Observable<AnteproyectoVersion> {
    return this.http.get<AnteproyectoVersion>(`${this.base}/${idAnteproyecto}/ultima-version`);
  }

}
