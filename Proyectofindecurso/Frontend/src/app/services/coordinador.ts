import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SeguimientoProyecto {
  idProyecto: number;
  estudiante: string;
  tituloProyecto: string;
  director: string;
  estado: string;
  ultimaRevision: string | null;
  avance: number | null;
}

export interface EstudianteSinDirector {
  idDocumento: number;
  estudiante: string;
  carrera: string | null;
  proyecto: string | null;
}

export interface DirectorCarga {
  idDocente: number;
  director: string;
  proyectosAsignados: number;
}

export interface ObservacionAdministrativa {
  id: number;
  idProyecto: number;
  proyecto: string;
  tipo: string;
  detalle: string;
  creadoPor: string | null;
  creadoEn: string | null;
}

export interface ComisionFormativa {
  idComision: number;
  carrera: string;
  periodoAcademico: string;
  estado: string;
  miembros: Array<{ idDocente: number; docente: string; cargo: string }>;
}

@Injectable({ providedIn: 'root' })
export class CoordinadorService {
  private readonly API_URL = 'http://localhost:8080/api/coordinador';

  constructor(private http: HttpClient) {}

  getSeguimiento(): Observable<SeguimientoProyecto[]> {
    return this.http.get<SeguimientoProyecto[]>(`${this.API_URL}/seguimiento`);
  }

  getEstudiantesSinDirector(): Observable<EstudianteSinDirector[]> {
    return this.http.get<EstudianteSinDirector[]>(`${this.API_URL}/directores/sin-asignar`);
  }

  getCargaDirectores(): Observable<DirectorCarga[]> {
    return this.http.get<DirectorCarga[]>(`${this.API_URL}/directores/carga`);
  }

  asignarDirector(payload: { idDocumento: number; idDocente: number; motivo: string }): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/directores/asignar`, payload);
  }

  validarProyecto(idProyecto: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/validacion/${idProyecto}`, {});
  }

  getObservaciones(idProyecto?: number): Observable<ObservacionAdministrativa[]> {
    const params = idProyecto ? `?idProyecto=${idProyecto}` : '';
    return this.http.get<ObservacionAdministrativa[]>(`${this.API_URL}/observaciones${params}`);
  }

  crearObservacion(payload: {
    idProyecto: number;
    tipo: string;
    detalle: string;
    creadoPor: string;
  }): Observable<ObservacionAdministrativa> {
    return this.http.post<ObservacionAdministrativa>(`${this.API_URL}/observaciones`, payload);
  }

  getComisiones(): Observable<ComisionFormativa[]> {
    return this.http.get<ComisionFormativa[]>(`${this.API_URL}/comisiones`);
  }

  crearComision(payload: {
    idCarrera: number;
    periodoAcademico: string;
    estado: string;
  }): Observable<ComisionFormativa> {
    return this.http.post<ComisionFormativa>(`${this.API_URL}/comisiones`, payload);
  }

  asignarMiembros(idComision: number, miembros: Array<{ idDocente: number; cargo: string }>): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/comisiones/${idComision}/miembros`, { miembros });
  }

  asignarComisionProyecto(payload: {
    idComision: number;
    idProyecto: number;
    resolucionActa: string;
    observacion: string;
    estado: string;
    fechaConformacion: string;
  }): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/comisiones/asignar-proyecto`, payload);
  }
}
