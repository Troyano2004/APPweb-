import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TutoriaHistorialResponse } from './model';

@Injectable({ providedIn: 'root' })
export class TutoriaHistorialService {
  private base = 'http://localhost:8080/api/estudiante/tutorias';

  constructor(private http: HttpClient) {}

  historial(idEstudiante: number, idAnteproyecto: number): Observable<TutoriaHistorialResponse[]> {
    return this.http.get<TutoriaHistorialResponse[]>(
      `${this.base}/historial/${idEstudiante}/${idAnteproyecto}`
    );
  }
}
