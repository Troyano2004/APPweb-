import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditFiltros, AuditConfig, AuditStats } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private base = `${environment.apiUrl}/api/auditoria`;

  constructor(private http: HttpClient) {}

  getLogs(f: AuditFiltros): Observable<any> {
    let p = new HttpParams()
      .set('page', f.page.toString())
      .set('size', f.size.toString());
    if (f.entidad   && f.entidad.trim()   !== '') p = p.set('entidad',   f.entidad);
    if (f.accion    && f.accion.trim()    !== '') p = p.set('accion',    f.accion);
    if (f.idUsuario)                              p = p.set('idUsuario', f.idUsuario);
    if (f.desde     && f.desde.trim()     !== '') p = p.set('desde',     f.desde);
    if (f.hasta     && f.hasta.trim()     !== '') p = p.set('hasta',     f.hasta);
    return this.http.get<any>(`${this.base}/logs`, { params: p });
  }

  exportCsv(filtros: AuditFiltros): Observable<Blob> {
    let params = new HttpParams();
    if (filtros.entidad) params = params.set('entidad', filtros.entidad);
    if (filtros.accion)  params = params.set('accion', filtros.accion);
    if (filtros.desde)   params = params.set('desde', filtros.desde);
    if (filtros.hasta)   params = params.set('hasta', filtros.hasta);
    return this.http.get(`${this.base}/logs/export/csv`, { params, responseType: 'blob' });
  }

  getConfigs(): Observable<AuditConfig[]> {
    return this.http.get<AuditConfig[]>(`${this.base}/config`);
  }

  updateConfig(id: number, body: Partial<AuditConfig>): Observable<AuditConfig> {
    return this.http.put<AuditConfig>(`${this.base}/config/${id}`, body);
  }

  toggleConfig(id: number): Observable<AuditConfig> {
    return this.http.patch<AuditConfig>(`${this.base}/config/${id}/toggle`, {});
  }

  createConfig(body: Partial<AuditConfig>): Observable<AuditConfig> {
    return this.http.post<AuditConfig>(`${this.base}/config`, body);
  }

  deleteConfig(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/config/${id}`);
  }

  getStats(): Observable<AuditStats> {
    return this.http.get<AuditStats>(`${this.base}/stats`);
  }
}