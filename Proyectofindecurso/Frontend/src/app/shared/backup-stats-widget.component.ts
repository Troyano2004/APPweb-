import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BackupService } from '../services/backup.service';

@Component({
  selector: 'app-backup-stats-widget',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <article class="card backup-widget">
      <div class="widget-header">
        <span class="widget-icon">💾</span>
        <h3>Respaldos de Base de Datos</h3>
        <a routerLink="/app/admin/backup" class="widget-link">Ver todos →</a>
      </div>

      <div class="widget-loading" *ngIf="cargando">
        <span class="spinner-sm"></span> Cargando...
      </div>

      <ng-container *ngIf="!cargando && stats">

        <!-- Métricas principales -->
        <div class="widget-metricas">

          <div class="metrica">
            <span class="metrica-valor" [class.ok]="stats.tasaExitoMes >= 90"
                                        [class.warn]="stats.tasaExitoMes >= 70 && stats.tasaExitoMes < 90"
                                        [class.error]="stats.tasaExitoMes < 70">
              {{ stats.tasaExitoMes }}%
            </span>
            <span class="metrica-label">Tasa de éxito (30d)</span>
          </div>

          <div class="metrica">
            <span class="metrica-valor">{{ stats.exitososMes }}</span>
            <span class="metrica-label">Exitosos</span>
          </div>

          <div class="metrica">
            <span class="metrica-valor error-val" *ngIf="stats.fallidosMes > 0">
              {{ stats.fallidosMes }}
            </span>
            <span class="metrica-valor ok" *ngIf="stats.fallidosMes === 0">0</span>
            <span class="metrica-label">Fallidos</span>
          </div>

          <div class="metrica">
            <span class="metrica-valor">{{ formatBytes(stats.tamanoAcumuladoBytes) }}</span>
            <span class="metrica-label">Acumulado (30d)</span>
          </div>

        </div>

        <!-- Último backup -->
        <div class="widget-ultimo" *ngIf="stats.ultimoBackupFecha">
          <div class="ultimo-row">
            <span class="ultimo-label">Último backup</span>
            <span class="badge"
                  [class.badge-ok]="stats.ultimoBackupEstado === 'EXITOSO'"
                  [class.badge-error]="stats.ultimoBackupEstado === 'FALLIDO'">
              {{ stats.ultimoBackupEstado }}
            </span>
          </div>
          <div class="ultimo-detalle">
            <span>📋 {{ stats.ultimoBackupJob }}</span>
            <span>🕐 {{ formatFecha(stats.ultimoBackupFecha) }}</span>
          </div>
        </div>

        <!-- Próxima ejecución -->
        <div class="widget-proxima" *ngIf="stats.proximaEjecucion">
          <span class="proxima-label">⏰ Próxima ejecución</span>
          <span class="proxima-valor">
            {{ stats.proximaEjecucionJob }} · {{ formatFecha(stats.proximaEjecucion) }}
          </span>
        </div>

        <!-- Jobs activos -->
        <div class="widget-footer">
          <span>🟢 {{ stats.jobsActivos }} jobs activos de {{ stats.jobsTotal }} total</span>
          <a routerLink="/app/admin/backup/restaurar" class="widget-link-sm">
            🔄 Restaurar
          </a>
        </div>

      </ng-container>

      <div class="widget-empty" *ngIf="!cargando && !stats">
        No hay datos de backups disponibles
      </div>
    </article>
  `,
  styles: [`
    .backup-widget {
      background: #fff;
      border: 1px solid #e5e7eb;
      border-radius: 0.75rem;
      padding: 1.1rem 1.2rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .widget-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      .widget-icon { font-size: 1.2rem; }
      h3 { margin: 0; font-size: 0.95rem; color: #1f2937; flex: 1; }
      .widget-link {
        font-size: 0.78rem;
        color: #1a7a4a;
        text-decoration: none;
        font-weight: 600;
        white-space: nowrap;
        &:hover { text-decoration: underline; }
      }
    }

    .widget-metricas {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.5rem;
    }

    .metrica {
      background: #f9fafb;
      border: 1px solid #f3f4f6;
      border-radius: 8px;
      padding: 0.5rem 0.6rem;
      text-align: center;
    }

    .metrica-valor {
      display: block;
      font-size: 1.25rem;
      font-weight: 800;
      color: #1a7a4a;
      line-height: 1.2;

      &.ok    { color: #1a7a4a; }
      &.warn  { color: #d97706; }
      &.error { color: #dc2626; }
    }
    .error-val { color: #dc2626 !important; }

    .metrica-label {
      display: block;
      font-size: 0.65rem;
      color: #6b7280;
      margin-top: 0.15rem;
      line-height: 1.2;
    }

    .widget-ultimo {
      background: #f9fafb;
      border-radius: 8px;
      padding: 0.55rem 0.75rem;

      .ultimo-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.25rem;
      }
      .ultimo-label { font-size: 0.75rem; font-weight: 600; color: #374151; }
      .ultimo-detalle {
        display: flex;
        gap: 0.75rem;
        font-size: 0.75rem;
        color: #6b7280;
        flex-wrap: wrap;
      }
    }

    .widget-proxima {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      .proxima-label { font-size: 0.72rem; color: #6b7280; }
      .proxima-valor { font-size: 0.8rem; font-weight: 600; color: #374151; }
    }

    .widget-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 0.5rem;
      border-top: 1px solid #f3f4f6;
      font-size: 0.75rem;
      color: #6b7280;

      .widget-link-sm {
        font-size: 0.75rem;
        color: #1a7a4a;
        text-decoration: none;
        font-weight: 600;
        &:hover { text-decoration: underline; }
      }
    }

    .badge {
      display: inline-block;
      padding: 0.1rem 0.45rem;
      border-radius: 20px;
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;

      &.badge-ok    { background: #dcfce7; color: #1a7a4a; }
      &.badge-error { background: #fee2e2; color: #dc2626; }
    }

    .widget-loading {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.82rem;
      color: #6b7280;
      padding: 0.5rem 0;
    }

    .spinner-sm {
      width: 14px; height: 14px;
      border: 2px solid #e5e7eb;
      border-top-color: #1a7a4a;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .widget-empty {
      font-size: 0.82rem;
      color: #9ca3af;
      text-align: center;
      padding: 0.5rem 0;
    }
  `]
})
export class BackupStatsWidgetComponent implements OnInit {
  stats: any = null;
  cargando  = true;

  constructor(private svc: BackupService) {}

  ngOnInit(): void {
    this.svc.estadisticas().subscribe({
      next:  s  => { this.stats = s; this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

  formatBytes(bytes: number): string {
    if (!bytes) return '0 B';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1048576)    return `${(bytes/1024).toFixed(1)} KB`;
    if (bytes < 1073741824) return `${(bytes/1048576).toFixed(1)} MB`;
    return `${(bytes/1073741824).toFixed(2)} GB`;
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '-';
    const d = new Date(fecha);
    return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  }
}
