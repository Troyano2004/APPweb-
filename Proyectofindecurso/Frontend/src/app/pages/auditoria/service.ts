import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AuditFiltros, AuditConfig, AuditStats, AuditLog } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private base = environment.apiUrl + '/api/auditoria';
  private eventSource: EventSource | null = null;

  // Subject que emite cada nuevo log recibido en tiempo real
  private nuevoLog$ = new Subject<AuditLog>();

  constructor(private http: HttpClient, private ngZone: NgZone) {}

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

  getEntidades(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/entidades`);
  }

  getAcciones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/acciones`);
  }

  getAccionesConfig(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/acciones-config`);
  }

  /**
   * Conecta al stream SSE del backend.
   * Retorna un Observable que emite cada AuditLog nuevo en tiempo real.
   * NgZone es necesario para que la detección de cambios de Angular
   * funcione con eventos que llegan fuera del ciclo de Angular.
   */
  conectarStream(): Observable<AuditLog> {
    this.desconectarStream();

    this.eventSource = new EventSource(`${this.base}/stream`, {
      withCredentials: true  // necesario para sesiones con cookies
    });

    this.eventSource.addEventListener('nuevo-log', (event: MessageEvent) => {
      this.ngZone.run(() => {
        try {
          const log: AuditLog = JSON.parse(event.data);
          this.nuevoLog$.next(log);
        } catch (e) {
          console.error('[SSE] Error parseando log:', e);
        }
      });
    });

    this.eventSource.addEventListener('connected', () => {
      console.log('[SSE] Conexión en vivo establecida con el servidor');
    });

    this.eventSource.onerror = () => {
      console.warn('[SSE] Reconectando...');
      // EventSource reconecta automáticamente al perder conexión
    };

    return this.nuevoLog$.asObservable();
  }

  /** Cierra la conexión SSE */
  desconectarStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  get estaConectado(): boolean {
    return this.eventSource?.readyState === EventSource.OPEN;
  }

  getCambios(f: any): Observable<any> {
    let p = new HttpParams()
      .set('page', f.page)
      .set('size', f.size);
    if (f.entidad)  p = p.set('entidad',  f.entidad);
    if (f.accion)   p = p.set('accion',   f.accion);
    if (f.username) p = p.set('username', f.username);
    if (f.desde)    p = p.set('desde',    f.desde);
    if (f.hasta)    p = p.set('hasta',    f.hasta);
    return this.http.get<any>(`${this.base}/cambios`, { params: p });
  }

  getUsuariosConCambios(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/usuarios-activos`);
  }

  getSesionesActivas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/sesiones`);
  }

  cerrarSesion(sessionId: string): Observable<any> {
    return this.http.delete(`${this.base}/sesiones/${sessionId}`);
  }

  cerrarTodasSesiones(username: string): Observable<any> {
    return this.http.delete(`${this.base}/sesiones/usuario/${username}`);
  }
}
