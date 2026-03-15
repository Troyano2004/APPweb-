export interface CoordinadorAdminResponse {
  idCoordinador: number;
  idUsuario: number;
  nombres: string;
  apellidos: string;
  username: string;
  idCarrera: number;
  carrera: string;
  activo: boolean;
}

export interface AsignarCoordinadorRequest {
  idUsuario: number;
  idCarrera: number;
}

export interface UsuarioCoordinadorItem {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  username: string;
}

export interface CarreraItem {
  idCarrera: number;
  nombre: string;
}
