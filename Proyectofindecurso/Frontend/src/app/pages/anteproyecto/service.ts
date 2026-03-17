import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Anteproyecto, AnteproyectoVersion, AnteproyectoVersionRequest } from './model';

@Injectable({ providedIn: 'root' })
export class AnteproyectoService {
  private base = 'http://localhost:8080/api/anteproyectos';

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
  ultimaRevision(idAnteproyecto: number): Observable<{decision: string, observacion: string}> {
    return this.http.get<any>(`${this.base}/${idAnteproyecto}/ultima-revision`);
  }

}
