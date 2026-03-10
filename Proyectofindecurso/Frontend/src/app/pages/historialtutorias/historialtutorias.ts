import {ChangeDetectorRef, Component} from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';
import { getSessionUser } from '../../services/session';
import { TutoriaHistorialService } from './service';
import { TutoriaHistorialResponse } from './model';

@Component({
  selector: 'app-historialtutorias',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './historialtutorias.html',
  styleUrl: './historialtutorias.scss',
})
export class Historialtutorias {
  cargando = false;
  mensaje = '';
  items: TutoriaHistorialResponse[] = [];

  idEstudiante!: number;
  idAnteproyecto!: number;

  openId: number | null = null;

  constructor(private api: TutoriaHistorialService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }
    this.idEstudiante = idUsuario;

    const raw = localStorage.getItem('est_idAnteproyecto');
    this.idAnteproyecto = Number(raw || 0);

    if (!this.idAnteproyecto) {
      this.mensaje = 'Falta est_idAnteproyecto en localStorage.';
      return;
    }

    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.mensaje = '';
    this.cdr.detectChanges();

    this.api.historial(this.idEstudiante, this.idAnteproyecto)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (r) => {
          this.items = r || [];
          if (!this.items.length) this.mensaje = 'No hay tutorías registradas.';
        },
        error: () => this.mensaje = 'No se pudo cargar el historial.'
      });
  }

  toggle(t: TutoriaHistorialResponse) {
    this.openId = (this.openId === t.idTutoria) ? null : t.idTutoria;
  }

  get pendientes(): TutoriaHistorialResponse[] {
    return this.items.filter(t => t.estado === 'PROGRAMADA');
  }
}
