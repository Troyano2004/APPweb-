import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CatalogoCarrera {
  idCarrera: number;
  nombre: string;
}

export interface CatalogoModalidad {
  idModalidad: number;
  nombre: string;
}

export interface CarreraModalidadDto {
  idCarrera: number;
  carrera: string;
  idModalidad: number;
  modalidad: string;
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class CatalogosService {
  private readonly API_URL = environment.apiUrl + '/api/catalogos';

  constructor(private readonly http: HttpClient) {}

  listarCarreras(): Observable<CatalogoCarrera[]> {
    return this.http.get<CatalogoCarrera[]>(`${this.API_URL}/carreras`);
  }

  listarModalidades(): Observable<CatalogoModalidad[]> {
    return this.http.get<CatalogoModalidad[]>(`${this.API_URL}/modalidades`);
  }

  crearModalidad(nombre: string): Observable<CatalogoModalidad> {
    return this.http.post<CatalogoModalidad>(`${this.API_URL}/modalidades`, { nombre });
  }

  actualizarModalidad(idModalidad: number, nombre: string): Observable<CatalogoModalidad> {
    return this.http.put<CatalogoModalidad>(`${this.API_URL}/modalidades/${idModalidad}`, { nombre });
  }

  eliminarModalidad(idModalidad: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/modalidades/${idModalidad}`);
  }


  listarCarreraModalidad(): Observable<CarreraModalidadDto[]> {
    return this.http.get<CarreraModalidadDto[]>(`${this.API_URL}/carrera-modalidad`);
  }

  asignarCarreraModalidad(idCarrera: number, idModalidad: number): Observable<void> {
    const params = new HttpParams().set('idCarrera', idCarrera).set('idModalidad', idModalidad);
    return this.http.post<void>(`${this.API_URL}/carrera-modalidad`, null, { params });
  }
  eliminarCarreraModalidad(idCarrera: number, idModalidad: number): Observable<void> {
    const params = new HttpParams().set('idCarrera', idCarrera).set('idModalidad', idModalidad);
    return this.http.delete<void>(`${this.API_URL}/carrera-modalidad`, { params });
  }

}
