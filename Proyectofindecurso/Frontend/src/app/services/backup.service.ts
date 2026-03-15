import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ── DTOs ──────────────────────────────────────────────────────────────────────

export interface BackupConfiguracion {
  id?:               number;
  rutaLocal:         string;
  cantidadMaxima:    number;
  comprimir:         boolean;
  guardarEnDrive:    boolean;
  driveFolderId?:    string;
  tipoRespaldo:      'COMPLETO' | 'DIFERENCIAL' | 'INCREMENTAL';
  programadoActivo:  boolean;
  horaProgramada?:   string;  // "HH:mm"
}

export interface BackupHistorial {
  id:             number;
  nombreArchivo:  string;
  rutaCompleta:   string;
  tipoRespaldo:   string;
  tamanioBytes:   number;
  enDrive:        boolean;
  driveFileId?:   string;
  estado:         'EXITOSO' | 'FALLIDO';
  mensajeError?:  string;
  fechaCreacion:  string; // ISO string
}

export interface EjecucionResult {
  exitoso:    boolean;
  mensaje:    string;
  historial?: BackupHistorial;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class BackupService {

  private readonly base = 'http://localhost:8080/admin/backup';

  constructor(private http: HttpClient) {}

  // Configuración
  obtenerConfiguracion(): Observable<BackupConfiguracion> {
    return this.http.get<BackupConfiguracion>(`${this.base}/configuracion`);
  }

  guardarConfiguracion(cfg: BackupConfiguracion): Observable<BackupConfiguracion> {
    return this.http.put<BackupConfiguracion>(`${this.base}/configuracion`, cfg);
  }

  // Historial
  listarHistorial(): Observable<BackupHistorial[]> {
    return this.http.get<BackupHistorial[]>(`${this.base}/historial`);
  }

  eliminarHistorial(id: number): Observable<any> {
    return this.http.delete(`${this.base}/historial/${id}`);
  }

  // Ejecución
  ejecutarBackup(): Observable<EjecucionResult> {
    return this.http.post<EjecucionResult>(`${this.base}/ejecutar`, {});
  }

  // Descarga — abre el archivo directamente en el navegador
  descargarBackup(id: number): void {
    const raw = localStorage.getItem('usuario');
    let token = '';
    if (raw) {
      try { token = JSON.parse(raw)?.token ?? ''; } catch {}
    }
    // Construir URL con el token como parámetro (descarga directa)
    const url = `${this.base}/descargar/${id}`;
    // Usar fetch para poder enviar el header Authorization
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.blob())
      .then(blob => {
        const link = document.createElement('a');
        link.href  = URL.createObjectURL(blob);
        link.download = `backup_${id}.zip`;
        link.click();
        URL.revokeObjectURL(link.href);
      })
      .catch(err => console.error('Error al descargar backup:', err));
  }

  // Formatear tamaño en bytes a unidad legible
  formatearTamanio(bytes: number | null): string {
    if (!bytes || bytes === 0) return '—';
    if (bytes < 1024)                  return bytes + ' B';
    if (bytes < 1024 * 1024)          return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024)   return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }
}
