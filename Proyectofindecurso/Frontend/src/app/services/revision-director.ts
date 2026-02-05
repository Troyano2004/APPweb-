import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ObservacionDto {
  idObservacion: number;
  seccion: string;          // enum string
  comentario: string;
  estado: string;           // PENDIENTE / ATENDIDA
  autor?: string | null;
  creadoEn?: string | null;
}

export interface CrearObservacionRequest {
  seccion: string;          // ej: "METODOLOGIA"
  comentario: string;
  idAutor: number;          // idDocente
}

export interface DocumentoPendienteDto {
  // Identificación
  id: number;                    // id del documento
  titulo: string | null;

  // Estudiante
  idEstudiante: number;
  nombreEstudiante: string;      // "Juan Pérez"
  carrera?: string | null;

  // Director
  idDirector: number;

  // Estado y seguimiento
  estado: 'EN_REVISION' | 'CORRECCION_REQUERIDA' | 'APROBADO_POR_DIRECTOR';
  fechaEnvio: string;            // ISO date: 2026-02-01T10:30:00
  actualizadoEn?: string | null; // última modificación
}


@Injectable({ providedIn: 'root' })
export class RevisionDirectorService {
  private readonly API_URL = 'http://localhost:8080';
  private readonly baseUrl = `${this.API_URL}/api/titulacion2`;

  constructor(private http: HttpClient) {}

  pendientes(idDocente: number): Observable<DocumentoPendienteDto[]> {
    return this.http.get<DocumentoPendienteDto[]>(`${this.baseUrl}/director/${idDocente}/pendientes`);
  }

  observaciones(idDocumento: number): Observable<ObservacionDto[]> {
    return this.http.get<ObservacionDto[]>(`${this.baseUrl}/documento/${idDocumento}/observaciones`);
  }

  agregarObservacion(idDocente: number, idDocumento: number, payload: CrearObservacionRequest) {
    return this.http.post(`${this.baseUrl}/director/${idDocente}/documento/${idDocumento}/observacion`, payload);
  }

  devolver(idDocente: number, idDocumento: number) {
    return this.http.post(`${this.baseUrl}/director/${idDocente}/documento/${idDocumento}/devolver`, {});
  }

  aprobar(idDocente: number, idDocumento: number) {
    return this.http.post(`${this.baseUrl}/director/${idDocente}/documento/${idDocumento}/aprobar`, {});
  }
}
