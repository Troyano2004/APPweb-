import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Estudiante {
  idEstudiante:number;
  promedioRecord80:number;
  discapacidad:boolean;
  usuario?: {
    cedula: string;
    nombres: string;
    apellidos: string;
  };
  carrera?:{
    nombre:string;
  };
}
@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private apiUrl = `${environment.apiUrl}/api/estudiantes`;
  constructor(private http: HttpClient) { }
  getEstudiantes():Observable<any>{
    return this.http.get<any>(this.apiUrl);
  }

}


