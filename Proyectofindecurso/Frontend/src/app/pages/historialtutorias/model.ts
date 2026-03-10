export interface TutoriaHistorialResponse {
  idTutoria: number;
  fecha: string;
  hora?: string;
  modalidad: string;
  estado: string;
  linkReunion?: string;
  directorNombre?: string;

  idActa?: number;
  observaciones?: string;
  cumplimiento?: string;
  conclusion?: string;
}
