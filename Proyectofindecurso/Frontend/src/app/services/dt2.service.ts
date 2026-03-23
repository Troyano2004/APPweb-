import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// ── M1 ────────────────────────────────────────────────────────────────────────
export interface ProyectoPendienteConfiguracionDto {
  idProyecto: number;
  titulo: string;
  estudiante: string;
  carrera: string;
  periodo: string;
  estadoProyecto: string;
  tieneDocenteDt2: boolean;
  tieneDirector: boolean;
  tieneTribunal: boolean;
  configuracionCompleta: boolean;
}

export interface MiembroTribunalDto {
  idTribunal: number;
  idDocente: number;
  nombre: string;
  cargo: string;
}

export interface BitacoraDto {
  tipo: string;
  nombreAsignado: string;
  cargo: string;
  realizadoPor: string;
  fecha: string;
  observacion: string;
}

export interface ConfiguracionProyectoDto {
  idProyecto: number;
  titulo: string;
  estadoProyecto: string;
  docenteDt2: string;
  idDocenteDt2: number;
  director: string;
  idDirector: number;
  tribunal: MiembroTribunalDto[];
  configuracionCompleta: boolean;
  bitacora: BitacoraDto[];
}

export interface AsignarDocenteDt2Request {
  idDocenteDt2: number;
  idRealizadoPor: number;
  periodo: string;
  observacion?: string;
}

export interface AsignarDirectorRequest {
  idDirector: number;
  idRealizadoPor: number;
  motivo?: string;
  periodo: string;
}

export interface AsignarTribunalDt2Request {
  idRealizadoPor: number;
  periodo: string;
  miembros: { idDocente: number; cargo: string }[];
}

// ── M2 ────────────────────────────────────────────────────────────────────────
export interface AsesoriaDto {
  idAsesoria: number;
  fecha: string;
  observaciones: string;
  evidenciaUrl: string;
  porcentajeAvance: number;
  numeroCorte: number;
  calificacion: number;
}

export interface RegistrarAsesoriaRequest {
  idDirector: number;
  fecha: string;
  observaciones: string;
  evidenciaUrl?: string;
  porcentajeAvance: number;
  numeroCorte: number;
  calificacion?: number;
}

export interface ActaCorteDto {
  idActaCorte: number;
  numeroCorte: number;
  fechaGeneracion: string;
  totalAsesorias: number;
  asesoriasSuficientes: boolean;
  notaCorte: number;
  observaciones: string;
  urlActaPdf: string;
  advertencia: string;
}

export interface CerrarCorteRequest {
  idDirector: number;
  numeroCorte: number;
  notaCorte: number;
  observaciones?: string;
}

export interface SeguimientoDto {
  idProyecto: number;
  titulo: string;
  estudiante: string;
  estadoProyecto: string;
  totalAsesorias: number;
  asesoriaCorte1: number;
  asesoriaCorte2: number;
  ultimoAvance: number;
  actas: ActaCorteDto[];
}

// ── M3 ────────────────────────────────────────────────────────────────────────
export interface AntiplacioIntentoDto {
  idIntento: number;
  fechaIntento: string;
  porcentajeCoincidencia: number;
  urlInforme: string;
  favorable: boolean;
  observaciones: string;
  resultado: string;
}

export interface CertificadoAntiplacioDto {
  idProyecto: number;
  certificadoFavorable: boolean;
  ultimoPorcentaje: number;
  urlUltimoInforme: string;
  fechaUltimoIntento: string;
  totalIntentos: number;
  historial: AntiplacioIntentoDto[];
}

// ── M4 ────────────────────────────────────────────────────────────────────────
export interface EvaluacionMiembroDto {
  idDocente: number;
  nombreDocente: string;
  cargo: string;
  nota: number;
  observaciones: string;
  solicitudCorrecciones: boolean;
}

export interface PredefensaDto {
  idSustentacion: number;
  fecha: string;
  hora: string;
  lugar: string;
  estado: string;
  notaDocenteDt2: number;
  promedioTribunal: number;
  notaFinalPonderada: number;
  evaluacionesTribunal: EvaluacionMiembroDto[];
  solicitudCorrecciones: boolean;
  observacionesTribunal: string;
  miembrosQueCalificaron: number;
  totalMiembrosTribunal: number;
}

export interface ProgramarPredefensaRequest {
  idRealizadoPor: number;
  fecha: string;
  hora: string;
  lugar: string;
  observaciones?: string;
}

export interface CalificarPredefensaDocenteRequest {
  idDocenteDt2: number;
  nota: number;
  observaciones?: string;
}

export interface CalificarPredefensaTribunalRequest {
  idDocente: number;
  nota: number;
  observaciones?: string;
  solicitudCorrecciones?: boolean;
}

// ── M5 ────────────────────────────────────────────────────────────────────────
export interface DocumentosPreviosDto {
  ejemplarImpreso: boolean;
  copiaDigitalBiblioteca: boolean;
  copiasDigitalesTribunal: boolean;
  informeCompilatioFirmado: boolean;
  completo: boolean;
  observaciones: string;
}

export interface DocumentosPreviosRequest {
  idRealizadoPor: number;
  ejemplarImpreso: boolean;
  copiaDigitalBiblioteca: boolean;
  copiasDigitalesTribunal: boolean;
  informeCompilatioFirmado: boolean;
  observaciones?: string;
}

export interface ProgramarSustentacionRequest {
  idRealizadoPor: number;
  fecha: string;
  hora: string;
  lugar: string;
  observaciones?: string;
}

export interface CalificarSustentacionRequest {
  idDocente: number;
  calidadTrabajo: number;
  originalidad: number;
  dominioTema: number;
  preguntas: number;
  observaciones?: string;
}

export interface ResultadoSustentacionDto {
  idProyecto: number;
  titulo: string;
  estudiante: string;
  fechaSustentacion: string;
  resultado: string;
  promedioTribunal: number;
  notaGradoFinal: number;
  promedioRecordAcademico: number;
  evaluaciones: EvaluacionMiembroDto[];
  habilitadoSegundaOportunidad: boolean;
  fechaLimiteSegundaOportunidad: string;
}

export interface SegundaOportunidadRequest {
  idRealizadoPor: number;
  fechaSustentacion: string;
  hora: string;
  lugar: string;
  observaciones?: string;
}

export interface MensajeDto {
  mensaje: string;
  estado: string;
  exito: boolean;
}

// ── Service ───────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class Dt2Service {
  private readonly base = environment.apiUrl + '/api/dt2';

  constructor(private http: HttpClient) {}

  // ── M1 ──────────────────────────────────────────────────────────────────────
  listarPendientesConfiguracion(): Observable<ProyectoPendienteConfiguracionDto[]> {
    return this.http.get<ProyectoPendienteConfiguracionDto[]>(`${this.base}/proyectos/pendientes-configuracion`);
  }

  getConfiguracion(idProyecto: number): Observable<ConfiguracionProyectoDto> {
    return this.http.get<ConfiguracionProyectoDto>(`${this.base}/proyectos/${idProyecto}/configuracion`);
  }

  asignarDocenteDt2(idProyecto: number, req: AsignarDocenteDt2Request): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/asignar-docente-dt2`, req);
  }

  asignarDirector(idProyecto: number, req: AsignarDirectorRequest): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/asignar-director`, req);
  }

  asignarTribunal(idProyecto: number, req: AsignarTribunalDt2Request): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/asignar-tribunal`, req);
  }

  // ── M2 ──────────────────────────────────────────────────────────────────────
  listarProyectosDirector(idDirector: number): Observable<ProyectoPendienteConfiguracionDto[]> {
    return this.http.get<ProyectoPendienteConfiguracionDto[]>(`${this.base}/director/${idDirector}/proyectos`);
  }

  // ✅ NUEVO: proyectos asignados al Docente DT2 con documento en APROBADO_POR_DIRECTOR

  // ✅ NUEVO: proyectos en estado PREDEFENSA (para coordinador)
  listarProyectosEnPredefensa(): Observable<ProyectoPendienteConfiguracionDto[]> {
    return this.http.get<ProyectoPendienteConfiguracionDto[]>(`${this.base}/proyectos/en-predefensa`);
  }

  listarProyectosDocenteDt2(idDocenteDt2: number): Observable<ProyectoPendienteConfiguracionDto[]> {
    return this.http.get<ProyectoPendienteConfiguracionDto[]>(`${this.base}/docente-dt2/${idDocenteDt2}/proyectos`);
  }

  // ✅ NUEVO: proyectos en PREDEFENSA donde el docente es tribunal
  listarProyectosTribunal(idDocente: number): Observable<ProyectoPendienteConfiguracionDto[]> {
    return this.http.get<ProyectoPendienteConfiguracionDto[]>(`${this.base}/tribunal/${idDocente}/proyectos`);
  }

  registrarAsesoria(idProyecto: number, req: RegistrarAsesoriaRequest): Observable<AsesoriaDto> {
    return this.http.post<AsesoriaDto>(`${this.base}/proyectos/${idProyecto}/asesorias`, req);
  }

  listarAsesorias(idProyecto: number): Observable<AsesoriaDto[]> {
    return this.http.get<AsesoriaDto[]>(`${this.base}/proyectos/${idProyecto}/asesorias`);
  }

  cerrarCorte(idProyecto: number, req: CerrarCorteRequest): Observable<ActaCorteDto> {
    return this.http.post<ActaCorteDto>(`${this.base}/proyectos/${idProyecto}/acta-corte`, req);
  }

  getSeguimiento(idProyecto: number): Observable<SeguimientoDto> {
    return this.http.get<SeguimientoDto>(`${this.base}/proyectos/${idProyecto}/seguimiento`);
  }

  // ── M3 ──────────────────────────────────────────────────────────────────────
  registrarAntiplagio(idProyecto: number, formData: FormData): Observable<CertificadoAntiplacioDto> {
    return this.http.post<CertificadoAntiplacioDto>(`${this.base}/proyectos/${idProyecto}/antiplagio`, formData);
  }

  getCertificado(idProyecto: number): Observable<CertificadoAntiplacioDto> {
    return this.http.get<CertificadoAntiplacioDto>(`${this.base}/proyectos/${idProyecto}/antiplagio`);
  }

  // ── M4 ──────────────────────────────────────────────────────────────────────
  programarPredefensa(idProyecto: number, req: ProgramarPredefensaRequest): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/predefensa/programar`, req);
  }

  getPredefensa(idProyecto: number): Observable<PredefensaDto> {
    return this.http.get<PredefensaDto>(`${this.base}/proyectos/${idProyecto}/predefensa`);
  }

  calificarPredefensaDocente(idProyecto: number, req: CalificarPredefensaDocenteRequest): Observable<PredefensaDto> {
    return this.http.post<PredefensaDto>(`${this.base}/proyectos/${idProyecto}/predefensa/calificar-docente`, req);
  }

  calificarPredefensaTribunal(idProyecto: number, req: CalificarPredefensaTribunalRequest): Observable<PredefensaDto> {
    return this.http.post<PredefensaDto>(`${this.base}/proyectos/${idProyecto}/predefensa/calificar-tribunal`, req);
  }

  // ── M5 ──────────────────────────────────────────────────────────────────────
  getDocumentosPrevios(idProyecto: number): Observable<DocumentosPreviosDto> {
    return this.http.get<DocumentosPreviosDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/documentos`);
  }

  registrarDocumentosPrevios(idProyecto: number, req: DocumentosPreviosRequest): Observable<DocumentosPreviosDto> {
    return this.http.post<DocumentosPreviosDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/documentos`, req);
  }

  programarSustentacion(idProyecto: number, req: ProgramarSustentacionRequest): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/programar`, req);
  }

  calificarSustentacion(idProyecto: number, req: CalificarSustentacionRequest): Observable<ResultadoSustentacionDto> {
    return this.http.post<ResultadoSustentacionDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/calificar`, req);
  }

  consolidarResultado(idProyecto: number): Observable<ResultadoSustentacionDto> {
    return this.http.post<ResultadoSustentacionDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/consolidar`, {});
  }

  getResultado(idProyecto: number): Observable<any> {
    return this.http.get<any>(`${this.base}/proyectos/${idProyecto}/sustentacion/resultado`);
  }

  habilitarSegundaOportunidad(idProyecto: number, req: SegundaOportunidadRequest): Observable<MensajeDto> {
    return this.http.post<MensajeDto>(`${this.base}/proyectos/${idProyecto}/sustentacion/segunda-oportunidad`, req);
  }
}
