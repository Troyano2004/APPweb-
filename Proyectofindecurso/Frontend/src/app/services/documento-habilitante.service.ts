
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HabilitanteDto {
  id?: number;
  idProyecto?: number;
  idEstudiante?: number;
  nombreEstudiante?: string;

  tipoDocumento: string;
  etiquetaTipo: string;
  descripcionTipo: string;
  obligatorio: boolean;

  nombreArchivo?: string | null;
  urlArchivo?: string | null;
  formato?: string | null;

  porcentajeCoincidencia?: number | null;
  umbralPermitido?: number | null;
  resultadoAntiplagio?: string | null;

  estado: string;
  comentarioValidacion?: string | null;
  validadoPorNombre?: string | null;
  fechaValidacion?: string | null;
  fechaSubida?: string | null;
  actualizadoEn?: string | null;
}

export interface ResumenHabilitacionDto {
  idProyecto: number;
  tituloProyecto: string;
  habilitadoParaSustentacion: boolean;
  totalDocumentos: number;
  aprobados: number;
  pendientes: number;
  rechazados: number;
  documentos: HabilitanteDto[];
}

export interface SubirHabilitanteRequest {
  idProyecto: number;
  tipoDocumento: string;
  urlArchivo: string;
  nombreArchivo: string;
  porcentajeCoincidencia?: number;
  umbralPermitido?: number;
}

export interface ValidarHabilitanteRequest {
  decision: 'APROBADO' | 'RECHAZADO';
  comentario?: string;
  porcentajeCoincidencia?: number;
}
export interface SubirAntiplagioPorDirectorRequest {
  urlArchivo: string;
  nombreArchivo: string;
  porcentajeCoincidencia: number;
}
@Injectable({
  providedIn: 'root'
})
export class DocumentoHabilitanteService {
  private readonly API_URL = 'http://localhost:8080';
  private readonly base = `${this.API_URL}/api/habilitantes`;

  constructor(private http: HttpClient) {}

  getResumenEstudiante(idEstudiante: number): Observable<ResumenHabilitacionDto> {
    return this.http.get<ResumenHabilitacionDto>(
      `${this.base}/estudiante/${idEstudiante}/resumen`
    );
  }

  subirDocumento(idEstudiante: number, req: SubirHabilitanteRequest): Observable<HabilitanteDto> {
    return this.http.post<HabilitanteDto>(
      `${this.base}/estudiante/${idEstudiante}/subir`,
      req
    );
  }

  getResumenProyecto(idProyecto: number): Observable<ResumenHabilitacionDto> {
    return this.http.get<ResumenHabilitacionDto>(
      `${this.base}/proyecto/${idProyecto}/resumen`
    );
  }

  getPendientesDirector(idDocente: number): Observable<HabilitanteDto[]> {
    return this.http.get<HabilitanteDto[]>(
      `${this.base}/director/${idDocente}/pendientes`
    );
  }

  validarDocumento(
    idDocente: number,
    idHabilitante: number,
    req: ValidarHabilitanteRequest
  ): Observable<HabilitanteDto> {
    return this.http.post<HabilitanteDto>(
      `${this.base}/director/${idDocente}/validar/${idHabilitante}`,
      req
    );
  }

  uploadArchivo(file: File): Observable<{ url: string; filename: string }> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<{ url: string; filename: string }>(
      `${this.API_URL}/api/uploads/files`,
      formData
    );
  }
  getResumenComplexivo(idEstudiante: number): Observable<ResumenHabilitacionDto> {
    return this.http.get<ResumenHabilitacionDto>(
      `${this.base}/estudiante/${idEstudiante}/resumen-complexivo`
    );
  }

  subirDocumentoComplexivo(
    idEstudiante: number,
    req: SubirHabilitanteRequest
  ): Observable<HabilitanteDto> {
    return this.http.post<HabilitanteDto>(
      `${this.base}/estudiante/${idEstudiante}/subir-complexivo`,
      req
    );

  }
  subirCertificadoAntiplagio(
    idDocente: number,
    idProyecto: number,
    req: SubirAntiplagioPorDirectorRequest
  ): Observable<HabilitanteDto> {
    return this.http.post<HabilitanteDto>(
      `${this.base}/director/${idDocente}/proyecto/${idProyecto}/antiplagio`,
      req
    );
  }
}
