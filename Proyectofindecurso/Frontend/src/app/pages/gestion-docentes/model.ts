export interface DocenteCarreraResponse {
  idDocente: number;
  idDocenteCarrera?: number;
  nombres: string;
  apellidos: string;
  username: string;
  idCarrera?: number;
  carrera?: string;
  tieneCarrera: boolean;
  activo: boolean;

}

export interface AsignarCarreraRequest {
  idDocente: number;
  idCarrera: number;
}
export interface CarrerasItem
{
  idCarrera: number;
  nombre: string;
}
