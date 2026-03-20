import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { BackupToastService, BackupToast } from './backup-toast.service';

@Component({
  selector: 'app-backup-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div class="toast"
           *ngFor="let t of toasts; trackBy: trackById"
           [class.exitoso]="t.tipo === 'EXITOSO'"
           [class.fallido]="t.tipo === 'FALLIDO'"
           (click)="toastSvc.eliminarToast(t.id)">

        <div class="toast-icon">
          {{ t.tipo === 'EXITOSO' ? '✅' : '❌' }}
        </div>

        <div class="toast-body">
          <div class="toast-titulo">
            {{ t.tipo === 'EXITOSO' ? 'Backup completado' : 'Backup fallido' }}
            <span class="toast-manual" *ngIf="t.manual">· Manual</span>
          </div>
          <div class="toast-detalle">
            💾 {{ t.jobNombre }} · 🗄️ {{ t.database }}
          </div>
          <div class="toast-meta" *ngIf="t.tipo === 'EXITOSO'">
            <span *ngIf="t.duracion">⏱️ {{ t.duracion }}s</span>
            <span *ngIf="t.tamano">📦 {{ toastSvc.formatBytes(t.tamano) }}</span>
          </div>
        </div>

        <button class="toast-close" (click)="toastSvc.eliminarToast(t.id); $event.stopPropagation()">
          ✕
        </button>

        <div class="toast-progress"></div>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      bottom: 1.5rem;
      right: 1.5rem;
      z-index: 9999;
      display: flex;
      flex-direction: column-reverse;
      gap: 0.6rem;
      max-width: 360px;
    }

    .toast {
      position: relative;
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      background: #fff;
      border: 1.5px solid #e5e7eb;
      border-radius: 12px;
      padding: 0.85rem 2.5rem 0.85rem 0.9rem;
      box-shadow: 0 8px 24px rgba(0,0,0,0.12);
      cursor: pointer;
      animation: slideIn 0.3s ease;
      overflow: hidden;

      &.exitoso { border-color: #86efac; background: #f0fdf4; }
      &.fallido  { border-color: #fca5a5; background: #fff5f5; }
    }

    @keyframes slideIn {
      from { transform: translateX(110%); opacity: 0; }
      to   { transform: translateX(0);   opacity: 1; }
    }

    .toast-icon { font-size: 1.3rem; flex-shrink: 0; margin-top: 0.1rem; }

    .toast-body { flex: 1; min-width: 0; }

    .toast-titulo {
      font-weight: 700;
      font-size: 0.88rem;
      color: #1f2937;
      margin-bottom: 0.2rem;
    }

    .toast-manual {
      font-size: 0.72rem;
      color: #6b7280;
      font-weight: 400;
    }

    .toast-detalle {
      font-size: 0.8rem;
      color: #374151;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .toast-meta {
      display: flex;
      gap: 0.75rem;
      margin-top: 0.25rem;
      font-size: 0.75rem;
      color: #6b7280;
    }

    .toast-close {
      position: absolute;
      top: 0.5rem;
      right: 0.6rem;
      background: none;
      border: none;
      font-size: 0.75rem;
      cursor: pointer;
      color: #9ca3af;
      padding: 0.15rem;
      &:hover { color: #374151; }
    }

    .toast-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      background: #1a7a4a;
      border-radius: 0 0 0 12px;
      animation: progress 6s linear forwards;

      .fallido & { background: #dc2626; }
    }

    @keyframes progress {
      from { width: 100%; }
      to   { width: 0%; }
    }
  `]
})
export class BackupToastComponent implements OnInit, OnDestroy {
  toasts: BackupToast[] = [];
  private sub?: Subscription;

  constructor(public toastSvc: BackupToastService) {}

  ngOnInit(): void {
    this.sub = this.toastSvc.toasts$.subscribe(t => this.toasts = t);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  trackById(_: number, t: BackupToast): string { return t.id; }
}
