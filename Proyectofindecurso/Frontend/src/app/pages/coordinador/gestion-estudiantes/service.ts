import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EstudianteCarreraResponse } from './model';

@Injectable({ providedIn: 'root' })
export class GestionEstudiantesService {

  private readonly base = 'http://localhost:8080/api/coordinador/gestion-estudiantes';

  constructor(private http: HttpClient) {}

  listar(idUsuario: number): Observable<EstudianteCarreraResponse[]> {
    return this.http.get<EstudianteCarreraResponse[]>(`${this.base}/${idUsuario}`);
  }
}
