import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { Dt1ApiService } from '../service';
import { Dt1Detalle, Dt1RevisionRequest } from '../model';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-dt1revision',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dt1revision.html',
  styleUrls: ['./dt1revision.scss']
})
export class Dt1RevisionComponent implements OnInit {

  cargando = false;
  mensaje = '';

  idDocente = 0;
  idAnteproyecto = 0;

  detalle: Dt1Detalle | null = null;

  decision: 'APROBADO' | 'RECHAZADO' = 'APROBADO';
  observacion = '';

  constructor(
    private api: Dt1ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = Number(user?.['idUsuario'] || 0);

    // Si tienes ruta con :id úsalo, si no, usa localStorage
    const idUrl = Number(this.route.snapshot.paramMap.get('id') || 0);
    const idLocal = Number(localStorage.getItem('dt1_idAnteproyecto') || 0);
    this.idAnteproyecto = idUrl || idLocal;

    if (!this.idDocente || !this.idAnteproyecto) {
      this.mensaje = 'Faltan datos. Vuelve a la lista y selecciona un anteproyecto.';
      this.cdr.detectChanges();
      return;
    }

    this.cargarDetalle();
  }

  cargarDetalle(): void {
    this.cargando = true;
    this.mensaje = '';
    this.detalle = null;

    // ✅ fuerza que se pinte el loader
    this.cdr.detectChanges();

    this.api.detalle(this.idAnteproyecto, this.idDocente).subscribe({
      next: (data) => {
        console.log('DATOS RECIBIDOS:', data);

        this.detalle = data;
        this.cargando = false;

        // ✅ fuerza que se pinten los datos
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.mensaje = this.mostrarError(e);
        this.cargando = false;

        // ✅ fuerza que se pinte el error y se quite el loader
        this.cdr.detectChanges();
      }
    });
  }

  verPdf(): void {
    this.api.pdf(this.idAnteproyecto, this.idDocente).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (e) => {
        this.mensaje = this.mostrarError(e);
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    if (this.decision === 'RECHAZADO' && !this.observacion.trim()) {
      this.mensaje = 'Si rechazas, escribe una observación.';
      this.cdr.detectChanges();
      return;
    }

    const data: Dt1RevisionRequest = {
      idAnteproyecto: this.idAnteproyecto,
      idDocente: this.idDocente,
      decision: this.decision,
      observacion: this.observacion.trim()
    };

    this.cargando = true;
    this.mensaje = '';
    this.cdr.detectChanges();

    this.api.revisar(data).subscribe({
      next: () => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.router.navigate(['/app/dt1/lista']);
      },
      error: (e) => {
        this.mensaje = this.mostrarError(e);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void {
    this.router.navigate(['/app/dt1/lista']);
  }

  private mostrarError(e: any): string {
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}
