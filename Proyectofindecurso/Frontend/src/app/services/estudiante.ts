import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Estudiante {
  idEstudiante: number;
  promedioRecord80: number;
  discapacidad: boolean;
  usuario?: {
    cedula: string;
    nombres: string;
    apellidos: string;
  };
  carrera?: {
    nombre: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private apiUrl = 'http://localhost:8080/api/estudiantes';

  constructor(private http: HttpClient) {}

  getEstudiantes(): Observable<any> {
    // any a propósito, porque a veces el backend devuelve [] o {data:[]} o {content:[]}
    return this.http.get<any>(this.apiUrl);
  }
}
