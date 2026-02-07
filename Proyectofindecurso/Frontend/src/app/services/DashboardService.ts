import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardResumen {
  propuestasPendientes: number;
  tutoriasActivas: number;
  proyectosAprobados: number;
  documentosPendientes: number;
}

export interface DashboardItem {
  mensaje: string;
  fecha: string | null;
}

export interface DashboardDetalle {
  alertas: DashboardItem[];
  actividades: DashboardItem[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly API_URL = 'http://localhost:8080/api/dashboard';

  constructor(private http: HttpClient) {}

  getResumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.API_URL}/resumen`);
  }

  getDetalle(): Observable<DashboardDetalle> {
    return this.http.get<DashboardDetalle>(`${this.API_URL}/detalle`);
  }
}
