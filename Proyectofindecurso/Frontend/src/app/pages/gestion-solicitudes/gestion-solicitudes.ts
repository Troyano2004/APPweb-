import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionSolicitudesService } from './service';
import { SolicitudPendiente } from './model';

@Component({
  selector: 'app-gestion-solicitudes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './gestion-solicitudes.html',
  styleUrls: ['./gestion-solicitudes.scss']
})
export class GestionSolicitudesComponent implements OnInit {

  solicitudes: SolicitudPendiente[] = [];
  cargando = false;
  error = '';
  exito = '';

  solicitudSeleccionada: SolicitudPendiente | null = null;
  mostrarDetalle = false;

  mostrarModalRechazo = false;
  solicitudARechazar: SolicitudPendiente | null = null;

  formRechazo: FormGroup;

  constructor(
    private svc: GestionSolicitudesService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.formRechazo = this.fb.group({
      motivo: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit() {
    this.cargarSolicitudes();
  }

  cargarSolicitudes() {
    this.cargando = true;
    this.error = '';
    this.svc.listarPendientes().subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Error al cargar las solicitudes.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  verDetalle(s: SolicitudPendiente) {
    this.solicitudSeleccionada = s;
    this.mostrarDetalle = true;
    this.cdr.detectChanges();
  }

  cerrarDetalle() {
    this.solicitudSeleccionada = null;
    this.mostrarDetalle = false;
    this.cdr.detectChanges();
  }

  aprobar(s: SolicitudPendiente) {
    this.exito = '';
    this.error = '';
    this.svc.aprobar(s.idSolicitud).subscribe({
      next: () => {
        this.exito = `Solicitud de ${s.nombres} ${s.apellidos} aprobada. Credenciales enviadas a ${s.correo}.`;
        this.cerrarDetalle();
        this.cargarSolicitudes();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al aprobar la solicitud.';
        this.cdr.detectChanges();
      }
    });
  }

  abrirRechazo(s: SolicitudPendiente) {
    this.solicitudARechazar = s;
    this.formRechazo.reset();
    this.mostrarModalRechazo = true;
    this.cerrarDetalle();
    this.cdr.detectChanges();
  }

  cerrarRechazo() {
    this.solicitudARechazar = null;
    this.mostrarModalRechazo = false;
    this.formRechazo.reset();
    this.cdr.detectChanges();
  }

  confirmarRechazo() {
    if (this.formRechazo.invalid) {
      this.formRechazo.markAllAsTouched();
      return;
    }
    const motivo = this.formRechazo.value.motivo;
    this.svc.rechazar(this.solicitudARechazar!.idSolicitud, motivo).subscribe({
      next: () => {
        this.exito = `Solicitud de ${this.solicitudARechazar?.nombres} rechazada.`;
        this.cerrarRechazo();
        this.cargarSolicitudes();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al rechazar la solicitud.';
        this.cerrarRechazo();
        this.cdr.detectChanges();
      }
    });
  }

  invalido(campo: string): boolean {
    const ctrl = this.formRechazo.get(campo);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }
}
