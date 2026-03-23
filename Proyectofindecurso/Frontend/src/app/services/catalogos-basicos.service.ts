import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Universidad {
  idUniversidad?: number;
  nombre: string;
  mision: string;
  vision?: string;
  lema?: string;
  campus?: string;
  direccion?: string;
  contactoInfo?: string;
}

export interface Facultad {
  idFacultad?: number;
  nombre: string;
  idUniversidad: number;
  nombreUniversidad?: string;
}

export interface Carrera {
  idCarrera?: number;
  nombre: string;
  idFacultad: number;
  nombreFacultad?: string;
}

export interface PeriodoTitulacion {
  idPeriodo?: number;
  descripcion: string;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
}

export interface TipoTrabajoTitulacion {
  idTipoTrabajo?: number;
  nombre: string;
  idModalidad: number;
  nombreModalidad?: string;
}

export interface Modalidad {
  idModalidad: number;
  nombre: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogosBasicosService {
  private readonly baseUrl = environment.apiUrl + '/api/catalogos';

  constructor(private http: HttpClient) {}

  // ==================== UNIVERSIDAD ====================
  listarUniversidades(): Observable<Universidad[]> {
    return this.http.get<Universidad[]>(`${this.baseUrl}/universidad`);
  }

  obtenerUniversidad(id: number): Observable<Universidad> {
    return this.http.get<Universidad>(`${this.baseUrl}/universidad/${id}`);
  }

  crearUniversidad(data: Universidad): Observable<Universidad> {
    return this.http.post<Universidad>(`${this.baseUrl}/universidad`, data);
  }

  actualizarUniversidad(id: number, data: Universidad): Observable<Universidad> {
    return this.http.put<Universidad>(`${this.baseUrl}/universidad/${id}`, data);
  }

  eliminarUniversidad(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/universidad/${id}`);
  }

  // ==================== FACULTAD ====================
  listarFacultades(): Observable<Facultad[]> {
    return this.http.get<Facultad[]>(`${this.baseUrl}/facultad`);
  }

  listarFacultadesPorUniversidad(idUniversidad: number): Observable<Facultad[]> {
    return this.http.get<Facultad[]>(`${this.baseUrl}/facultad/universidad/${idUniversidad}`);
  }

  obtenerFacultad(id: number): Observable<Facultad> {
    return this.http.get<Facultad>(`${this.baseUrl}/facultad/${id}`);
  }

  crearFacultad(data: Facultad): Observable<Facultad> {
    return this.http.post<Facultad>(`${this.baseUrl}/facultad`, data);
  }

  actualizarFacultad(id: number, data: Facultad): Observable<Facultad> {
    return this.http.put<Facultad>(`${this.baseUrl}/facultad/${id}`, data);
  }

  eliminarFacultad(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/facultad/${id}`);
  }

  // ==================== CARRERA ====================
  listarCarreras(): Observable<Carrera[]> {
    return this.http.get<Carrera[]>(`${this.baseUrl}/carrera`);
  }

  listarCarrerasPorFacultad(idFacultad: number): Observable<Carrera[]> {
    return this.http.get<Carrera[]>(`${this.baseUrl}/carrera/facultad/${idFacultad}`);
  }

  obtenerCarrera(id: number): Observable<Carrera> {
    return this.http.get<Carrera>(`${this.baseUrl}/carrera/${id}`);
  }

  crearCarrera(data: Carrera): Observable<Carrera> {
    return this.http.post<Carrera>(`${this.baseUrl}/carrera`, data);
  }

  actualizarCarrera(id: number, data: Carrera): Observable<Carrera> {
    return this.http.put<Carrera>(`${this.baseUrl}/carrera/${id}`, data);
  }

  eliminarCarrera(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/carrera/${id}`);
  }

  // ==================== PERIODO ====================
  listarPeriodos(): Observable<PeriodoTitulacion[]> {
    return this.http.get<PeriodoTitulacion[]>(`${this.baseUrl}/periodo`);
  }

  listarPeriodosActivos(): Observable<PeriodoTitulacion[]> {
    return this.http.get<PeriodoTitulacion[]>(`${this.baseUrl}/periodo/activos`);
  }

  obtenerPeriodo(id: number): Observable<PeriodoTitulacion> {
    return this.http.get<PeriodoTitulacion>(`${this.baseUrl}/periodo/${id}`);
  }

  crearPeriodo(data: PeriodoTitulacion): Observable<PeriodoTitulacion> {
    return this.http.post<PeriodoTitulacion>(`${this.baseUrl}/periodo`, data);
  }

  actualizarPeriodo(id: number, data: PeriodoTitulacion): Observable<PeriodoTitulacion> {
    return this.http.put<PeriodoTitulacion>(`${this.baseUrl}/periodo/${id}`, data);
  }

  activarPeriodo(id: number): Observable<PeriodoTitulacion> {
    return this.http.put<PeriodoTitulacion>(`${this.baseUrl}/periodo/${id}/activar`, {});
  }

  desactivarPeriodo(id: number): Observable<PeriodoTitulacion> {
    return this.http.put<PeriodoTitulacion>(`${this.baseUrl}/periodo/${id}/desactivar`, {});
  }

  eliminarPeriodo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/periodo/${id}`);
  }

  // ==================== TIPO TRABAJO ====================
  listarTiposTrabajo(): Observable<TipoTrabajoTitulacion[]> {
    return this.http.get<TipoTrabajoTitulacion[]>(`${this.baseUrl}/tipo-trabajo`);
  }

  listarTiposTrabajoPorModalidad(idModalidad: number): Observable<TipoTrabajoTitulacion[]> {
    return this.http.get<TipoTrabajoTitulacion[]>(`${this.baseUrl}/tipo-trabajo/modalidad/${idModalidad}`);
  }

  obtenerTipoTrabajo(id: number): Observable<TipoTrabajoTitulacion> {
    return this.http.get<TipoTrabajoTitulacion>(`${this.baseUrl}/tipo-trabajo/${id}`);
  }

  crearTipoTrabajo(data: TipoTrabajoTitulacion): Observable<TipoTrabajoTitulacion> {
    return this.http.post<TipoTrabajoTitulacion>(`${this.baseUrl}/tipo-trabajo`, data);
  }

  actualizarTipoTrabajo(id: number, data: TipoTrabajoTitulacion): Observable<TipoTrabajoTitulacion> {
    return this.http.put<TipoTrabajoTitulacion>(`${this.baseUrl}/tipo-trabajo/${id}`, data);
  }

  eliminarTipoTrabajo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tipo-trabajo/${id}`);
  }

  // ==================== MODALIDADES (para usar en selects) ====================
  listarModalidades(): Observable<Modalidad[]> {
    return this.http.get<Modalidad[]>(`${this.baseUrl}/modalidades`);
  }
}
