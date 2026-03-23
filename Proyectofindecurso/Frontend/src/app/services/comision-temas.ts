import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// ── DTOs ────────────────────────────────────────────────────────────────────

export interface TemaBancoDto {
  idTema: number;
  titulo: string;
  descripcion: string;
  carrera: string;
  docente: string;
  estado: string;
  observaciones: string | null;
  idEstudianteSugerente: number | null; // ✅ NUEVO
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

export interface ModalidadSimpleDto {
  idModalidad: number;
  nombre: string;
}

export interface EstadoModalidadDto {
  tieneModalidad: boolean;
  idEleccion: number | null;
  idModalidad: number | null;
  modalidad: string | null;
  idCarrera: number | null;
  modalidadesDisponibles: ModalidadSimpleDto[];
}

// ── Service ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class ComisionTemasService {

  private readonly API_URL = environment.apiUrl + '/api/comision-temas';

  constructor(private readonly http: HttpClient) {}

  // ── Comisión: banco de temas ─────────────────────────────────────────────

  listarBanco(idDocente: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(`${this.API_URL}/docente/${idDocente}/banco`);
  }

  crearTema(idDocente: number, payload: CrearTemaRequest): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(`${this.API_URL}/docente/${idDocente}/banco`, payload);
  }

  // ── Comisión: propuestas de estudiantes ──────────────────────────────────

  listarPropuestasComision(idDocente: number): Observable<PropuestaTemaDto[]> {
    return this.http.get<PropuestaTemaDto[]>(`${this.API_URL}/docente/${idDocente}/propuestas`);
  }

  decidirPropuesta(
    idDocente: number,
    idPropuesta: number,
    estado: 'APROBADA' | 'RECHAZADA',
    observaciones: string
  ): Observable<PropuestaTemaDto> {
    return this.http.post<PropuestaTemaDto>(
      `${this.API_URL}/docente/${idDocente}/propuestas/${idPropuesta}/decision`,
      { estado, observaciones }
    );
  }

  // ── Estudiante ───────────────────────────────────────────────────────────

  crearPropuestaEstudiante(idEstudiante: number, payload: CrearPropuestaRequest): Observable<PropuestaTemaDto> {
    return this.http.post<PropuestaTemaDto>(`${this.API_URL}/estudiante/${idEstudiante}/propuestas`, payload);
  }

  listarPropuestasEstudiante(idEstudiante: number): Observable<PropuestaTemaDto[]> {
    return this.http.get<PropuestaTemaDto[]>(`${this.API_URL}/estudiante/${idEstudiante}/propuestas`);
  }

  listarTemasDisponiblesEstudiante(idEstudiante: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(`${this.API_URL}/estudiante/${idEstudiante}/temas-disponibles`);
  }

  // ── Estudiante: modalidad ────────────────────────────────────────────────

  obtenerEstadoModalidad(idEstudiante: number): Observable<EstadoModalidadDto> {
    return this.http.get<EstadoModalidadDto>(`${this.API_URL}/estudiante/${idEstudiante}/estado-modalidad`);
  }

  seleccionarModalidad(idEstudiante: number, idModalidad: number): Observable<EstadoModalidadDto> {
    return this.http.post<EstadoModalidadDto>(
      `${this.API_URL}/estudiante/${idEstudiante}/seleccionar-modalidad`,
      { idModalidad }
    );
  }

  // ── Sugerencias de temas ─────────────────────────────────────────────────

  sugerirTema(idEstudiante: number, titulo: string, descripcion: string): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(
      `${this.API_URL}/estudiante/${idEstudiante}/sugerir-tema`,
      { titulo, descripcion }
    );
  }

  listarSugerencias(idDocente: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(`${this.API_URL}/docente/${idDocente}/sugerencias`);
  }

  aprobarSugerencia(idDocente: number, idTema: number, observaciones?: string): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(
      `${this.API_URL}/docente/${idDocente}/sugerencias/${idTema}/aprobar`,
      { observaciones: observaciones ?? '' }
    );
  }

  rechazarSugerencia(idDocente: number, idTema: number, observaciones?: string): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(
      `${this.API_URL}/docente/${idDocente}/sugerencias/${idTema}/rechazar`,
      { observaciones: observaciones ?? '' }
    );
  }

  // ── Temas aprobados para un estudiante específico ────────────────────────

  listarTemasAprobadosEstudiante(idEstudiante: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(
      `${this.API_URL}/estudiante/${idEstudiante}/temas-aprobados`
    );
  }
}
