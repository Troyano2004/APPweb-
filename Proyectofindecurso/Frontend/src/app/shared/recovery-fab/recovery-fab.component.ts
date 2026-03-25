import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecoveryModeService } from '../../services/recovery-mode.service';

@Component({
  selector: 'app-recovery-fab',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (svc.bdCaida()) {
      <div class="recovery-overlay">

        <!-- Badge pulsante arriba -->
        <div class="recovery-badge">
          <span class="dot"></span>
          BASE DE DATOS CAÍDA
        </div>

        <!-- Botón flotante -->
        <button class="recovery-fab" (click)="svc.abrirRecovery()" title="Abrir Recovery Mode">
          <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22"
               viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>Recovery Mode</span>
        </button>

      </div>
    }
  `,
  styles: [`
    /* ── Badge pulsante ───────────────────────────────── */
    .recovery-badge {
      position: fixed;
      top: 16px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 9999;
      background: #dc2626;
      color: #fff;
      font-size: 0.7rem;
      font-weight: 800;
      letter-spacing: 0.1em;
      padding: 6px 16px;
      border-radius: 20px;
      display: flex;
      align-items: center;
      gap: 8px;
      animation: fadeInDown 0.4s ease;
      box-shadow: 0 4px 15px rgba(220, 38, 38, 0.4);
    }

    .dot {
      width: 8px;
      height: 8px;
      background: #fff;
      border-radius: 50%;
      animation: pulse 1.5s infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50%       { opacity: 0.5; transform: scale(1.3); }
    }

    @keyframes fadeInDown {
      from { opacity: 0; transform: translateX(-50%) translateY(-10px); }
      to   { opacity: 1; transform: translateX(-50%) translateY(0); }
    }

    /* ── Botón flotante ───────────────────────────────── */
    .recovery-fab {
      position: fixed;
      bottom: 28px;
      right: 28px;
      z-index: 9999;
      background: linear-gradient(135deg, #dc2626, #b91c1c);
      color: #fff;
      border: none;
      border-radius: 50px;
      padding: 14px 22px;
      font-size: 0.88rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 10px;
      box-shadow: 0 6px 24px rgba(220, 38, 38, 0.45);
      transition: all 0.2s ease;
      animation: fadeInUp 0.4s ease;
    }

    .recovery-fab:hover {
      background: linear-gradient(135deg, #b91c1c, #991b1b);
      transform: translateY(-2px);
      box-shadow: 0 10px 30px rgba(220, 38, 38, 0.55);
    }

    .recovery-fab:active {
      transform: translateY(0);
    }

    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(20px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class RecoveryFabComponent {
  svc = inject(RecoveryModeService);
}
