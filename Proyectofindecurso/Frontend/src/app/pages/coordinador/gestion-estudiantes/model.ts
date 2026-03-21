export interface EstudianteCarreraResponse {
  idEstudiante: number;
  nombres: string;
  apellidos: string;
  cedula: string;
  carrera: string;
  tieneTutor: boolean;
  tutorNombre?: string;
}
