
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EstadoComplexivoEstudianteDto {
  tieneComplexivo: boolean;
  idComplexivo: number | null;
  estadoComplexivo: string | null;
  tieneInforme: boolean;
  idInforme: number | null;
  estadoInforme: string | null;
  tieneDocente: boolean;
  nombreDocente: string | null;
}

export interface ComplexivoInformeDto {
  idInforme: number | null;
  idComplexivo: number | null;
  titulo: string | null;
  planteamientoProblema: string | null;
  objetivos: string | null;
  marcoTeorico: string | null;
  metodologia: string | null;
  resultados: string | null;
  conclusiones: string | null;
  bibliografia: string | null;
  estado: string | null;
  observaciones: string | null;
  idDocente: number | null;
  nombreDocente: string | null;
}

export interface ComplexivoInformeUpdateRequest {
  titulo: string;
  planteamientoProblema: string;
  objetivos: string;
  marcoTeorico: string;
  metodologia: string;
  resultados: string;
  conclusiones: string;
  bibliografia: string;
}

export interface DocenteOpcionDto {
  idDocente: number;
  nombre: string;
}

export interface EstudianteComplexivoSinDocenteDto {
  idEstudiante: number;
  nombre: string;
  carrera: string;
  modalidad: string;
  estadoComplexivo: string;
}

export interface ComplexivoDocenteAsignacionResponse {
  idAsignacion: number;
  idEstudiante: number;
  nombreEstudiante: string;
  idDocente: number;
  nombreDocente: string;
  idPeriodo: number;
  periodo: string;
  fechaAsignacion: string;
  activo: boolean;
}

export interface InfoCoordinadorComplexivoDto {
  idCarrera: number;
  carrera: string;
  idPeriodo: number;
  periodo: string;
  docentesDisponibles: DocenteOpcionDto[];
  estudiantesSinDocente: EstudianteComplexivoSinDocenteDto[];
  asignacionesActuales: ComplexivoDocenteAsignacionResponse[];
}

export interface AsignarDocenteComplexivoRequest {
  idEstudiante: number;
  idDocente: number;
  idUsuarioCoordinador: number;
  observacion: string;
}

export interface EstudianteDeDocenteDto {
  idComplexivo: number;
  idEstudiante: number;
  nombreEstudiante: string;
  carrera: string;
  estadoComplexivo: string;
  tieneInforme: boolean;
  estadoInforme: string | null;
}

export interface ComplexivoAsesoriaDto {
  idAsesoria: number;
  idComplexivo: number;
  fecha: string;
  observaciones: string;
  nombreDocente: string;
}

@Injectable({ providedIn: 'root' })
export class ComplexivoService {
  private readonly BASE = 'http://localhost:8080/api/complexivo';

  constructor(private http: HttpClient) {}

  // ── Estudiante ─────────────────────────────────────────────────
  getEstado(idEstudiante: number): Observable<EstadoComplexivoEstudianteDto> {
    return this.http.get<EstadoComplexivoEstudianteDto>(
      `${this.BASE}/estudiante/${idEstudiante}/estado`);
  }

  getInforme(idEstudiante: number): Observable<ComplexivoInformeDto> {
    return this.http.get<ComplexivoInformeDto>(
      `${this.BASE}/estudiante/${idEstudiante}/informe`);
  }

  guardarInforme(idEstudiante: number, req: ComplexivoInformeUpdateRequest): Observable<ComplexivoInformeDto> {
    return this.http.put<ComplexivoInformeDto>(
      `${this.BASE}/estudiante/${idEstudiante}/informe`, req);
  }

  enviarInforme(idEstudiante: number): Observable<ComplexivoInformeDto> {
    return this.http.post<ComplexivoInformeDto>(
      `${this.BASE}/estudiante/${idEstudiante}/informe/enviar`, {});
  }

  // ── Coordinador ────────────────────────────────────────────────
  getInfoCoordinador(idUsuario: number): Observable<InfoCoordinadorComplexivoDto> {
    return this.http.get<InfoCoordinadorComplexivoDto>(
      `${this.BASE}/coordinador/info?idUsuario=${idUsuario}`);
  }

  asignarDocente(req: AsignarDocenteComplexivoRequest): Observable<ComplexivoDocenteAsignacionResponse> {
    return this.http.post<ComplexivoDocenteAsignacionResponse>(
      `${this.BASE}/coordinador/asignar-docente`, req);
  }

  // ── Docente ────────────────────────────────────────────────────
  getMisEstudiantes(idDocente: number): Observable<EstudianteDeDocenteDto[]> {
    return this.http.get<EstudianteDeDocenteDto[]>(
      `${this.BASE}/docente/${idDocente}/estudiantes`);
  }

  getInformeDocente(idDocente: number, idComplexivo: number): Observable<ComplexivoInformeDto> {
    return this.http.get<ComplexivoInformeDto>(
      `${this.BASE}/docente/${idDocente}/informe/${idComplexivo}`);
  }

  aprobarInforme(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.http.post<ComplexivoInformeDto>(
      `${this.BASE}/docente/${idDocente}/informe/${idInforme}/aprobar`,
      { observaciones });
  }

  rechazarInforme(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.http.post<ComplexivoInformeDto>(
      `${this.BASE}/docente/${idDocente}/informe/${idInforme}/rechazar`,
      { observaciones });
  }

  registrarAsesoria(idDocente: number, idComplexivo: number, observaciones: string): Observable<ComplexivoAsesoriaDto> {
    return this.http.post<ComplexivoAsesoriaDto>(
      `${this.BASE}/docente/${idDocente}/asesoria/${idComplexivo}`,
      { observaciones });
  }

  listarAsesorias(idDocente: number, idComplexivo: number): Observable<ComplexivoAsesoriaDto[]> {
    return this.http.get<ComplexivoAsesoriaDto[]>(
      `${this.BASE}/docente/${idDocente}/asesorias/${idComplexivo}`);
  }
}
