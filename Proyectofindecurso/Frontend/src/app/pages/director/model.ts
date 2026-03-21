export type Modalidad = 'PRESENCIAL' | 'VIRTUAL';
export type EstadoTutoria = 'PROGRAMADA' | 'REALIZADA' | 'CANCELADA';
export type Cumplimiento = 'NINGUNO' | 'PARCIAL' | 'COMPLETO';

export interface AnteDirectorItem {
  idAnteproyecto: number;
  tituloProyecto: string;
  estudianteNombre: string;
  periodo: string;
  estadoAnteproyecto: string;
}

export interface TutoriaCreateRequest {
  fecha: string;     // yyyy-mm-dd
  hora?: string;     // HH:mm
  modalidad: Modalidad;
}

export interface Tutoria {
  idTutoria: number;
  fecha: string;
  hora?: string;
  modalidad: Modalidad;
  estado: EstadoTutoria;
  linkReunion?: string;  // ← agregar
}

export interface ActaRevisionDirectorRequest {
  directorCargo: string;
  directorFirma?: string;
  estudianteCargo: string;
  estudianteFirma?: string;

  tituloProyecto: string;
  objetivo: string;
  detalleRevision: string;
  cumplimiento: Cumplimiento;
  observaciones?: string;
  conclusion: string;
}

export interface ActaRevisionDirectorResponse extends ActaRevisionDirectorRequest {
  idActa: number;
  idTutoria: number;
  directorNombre: string;
  observaciones?: string;
  estudianteNombre: string;
}
export interface TutoriaCalendario {
  idTutoria: number;
  fecha: string;
  hora?: string;
  modalidad: Modalidad;
  estado: EstadoTutoria;
  linkReunion?: string;
  estudianteNombre: string;
  tituloProyecto: string;
}
export interface TutoriaReporteItem {
  idTutoria: number;
  fecha: string;
  hora?: string;
  modalidad: Modalidad;
}

export interface ReporteAsistencia {
  idEstudiante: number;
  estudiante: string;
  tituloProyecto: string;
  totalTutorias: number;
  tutorias: TutoriaReporteItem[];
}
