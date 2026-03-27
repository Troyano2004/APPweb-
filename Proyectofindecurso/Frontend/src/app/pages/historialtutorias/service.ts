import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TutoriaHistorialResponse } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TutoriaHistorialService {
  private base = `${environment.apiUrl}/api/estudiante/tutorias`;

  constructor(private http: HttpClient) {}

  historial(idEstudiante: number, idAnteproyecto: number): Observable<TutoriaHistorialResponse[]> {
    return this.http.get<TutoriaHistorialResponse[]>(
      `${this.base}/historial/${idEstudiante}/${idAnteproyecto}`
    );
  }
}
