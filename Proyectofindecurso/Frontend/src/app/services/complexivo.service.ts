import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EstadoComplexivoEstudianteDto {
  tieneComplexivo: boolean; idComplexivo: number | null;
  estadoComplexivo: string | null; tieneInforme: boolean;
  idInforme: number | null; estadoInforme: string | null;
  tieneDocente: boolean; nombreDocente: string | null;
}
export interface ComplexivoInformeDto {
  idInforme: number | null; idComplexivo: number | null;
  titulo: string | null; planteamientoProblema: string | null;
  objetivos: string | null; marcoTeorico: string | null;
  metodologia: string | null; resultados: string | null;
  conclusiones: string | null; bibliografia: string | null;
  estado: string | null; observaciones: string | null;
  idDocente: number | null; nombreDocente: string | null;
}
export interface ComplexivoInformeUpdateRequest {
  titulo: string; planteamientoProblema: string; objetivos: string;
  marcoTeorico: string; metodologia: string; resultados: string;
  conclusiones: string; bibliografia: string;
}
export interface DocenteOpcionDto { idDocente: number; nombre: string; }
export interface EstudianteComplexivoSinDocenteDto {
  idEstudiante: number; nombre: string; carrera: string;
  modalidad: string; estadoComplexivo: string;
}
export interface ComplexivoDocenteAsignacionResponse {
  idAsignacion: number; idEstudiante: number; nombreEstudiante: string;
  idDocente: number; nombreDocente: string; idPeriodo: number;
  periodo: string; fechaAsignacion: string; activo: boolean;
}
export interface InfoCoordinadorComplexivoDto {
  idCarrera: number; carrera: string; idPeriodo: number; periodo: string;
  docentesDisponibles: DocenteOpcionDto[];
  estudiantesSinDocente: EstudianteComplexivoSinDocenteDto[];
  asignacionesActuales: ComplexivoDocenteAsignacionResponse[];
}
// Alias para DT1 y DT2 (misma estructura)
export type InfoCoordinadorDt1Dto = InfoCoordinadorComplexivoDto;
export type InfoCoordinadorDt2Dto = InfoCoordinadorComplexivoDto;

export interface AsignarDocenteComplexivoRequest {
  idEstudiante: number; idDocente: number;
  idUsuarioCoordinador: number; observacion: string;
}
export interface EstudianteDeDocenteDto {
  idComplexivo: number; idEstudiante: number; nombreEstudiante: string;
  carrera: string; estadoComplexivo: string;
  tieneInforme: boolean; estadoInforme: string | null;
}
export interface ComplexivoAsesoriaDto {
  idAsesoria: number; idComplexivo: number; fecha: string;
  observaciones: string; nombreDocente: string;
}
export interface PropuestaComplexivoDto {
  idPropuesta: number; idEstudiante: number; nombreEstudiante: string;
  titulo: string; planteamientoProblema: string; objetivosGenerales: string;
  objetivosEspecificos: string; metodologia: string; resultadosEsperados: string;
  bibliografia: string; estado: string;
  observacionesComision: string | null; fechaEnvio: string;
}

@Injectable({ providedIn: 'root' })
export class ComplexivoService {
  private readonly BASE = `${environment.apiUrl}/api/complexivo`;

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

  iniciarComplexivo(idEstudiante: number): Observable<EstadoComplexivoEstudianteDto> {
    return this.http.post<EstadoComplexivoEstudianteDto>(
      `${this.BASE}/estudiante/${idEstudiante}/iniciar`, {});
  }

  // ── Coordinador DT1

  // ── Coordinador DT1 ────────────────────────────────────────────
  getInfoCoordinadorDt1(idUsuario: number): Observable<InfoCoordinadorDt1Dto> {
    return this.http.get<InfoCoordinadorDt1Dto>(
      `${this.BASE}/coordinador/dt1/info?idUsuario=${idUsuario}`);
  }
  asignarDt1(req: AsignarDocenteComplexivoRequest): Observable<ComplexivoDocenteAsignacionResponse> {
    return this.http.post<ComplexivoDocenteAsignacionResponse>(
      `${this.BASE}/coordinador/dt1/asignar`, req);
  }

  // ── Coordinador DT2 ────────────────────────────────────────────
  getInfoCoordinadorDt2(idUsuario: number): Observable<InfoCoordinadorDt2Dto> {
    return this.http.get<InfoCoordinadorDt2Dto>(
      `${this.BASE}/coordinador/dt2/info?idUsuario=${idUsuario}`);
  }
  asignarDt2(req: AsignarDocenteComplexivoRequest): Observable<ComplexivoDocenteAsignacionResponse> {
    return this.http.post<ComplexivoDocenteAsignacionResponse>(
      `${this.BASE}/coordinador/dt2/asignar`, req);
  }

  // ── Legacy coordinador ─────────────────────────────────────────
  getInfoCoordinador(idUsuario: number): Observable<InfoCoordinadorComplexivoDto> {
    return this.http.get<InfoCoordinadorComplexivoDto>(
      `${this.BASE}/coordinador/info?idUsuario=${idUsuario}`);
  }
  asignarDocente(req: AsignarDocenteComplexivoRequest): Observable<ComplexivoDocenteAsignacionResponse> {
    return this.http.post<ComplexivoDocenteAsignacionResponse>(
      `${this.BASE}/coordinador/asignar-docente`, req);
  }

  // ── DT1 — propuestas ───────────────────────────────────────────
  getPropuestasDocenteDt1(idDocente: number): Observable<PropuestaComplexivoDto[]> {
    return this.http.get<PropuestaComplexivoDto[]>(
      `${this.BASE}/dt1/${idDocente}/propuestas`);
  }
  decidirPropuestaDt1(idDocente: number, idPropuesta: number,
                      estado: string, observaciones: string): Observable<PropuestaComplexivoDto> {
    return this.http.post<PropuestaComplexivoDto>(
      `${this.BASE}/dt1/${idDocente}/propuestas/${idPropuesta}/decision`,
      { estado, observaciones });
  }

  // ── DT2 — informes ─────────────────────────────────────────────
  getMisEstudiantesDt2(idDocente: number): Observable<EstudianteDeDocenteDto[]> {
    return this.http.get<EstudianteDeDocenteDto[]>(
      `${this.BASE}/dt2/${idDocente}/estudiantes`);
  }
  getInformeDocenteDt2(idDocente: number, idComplexivo: number): Observable<ComplexivoInformeDto> {
    return this.http.get<ComplexivoInformeDto>(
      `${this.BASE}/dt2/${idDocente}/informe/${idComplexivo}`);
  }
  aprobarInformeDt2(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.http.post<ComplexivoInformeDto>(
      `${this.BASE}/dt2/${idDocente}/informe/${idInforme}/aprobar`, { observaciones });
  }
  rechazarInformeDt2(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.http.post<ComplexivoInformeDto>(
      `${this.BASE}/dt2/${idDocente}/informe/${idInforme}/rechazar`, { observaciones });
  }
  registrarAsesoriaDt2(idDocente: number, idComplexivo: number, observaciones: string): Observable<ComplexivoAsesoriaDto> {
    return this.http.post<ComplexivoAsesoriaDto>(
      `${this.BASE}/dt2/${idDocente}/asesoria/${idComplexivo}`, { observaciones });
  }
  listarAsesoriasDt2(idDocente: number, idComplexivo: number): Observable<ComplexivoAsesoriaDto[]> {
    return this.http.get<ComplexivoAsesoriaDto[]>(
      `${this.BASE}/dt2/${idDocente}/asesorias/${idComplexivo}`);
  }

  // ── Legacy docente (compatibilidad) ───────────────────────────
  getPropuestasDocente(idDocente: number): Observable<PropuestaComplexivoDto[]> {
    return this.getPropuestasDocenteDt1(idDocente);
  }
  decidirPropuesta(idDocente: number, idPropuesta: number,
                   estado: string, observaciones: string): Observable<PropuestaComplexivoDto> {
    return this.decidirPropuestaDt1(idDocente, idPropuesta, estado, observaciones);
  }
  getMisEstudiantes(idDocente: number): Observable<EstudianteDeDocenteDto[]> {
    return this.getMisEstudiantesDt2(idDocente);
  }
  getInformeDocente(idDocente: number, idComplexivo: number): Observable<ComplexivoInformeDto> {
    return this.getInformeDocenteDt2(idDocente, idComplexivo);
  }
  aprobarInforme(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.aprobarInformeDt2(idDocente, idInforme, observaciones);
  }
  rechazarInforme(idDocente: number, idInforme: number, observaciones: string): Observable<ComplexivoInformeDto> {
    return this.rechazarInformeDt2(idDocente, idInforme, observaciones);
  }
  registrarAsesoria(idDocente: number, idComplexivo: number, observaciones: string): Observable<ComplexivoAsesoriaDto> {
    return this.registrarAsesoriaDt2(idDocente, idComplexivo, observaciones);
  }
  listarAsesorias(idDocente: number, idComplexivo: number): Observable<ComplexivoAsesoriaDto[]> {
    return this.listarAsesoriasDt2(idDocente, idComplexivo);
  }
}
