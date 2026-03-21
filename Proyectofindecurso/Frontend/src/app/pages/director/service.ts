import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  ActaRevisionDirectorRequest, ActaRevisionDirectorResponse, AnteDirectorItem, ReporteAsistencia, Tutoria,
  TutoriaCalendario, TutoriaCreateRequest
} from './model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DirectorApiService {
  private base = 'http://localhost:8080/api/director';

  constructor(private http: HttpClient) {}

  misAnteproyectos(idDocente: number): Observable<AnteDirectorItem[]> {
    return this.http.get<AnteDirectorItem[]>(`${this.base}/mis-anteproyectos/${idDocente}`);
  }

  tutorias(idAnteproyecto: number, idDocente: number): Observable<Tutoria[]> {
    return this.http.get<Tutoria[]>(`${this.base}/${idAnteproyecto}/tutorias/${idDocente}`);
  }

  programarTutoria(idAnteproyecto: number, idDocente: number, req: TutoriaCreateRequest): Observable<Tutoria> {
    return this.http.post<Tutoria>(`${this.base}/${idAnteproyecto}/tutorias/${idDocente}`, req);
  }

  cancelarTutoria(idTutoria: number, idDocente: number): Observable<Tutoria> {
    return this.http.post<Tutoria>(`${this.base}/tutorias/${idTutoria}/cancelar/${idDocente}`, {});
  }

  obtenerActa(idTutoria: number, idDocente: number): Observable<ActaRevisionDirectorResponse> {
    return this.http.get<ActaRevisionDirectorResponse>(`${this.base}/tutorias/${idTutoria}/acta/${idDocente}`);
  }

  guardarActa(idTutoria: number, idDocente: number, req: ActaRevisionDirectorRequest): Observable<ActaRevisionDirectorResponse> {
    return this.http.post<ActaRevisionDirectorResponse>(`${this.base}/tutorias/${idTutoria}/acta/${idDocente}`, req);
  }
  reporteAsistencia(idDocente: number): Observable<ReporteAsistencia[]> {
    return this.http.get<ReporteAsistencia[]>(`${this.base}/reporte-asistencia/${idDocente}`);
  }
  calendarioTutorias(idDocente: number): Observable<TutoriaCalendario[]> {
    return this.http.get<TutoriaCalendario[]>(`${this.base}/calendario/${idDocente}`);
  }
}
