import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { CoordinadorAdminResponse, AsignarCoordinadorRequest,CarreraItem } from './model';


@Injectable({providedIn:'root'})
export class GestionCoordinadoresService{
  private readonly base = 'http://localhost:8080/api/coordinador';
  constructor(private http:HttpClient) {
  }
  listar():Observable<CoordinadorAdminResponse[]>
  {
    return this.http.get<CoordinadorAdminResponse[]>(`${this.base}/coordinadores`)
  }
  asignar(req:AsignarCoordinadorRequest):Observable<CoordinadorAdminResponse>{
    return this.http.post<CoordinadorAdminResponse>(`${this.base}/coordinadores`, req);
  }
  cambiarEstado(id: number, activo: boolean): Observable<CoordinadorAdminResponse> {
    return this.http.patch<CoordinadorAdminResponse>(
      `${this.base}/coordinadores/${id}/estado?activo=${activo}`, {}
    );
  }
  listarCarreras(): Observable<CarreraItem[]> {
    return this.http.get<CarreraItem[]>('http://localhost:8080/api/catalogos/carreras');
  }
  listarUsuariosCoordinador(): Observable<CoordinadorAdminResponse[]> {
    return this.http.get<CoordinadorAdminResponse[]>(`${this.base}/usuarios-disponibles`);
  }
}

