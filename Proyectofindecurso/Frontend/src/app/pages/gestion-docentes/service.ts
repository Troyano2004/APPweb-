import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DocenteCarreraResponse, AsignarCarreraRequest, CarrerasItem} from './model';

@Injectable({ providedIn: 'root' })
export class GestionDocentesService {

  private readonly base = 'http://localhost:8080/api/admin/docentes';
  constructor(private http: HttpClient) {
  }

    listar(): Observable<DocenteCarreraResponse[]> {
      return this.http.get<DocenteCarreraResponse[]>(this.base);
    }

    asignarCarrera(req: AsignarCarreraRequest): Observable<DocenteCarreraResponse> {
      return this.http.post<DocenteCarreraResponse>(`${this.base}/asignar-carrera`, req);
    }
    filtrarPorCarrera(idCarrera: number): Observable<DocenteCarreraResponse[]> {
    return this.http.get<DocenteCarreraResponse[]>(
      `${this.base}/filtrar?idCarrera=${idCarrera}`
    );
  }

    cambiarEstado(idDocenteCarrera: number, activo: boolean): Observable<DocenteCarreraResponse> {
      return this.http.patch<DocenteCarreraResponse>(`${this.base}/${idDocenteCarrera}/estado?activo=${activo}`, {});
    }

    listarCarreras(): Observable<CarrerasItem[]> {
      return this.http.get<CarrerasItem[]>('http://localhost:8080/api/catalogos/carreras');
    }
  }

