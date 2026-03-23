import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WorkflowResumenDto {
  idDocumento: number;
  estado: string;
  mensaje: string;
  notaFinal?: number;
  notaDocente?: number;
  notaTribunal?: number;
}

export interface PrepararTribunalRequest {
  avalUrlPdf: string;
  avalComentario?: string;
  porcentajeAntiplagio: number;
  umbralAntiplagio?: number;
  urlInformeAntiplagio?: string;
}

export interface MiembroTribunalRequest {
  idDocente: number;
  cargo: string;
}

export interface AsignarTribunalRequest {
  miembros: MiembroTribunalRequest[];
}

export interface AgendarSustentacionRequest {
  fecha: string;
  hora: string;
  lugar: string;
  observaciones?: string;
  motivoReprogramacion?: string;
}

export interface NotaTribunalRequest {
  idDocente: number;
  nota: number;
}

export interface RegistrarResultadoRequest {
  notaDocente: number;
  notasTribunal: NotaTribunalRequest[];
  actaUrl: string;
  actaFirmadaUrl?: string;
  resultado?: string;
  observaciones?: string;
}

export interface CerrarExpedienteRequest {
  resultadoFinal: string;
  observacionesFinales?: string;
}

@Injectable({ providedIn: 'root' })
export class TitulacionWorkflowService {
  private readonly API_URL = environment.apiUrl;
  private readonly baseUrl = `${this.API_URL}/api/titulacion2/workflow`;

  constructor(private http: HttpClient) {}

  listoParaTribunal(idDocumento: number, payload: PrepararTribunalRequest): Observable<WorkflowResumenDto> {
    return this.http.post<WorkflowResumenDto>(`${this.baseUrl}/documento/${idDocumento}/listo-para-tribunal`, payload);
  }

  asignarTribunal(idDocumento: number, payload: AsignarTribunalRequest): Observable<WorkflowResumenDto> {
    return this.http.post<WorkflowResumenDto>(`${this.baseUrl}/documento/${idDocumento}/asignar-tribunal`, payload);
  }

  agendarSustentacion(idDocumento: number, payload: AgendarSustentacionRequest): Observable<WorkflowResumenDto> {
    return this.http.post<WorkflowResumenDto>(`${this.baseUrl}/documento/${idDocumento}/agendar-sustentacion`, payload);
  }

  registrarResultado(idDocumento: number, payload: RegistrarResultadoRequest): Observable<WorkflowResumenDto> {
    return this.http.post<WorkflowResumenDto>(`${this.baseUrl}/documento/${idDocumento}/registrar-resultado`, payload);
  }

  cerrarExpediente(idDocumento: number, payload: CerrarExpedienteRequest): Observable<WorkflowResumenDto> {
    return this.http.post<WorkflowResumenDto>(`${this.baseUrl}/documento/${idDocumento}/cerrar`, payload);
  }
}
