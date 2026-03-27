import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditoriaService } from '../service';

@Component({
  selector: 'app-sesiones-activas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sesiones-activas.html',
  styleUrl: './sesiones-activas.scss'
})
export class SesionesActivasComponent implements OnInit, OnDestroy {
  sesiones: any[] = [];
  cargando = false;
  usuarioActual = '';
  mensajeExito = '';
  mensajeError = '';
  mostrarConfirm = false;
  sesionSeleccionada: any = null;
  private intervalo: any;

  constructor(private svc: AuditoriaService) {}

  ngOnInit(): void {
    const raw = localStorage.getItem('usuario') || sessionStorage.getItem('usuario') || '{}';
    try { this.usuarioActual = JSON.parse(raw)?.username || ''; } catch { this.usuarioActual = ''; }
    this.cargar();
    this.intervalo = setInterval(() => this.cargar(), 30000);
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalo);
  }

  cargar(): void {
    this.cargando = true;
    this.svc.getSesionesActivas().subscribe({
      next: (data) => { this.sesiones = data; this.cargando = false; },
      error: ()    => { this.cargando = false; }
    });
  }

  cerrarSesion(sesion: any): void {
    this.sesionSeleccionada = sesion;
    this.mostrarConfirm = true;
  }

  cancelarCerrar(): void {
    this.mostrarConfirm = false;
    this.sesionSeleccionada = null;
  }

  confirmarCerrar(): void {
    this.mostrarConfirm = false;
    const sesion = this.sesionSeleccionada;
    this.svc.cerrarSesion(sesion.sessionId).subscribe({
      next: () => {
        this.mensajeExito = 'Sesión de ' + sesion.username + ' cerrada correctamente';
        this.sesionSeleccionada = null;
        setTimeout(() => this.mensajeExito = '', 4000);
        this.cargar();
      },
      error: (err) => {
        this.mensajeError = err.error?.error ?? 'Error al cerrar sesión';
        this.sesionSeleccionada = null;
        setTimeout(() => this.mensajeError = '', 4000);
      }
    });
  }

  esVpn(ip: string): boolean {
    if (!ip) return false;
    return ip.startsWith('26.') || ip.startsWith('192.168.');
  }

  formatearTiempo(minutos: number): string {
    if (minutos < 1) return 'Recién conectado';
    if (minutos < 60) return minutos + ' min';
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    return h + 'h ' + m + 'min';
  }
}
