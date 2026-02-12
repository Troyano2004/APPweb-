import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TemaBancoDto {
  idTema: number;
  titulo: string;
  descripcion: string;
  carrera: string;
  docente: string;
  estado: string;
  observaciones: string | null;
}

export interface PropuestaTemaDto {
  idPropuesta: number;
  titulo: string;
  tema: string;
  estudiante: string;
  carrera: string;
  estado: string;
  fechaEnvio: string | null;
  observaciones: string | null;
}

export interface CrearTemaRequest {
  idCarrera: number;
  titulo: string;
  descripcion: string;
  observaciones?: string;
}

export interface CrearPropuestaRequest {
  idCarrera?: number;
  idTema?: number;
  titulo: string;
  temaInvestigacion?: string;
  planteamientoProblema?: string;
  objetivosGenerales?: string;
  objetivosEspecificos?: string;
  marcoTeorico?: string;
  metodologia?: string;
  resultadosEsperados?: string;
  bibliografia?: string;
}

@Injectable({ providedIn: 'root' })
export class ComisionTemasService {
  private readonly API_URL = 'http://localhost:8080/api/comision-temas';

  constructor(private readonly http: HttpClient) {}

  listarBanco(idDocente: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(`${this.API_URL}/docente/${idDocente}/banco`);
  }

  crearTema(idDocente: number, payload: CrearTemaRequest): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(`${this.API_URL}/docente/${idDocente}/banco`, payload);
  }

  listarPropuestasComision(idDocente: number): Observable<PropuestaTemaDto[]> {
    return this.http.get<PropuestaTemaDto[]>(`${this.API_URL}/docente/${idDocente}/propuestas`);
  }

  decidirPropuesta(
    idDocente: number,
    idPropuesta: number,
    estado: 'APROBADA' | 'RECHAZADA',
    observaciones: string
  ): Observable<PropuestaTemaDto> {
    return this.http.post<PropuestaTemaDto>(`${this.API_URL}/docente/${idDocente}/propuestas/${idPropuesta}/decision`, {
      estado,
      observaciones
    });
  }

  crearPropuestaEstudiante(idEstudiante: number, payload: CrearPropuestaRequest): Observable<PropuestaTemaDto> {
    return this.http.post<PropuestaTemaDto>(`${this.API_URL}/estudiante/${idEstudiante}/propuestas`, payload);
  }

  listarPropuestasEstudiante(idEstudiante: number): Observable<PropuestaTemaDto[]> {
    return this.http.get<PropuestaTemaDto[]>(`${this.API_URL}/estudiante/${idEstudiante}/propuestas`);
  }
}
