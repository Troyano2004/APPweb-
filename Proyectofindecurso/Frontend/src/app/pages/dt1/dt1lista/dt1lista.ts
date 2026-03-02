import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { Dt1ApiService } from '../service';
import { Dt1Enviado } from '../model';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-dt1-enviados',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dt1lista.html',     // ✅ TU ARCHIVO REAL
  styleUrls: ['./dt1lista.scss']      // ✅ TU ARCHIVO REAL
})
export class Dt1EnviadosComponent implements OnInit {

  cargando = false;
  mensaje = '';
  items: Dt1Enviado[] = [];
  idDocente = 0;

  constructor(
    private api: Dt1ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);

    if (!idUsuario) {
      this.mensaje = 'No hay idUsuario en sesión';
      return;
    }

    this.idDocente = idUsuario;
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.mensaje = '';
    this.cdr.detectChanges();

    this.api.lista(this.idDocente)
      .pipe(finalize(() => {
        this.cargando = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (r) => {
          this.items = r || [];
          if (!this.items.length) this.mensaje = 'No hay anteproyectos enviados por revisar.';
        },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  abrir(it: Dt1Enviado): void {
    localStorage.setItem('dt1_idAnteproyecto', String(it.idAnteproyecto));
    this.router.navigate(['/app/dt1/revision']);
  }

  private err(e: any): string {
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}
