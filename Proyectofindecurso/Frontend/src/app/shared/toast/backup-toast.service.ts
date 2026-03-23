import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BackupToast {
  id:        string;
  tipo:      'EXITOSO' | 'FALLIDO' | 'INFO';
  jobNombre: string;
  database:  string;
  duracion:  number;
  tamano:    number;
  manual:    boolean;
  timestamp: Date;
}

@Injectable({ providedIn: 'root' })
export class BackupToastService {

  private readonly _toasts = new BehaviorSubject<BackupToast[]>([]);
  toasts$ = this._toasts.asObservable();

  private eventSource: EventSource | null = null;

  constructor(private zone: NgZone) {}

  // ── Conectar al stream SSE del backend ────────────────────────────────────

  conectar(token: string): void {
    if (this.eventSource) return; // ya conectado

    const url = `${environment.apiUrl}/api/backup/eventos`;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('backup-completado', (event: MessageEvent) => {
      this.zone.run(() => {
        try {
          const data = JSON.parse(event.data);
          this.agregarToast({
            id:        crypto.randomUUID(),
            tipo:      data.estado === 'EXITOSO' ? 'EXITOSO' : 'FALLIDO',
            jobNombre: data.jobNombre,
            database:  data.databaseNombre,
            duracion:  data.duracion,
            tamano:    data.tamano,
            manual:    data.manual,
            timestamp: new Date()
          });
        } catch (e) {
          console.error('Error parseando evento SSE', e);
        }
      });
    });

    this.eventSource.onerror = () => {
      // Reconectar en 5 segundos si se pierde la conexión
      this.eventSource?.close();
      this.eventSource = null;
      setTimeout(() => this.conectar(token), 5000);
    };
  }

  desconectar(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }

  // ── Gestión de toasts ─────────────────────────────────────────────────────

  agregarToast(toast: BackupToast): void {
    const actuales = this._toasts.getValue();
    this._toasts.next([...actuales, toast]);

    // Auto-eliminar después de 6 segundos
    setTimeout(() => this.eliminarToast(toast.id), 6000);
  }

  eliminarToast(id: string): void {
    const actuales = this._toasts.getValue();
    this._toasts.next(actuales.filter(t => t.id !== id));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  formatBytes(bytes: number): string {
    if (!bytes) return '-';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1048576)    return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1073741824) return `${(bytes / 1048576).toFixed(1)} MB`;
    return `${(bytes / 1073741824).toFixed(2)} GB`;
  }
}
