import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoDocumento =
  | 'BORRADOR'
  | 'EN_REVISION'
  | 'CORRECCION_REQUERIDA'
  | 'APROBADO_POR_DIRECTOR';

export interface DocumentoTitulacionDto {
  idDocumento?: number;
  id?: number;
  idEstudiante?: number | null;
  idDirector?: number | null;
  estado: EstadoDocumento;

  titulo: string | null;
  resumen: string | null;
  abstractText: string | null;

  introduccion: string | null;
  problema: string | null;
  planteamientoProblema?: string | null;
  objetivosGenerales: string | null;
  objetivoGeneral?: string | null;
  objetivosEspecificos: string | null;
  justificacion: string | null;

  marcoTeorico: string | null;
  metodologia: string | null;

  resultados: string | null;
  discusion: string | null;

  conclusiones: string | null;
  recomendaciones: string | null;

  bibliografia: string | null;
  anexos: string | null;
}

export interface DocumentoUpdateRequest {
  titulo?: string | null;
  resumen?: string | null;
  abstractText?: string | null;

  introduccion?: string | null;
  problema?: string | null;
  planteamientoProblema?: string | null;
  objetivosGenerales?: string | null;
  objetivoGeneral?: string | null;
  objetivosEspecificos?: string | null;
  justificacion?: string | null;

  marcoTeorico?: string | null;
  metodologia?: string | null;

  resultados?: string | null;
  discusion?: string | null;

  conclusiones?: string | null;
  recomendaciones?: string | null;

  bibliografia?: string | null;
  anexos?: string | null;
}

@Injectable({ providedIn: 'root' })
export class DocumentoTitulacionService {
  private readonly API_URL = 'http://localhost:8080';
  private readonly baseUrl = `${this.API_URL}/api/titulacion2`;

  constructor(private http: HttpClient) {}

  getDocumento(idEstudiante: number): Observable<DocumentoTitulacionDto> {
    return this.http.get<DocumentoTitulacionDto>(
      `${this.baseUrl}/estudiante/${idEstudiante}/documento`
    );
  }

  getDocumentoPorId(idDocumento: number): Observable<DocumentoTitulacionDto> {
    return this.http.get<DocumentoTitulacionDto>(
      `${this.baseUrl}/documento/${idDocumento}`
    );
  }

  updateDocumento(idEstudiante: number, payload: DocumentoUpdateRequest): Observable<DocumentoTitulacionDto> {
    return this.http.put<DocumentoTitulacionDto>(
      `${this.baseUrl}/estudiante/${idEstudiante}/documento`,
      payload
    );
  }

  enviarRevision(idEstudiante: number): Observable<DocumentoTitulacionDto> {
    return this.http.post<DocumentoTitulacionDto>(
      `${this.baseUrl}/estudiante/${idEstudiante}/enviar-revision`,
      {}
    );
  }
}
