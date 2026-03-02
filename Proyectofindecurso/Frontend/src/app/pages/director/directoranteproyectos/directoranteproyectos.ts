
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DirectorApiService } from '../service';
import { AnteDirectorItem } from '../model';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-director-mis-anteproyectos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './directoranteproyectos.html',
  styleUrls: ['./directoranteproyectos.scss'],
})
export class DirectorMisAnteproyectosComponent implements OnInit{

  cargando = false;
  mensaje = '';
  items: AnteDirectorItem[] = [];

  idDocente!: number;

  constructor(private api: DirectorApiService, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }

    // ✅ en tu sistema Docente.pk = Usuario.pk
    this.idDocente = idUsuario;

    this.cargar();
  }
  cargar() {
    this.cargando = true;
    this.mensaje = '';
    this.cdr.detectChanges(); // 🔥 pinta el "Cargando..."

    this.api.misAnteproyectos(this.idDocente).pipe(
      finalize(() => {
        this.cargando = false;
        this.cdr.detectChanges(); // 🔥 fuerza render al terminar
      })
    ).subscribe({
      next: (r) => {
        this.items = r;
        console.log('Respuesta:', r);
        this.cdr.detectChanges(); // 🔥 fuerza render de la lista
      },
      error: () => {
        this.mensaje = 'No se pudo cargar.';
        this.cdr.detectChanges();
      }
    });
  }
  abrir(item: AnteDirectorItem) {
    localStorage.setItem('director_idAnteproyecto', String(item.idAnteproyecto));
    this.router.navigate(['/app/director/tutorias']); }
}
