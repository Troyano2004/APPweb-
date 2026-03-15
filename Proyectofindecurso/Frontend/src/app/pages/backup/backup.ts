import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BackupService,
  BackupConfiguracion,
  BackupHistorial,
  EjecucionResult
} from '../../services/backup.service';

@Component({
  selector: 'app-backup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backup.html',
  styleUrls: ['./backup.scss']
})
export class BackupComponent implements OnInit {

  // ── Estado UI ──────────────────────────────────────────────────────────────
  tabActiva: 'config' | 'historial' = 'config';
  cargandoConfig   = false;
  cargandoBackup   = false;
  cargandoHistorial= false;
  guardandoConfig  = false;

  mensajeExito  = '';
  mensajeError  = '';

  // ── Datos ──────────────────────────────────────────────────────────────────
  config: BackupConfiguracion = {
    rutaLocal:        'C:/respaldos',
    cantidadMaxima:   10,
    comprimir:        true,
    guardarEnDrive:   false,
    driveFolderId:    '',
    tipoRespaldo:     'COMPLETO',
    programadoActivo: false,
    horaProgramada:   ''
  };

  historial: BackupHistorial[] = [];

  // Confirm eliminar
  idParaEliminar: number | null = null;

  // Resultado del último backup ejecutado
  ultimoResultado: EjecucionResult | null = null;

  constructor(
    private backupSvc: BackupService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarConfiguracion();
    this.cargarHistorial();
  }

  // ── Configuración ─────────────────────────────────────────────────────────

  cargarConfiguracion(): void {
    this.cargandoConfig = true;
    this.backupSvc.obtenerConfiguracion().subscribe({
      next: cfg => {
        this.config       = cfg;
        this.cargandoConfig = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.mensajeError  = 'Error al cargar configuración';
        this.cargandoConfig = false;
      }
    });
  }

  guardarConfiguracion(): void {
    this.limpiarMensajes();
    this.guardandoConfig = true;

    this.backupSvc.guardarConfiguracion(this.config).subscribe({
      next: cfg => {
        this.config          = cfg;
        this.guardandoConfig = false;
        this.mensajeExito    = 'Configuración guardada correctamente';
        this.limpiarMensajesDespues();
        this.cdr.detectChanges();
      },
      error: err => {
        this.guardandoConfig = false;
        this.mensajeError    = err?.error?.error ?? 'Error al guardar configuración';
      }
    });
  }

  // ── Historial ─────────────────────────────────────────────────────────────

  cargarHistorial(): void {
    this.cargandoHistorial = true;
    this.backupSvc.listarHistorial().subscribe({
      next: lista => {
        this.historial         = lista;
        this.cargandoHistorial = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoHistorial = false;
      }
    });
  }

  confirmarEliminar(id: number): void {
    this.idParaEliminar = id;
  }

  cancelarEliminar(): void {
    this.idParaEliminar = null;
  }

  eliminarRegistro(): void {
    if (this.idParaEliminar === null) return;
    const id = this.idParaEliminar;
    this.idParaEliminar = null;

    this.backupSvc.eliminarHistorial(id).subscribe({
      next: () => {
        this.historial   = this.historial.filter(h => h.id !== id);
        this.mensajeExito = 'Registro eliminado';
        this.limpiarMensajesDespues();
        this.cdr.detectChanges();
      },
      error: err => {
        this.mensajeError = err?.error?.error ?? 'Error al eliminar';
      }
    });
  }

  // ── Ejecución ─────────────────────────────────────────────────────────────

  ejecutarBackup(): void {
    this.limpiarMensajes();
    this.cargandoBackup  = true;
    this.ultimoResultado = null;

    this.backupSvc.ejecutarBackup().subscribe({
      next: res => {
        this.ultimoResultado = res;
        this.cargandoBackup  = false;
        if (res.exitoso) {
          this.mensajeExito = res.mensaje;
          this.cargarHistorial(); // refrescar lista
        } else {
          this.mensajeError = res.mensaje;
        }
        this.cdr.detectChanges();
      },
      error: err => {
        this.cargandoBackup  = false;
        this.mensajeError    = err?.error?.mensaje ?? err?.error?.error ?? 'Error al ejecutar respaldo';
      }
    });
  }

  descargar(id: number): void {
    this.backupSvc.descargarBackup(id);
  }

  // ── Utilidades ────────────────────────────────────────────────────────────

  cambiarTab(tab: 'config' | 'historial'): void {
    this.tabActiva = tab;
    this.limpiarMensajes();
    if (tab === 'historial') this.cargarHistorial();
  }

  formatTamanio(bytes: number): string {
    return this.backupSvc.formatearTamanio(bytes);
  }

  formatFecha(iso: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('es-EC', {
      year:   'numeric',
      month:  '2-digit',
      day:    '2-digit',
      hour:   '2-digit',
      minute: '2-digit'
    });
  }

  private limpiarMensajes(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
  }

  private limpiarMensajesDespues(ms = 4000): void {
    setTimeout(() => {
      this.mensajeExito = '';
      this.mensajeError = '';
      this.cdr.detectChanges();
    }, ms);
  }
}
