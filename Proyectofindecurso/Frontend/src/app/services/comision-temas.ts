
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
  idEstudianteSugerente: number | null;
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
  modalidad: string | null;
  planteamientoProblema: string | null;
  objetivosGenerales: string | null;
  objetivosEspecificos: string | null;
  marcoTeorico: string | null;
  metodologia: string | null;
  resultadosEsperados: string | null;
  bibliografia: string | null;
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

// ── DTO para la respuesta del análisis IA ─────────────────────────────────────
export interface RevisionPropuestaIARequest {
  modo?: 'integral' | 'coherencia' | 'pertinencia' | 'viabilidad';
  instruccionAdicional?: string;
}

export interface FeedbackIAPropuesta {
  estado_evaluacion:   'APROBABLE' | 'REQUIERE_AJUSTES' | 'RECHAZABLE' | 'ERROR';
  puntaje_estimado:     number;
  pertinencia_carrera: 'ALTA' | 'MEDIA' | 'BAJA' | 'ERROR';
  analisis_titulo:      string;
  analisis_objetivos:   string;
  analisis_metodologia: string;
  fortalezas:           string[];
  debilidades:          string[];
  sugerencias_mejora:   string[];
  mensaje_estudiante:   string;
}

export interface RevisionPropuestaIAResponse {
  idPropuesta:         number;
  tituloPropuesta:     string;
  nombreEstudiante:    string;
  nombreCarrera:       string;
  nombreFacultad:      string;
  modalidadTitulacion: string;
  estadoPropuesta:     string;
  feedbackIa:          string;   // JSON string — parsear con JSON.parse()
  fechaAnalisisIa:     string;
}

// ── DTO para revisión PREVIA (antes de guardar en BD) ─────────────────────────
export interface RevisionPropuestaPreviaRequest {
  idEstudiante:          number;
  titulo?:               string;
  temaInvestigacion?:    string;
  planteamientoProblema?: string;
  objetivosGenerales?:   string;
  objetivosEspecificos?: string;
  marcoTeorico?:         string;
  metodologia?:          string;
  resultadosEsperados?:  string;
  bibliografia?:         string;
  modo?:                 'integral' | 'coherencia' | 'pertinencia' | 'viabilidad';
  instruccionAdicional?: string;
}
// ─────────────────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class ComisionTemasService {

  private readonly API_URL    = 'http://localhost:8080/api/comision-temas';
  private readonly IA_API_URL = 'http://localhost:8080/api/revision-ia';

  constructor(private readonly http: HttpClient) {}

  // ── Comisión: banco de temas ─────────────────────────────────────────────

  listarBanco(idDocente: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(`${this.API_URL}/docente/${idDocente}/banco`);
  }

  crearTema(idDocente: number, payload: CrearTemaRequest): Observable<TemaBancoDto> {
    return this.http.post<TemaBancoDto>(`${this.API_URL}/docente/${idDocente}/banco`, payload);
  }

  // ── Comisión: propuestas ─────────────────────────────────────────────────

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

  // ── Docente Complexivo ───────────────────────────────────────────────────

  listarPropuestasComplexivo(idDocente: number): Observable<PropuestaTemaDto[]> {
    return this.http.get<PropuestaTemaDto[]>(
      `${this.API_URL}/docente/${idDocente}/propuestas-complexivo`
    );
  }

  decidirPropuestaComplexivo(
    idDocente: number,
    idPropuesta: number,
    estado: 'APROBADA' | 'RECHAZADA',
    observaciones: string
  ): Observable<PropuestaTemaDto> {
    return this.http.post<PropuestaTemaDto>(
      `${this.API_URL}/docente/${idDocente}/propuestas-complexivo/${idPropuesta}/decision`,
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

  obtenerEstadoModalidad(idEstudiante: number): Observable<EstadoModalidadDto> {
    return this.http.get<EstadoModalidadDto>(`${this.API_URL}/estudiante/${idEstudiante}/estado-modalidad`);
  }

  seleccionarModalidad(idEstudiante: number, idModalidad: number): Observable<EstadoModalidadDto> {
    return this.http.post<EstadoModalidadDto>(
      `${this.API_URL}/estudiante/${idEstudiante}/seleccionar-modalidad`,
      { idModalidad }
    );
  }

  listarTemasAprobadosEstudiante(idEstudiante: number): Observable<TemaBancoDto[]> {
    return this.http.get<TemaBancoDto[]>(
      `${this.API_URL}/estudiante/${idEstudiante}/temas-aprobados`
    );
  }

  // ── Sugerencias ──────────────────────────────────────────────────────────

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

  // ── IA: Evaluación de propuesta ──────────────────────────────────────────

  /**
   * Llama al backend IA para evaluar una propuesta ya guardada.
   * POST /api/revision-ia/propuesta/{idPropuesta}
   *
   * El backend detecta automáticamente la carrera y modalidad del estudiante
   * desde la base de datos y genera un análisis personalizado.
   *
   * @param idPropuesta  ID de la propuesta guardada en BD
   * @param modo         tipo de análisis (default: 'integral')
   */
  evaluarPropuestaConIA(
    idPropuesta: number,
    payload: RevisionPropuestaIARequest = { modo: 'integral' }
  ): Observable<RevisionPropuestaIAResponse> {
    return this.http.post<RevisionPropuestaIAResponse>(
      `${this.IA_API_URL}/propuesta/${idPropuesta}`,
      payload
    );
  }

  // ── IA: Revisión PREVIA (sin guardar en BD) ──────────────────────────────

  /**
   * Evalúa el BORRADOR del formulario ANTES de enviarlo.
   * POST /api/revision-ia/propuesta/previa
   *
   * No necesita idPropuesta. Envía los datos del form directamente.
   * El backend obtiene carrera y modalidad usando el idEstudiante.
   */
  evaluarPropuestaConIAPrevia(
    payload: RevisionPropuestaPreviaRequest
  ): Observable<RevisionPropuestaIAResponse> {
    return this.http.post<RevisionPropuestaIAResponse>(
      `${this.IA_API_URL}/propuesta/previa`,
      payload
    );
  }
}
