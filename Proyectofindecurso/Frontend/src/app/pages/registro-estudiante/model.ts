export interface EnviarCodigoRequest {
  correo: string;
}

export interface VerificarCodigoRequest {
  correo: string;
  codigo: string;
}

export interface SolicitudRegistroRequest {
  cedula: string;
  nombres: string;
  apellidos: string;
  correo: string;
  idCarrera: number;
}

export interface SolicitudRegistroResponse {
  idSolicitud?: number;
  correo?: string;
  estado?: string;
  mensaje?: string;
}

export interface CarreraItem {
  idCarrera: number;
  nombre: string;
}
