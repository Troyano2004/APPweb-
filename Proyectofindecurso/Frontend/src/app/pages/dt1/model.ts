export interface Dt1Enviado {
  idAnteproyecto: number;
  idEstudiante: number;
  estudiante: string;
  titulo: string;
  estado: string;
  version: number;
  fechaEnvio: string;
}

export interface Dt1Detalle {
  idAnteproyecto: number;
  estadoAnteproyecto: string;
  idVersion: number;
  numeroVersion: number;
  estadoVersion: string;
  fechaEnvio: string;
  estudiante: string;
  periodo: string;
  titulo: string;
  temaInvestigacion: string;
  planteamientoProblema: string;
  objetivosGenerales: string;
  objetivosEspecificos: string;
  marcoTeorico: string;
  metodologia: string;
  resultadosEsperados: string;
  bibliografia: string;
}

/** * Coincide exactamente con Dt1RevisionRequest.java
 */
export interface Dt1RevisionRequest {
  idAnteproyecto: number;
  idDocente: number;
  decision: 'APROBADO' | 'RECHAZADO';
  observacion: string;
}
