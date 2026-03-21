import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { DirectorApiService } from '../service';
import { Tutoria, TutoriaCreateRequest } from '../model';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-tutoriasdirector',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tutoriasdirector.html',
  styleUrl: './tutoriasdirector.scss',
})
export class Tutoriasdirector {
  cargando = false;
  mensaje = '';

  idDocente!: number;
  idAnteproyecto!: number;
  mesActual = new Date();
  diasCalendario: { fecha: Date; tieneTutoria: boolean; estado?: string }[] = [];

  form: FormGroup;
  tutorias: Tutoria[] = [];

  constructor(
    private api: DirectorApiService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      fecha: ['', Validators.required],
      hora: [''],
      modalidad: ['PRESENCIAL', Validators.required],
    });
  }

  ngOnInit() {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }
    this.idDocente = idUsuario;

    const raw = localStorage.getItem('director_idAnteproyecto');
    this.idAnteproyecto = Number(raw || 0);
    if (!this.idAnteproyecto) { this.mensaje = 'No hay anteproyecto seleccionado'; return; }

    this.cargar();
  }

  cargar() {
    this.cargando = true;
    this.mensaje = '';
    this.api.tutorias(this.idAnteproyecto, this.idDocente)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (r) =>
        {
          this.tutorias = r;
          this.generarCalendario(); // ← agrega esto
        },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  crear() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const req: TutoriaCreateRequest = this.form.getRawValue();

    this.cargando = true;
    this.api.programarTutoria(this.idAnteproyecto, this.idDocente, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (tutoria) => {
          this.form.reset({ modalidad: 'PRESENCIAL' });
          this.cargar();
          if (req.modalidad === 'VIRTUAL' && !tutoria.linkReunion) {
            this.mensaje = '⚠️ Tutoría creada, pero configure sus credenciales de Zoom para generar el link de reunión.';
          } else {
            this.mensaje = 'Tutoría programada correctamente.';
          }
        },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  cancelar(t: Tutoria) {
    this.cargando = true;
    this.api.cancelarTutoria(t.idTutoria, this.idDocente)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => { this.mensaje = 'Tutoría cancelada'; this.cargar(); },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  abrirActa(t: Tutoria) {
    localStorage.setItem('director_idTutoria', String(t.idTutoria));
    this.router.navigate(['/app/director/acta']);
  }
  irZoomConfig(): void {
    this.router.navigate(['/app/docente/zoom-config']);
  }

  volver() {
    this.router.navigate(['/app/director/mis-anteproyectos']);
  }

  private err(e:any){
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
  get nombreMes(): string {
    return this.mesActual.toLocaleDateString('es-EC', { month: 'long', year: 'numeric' });
  }

  mesAnterior(): void {
    this.mesActual = new Date(this.mesActual.getFullYear(), this.mesActual.getMonth() - 1, 1);
    this.generarCalendario();
    this.cdr.detectChanges();
  }

  mesSiguiente(): void {
    this.mesActual = new Date(this.mesActual.getFullYear(), this.mesActual.getMonth() + 1, 1);
    this.generarCalendario();
    this.cdr.detectChanges();
  }

  generarCalendario(): void {
    const year = this.mesActual.getFullYear();
    const month = this.mesActual.getMonth();
    const primerDia = new Date(year, month, 1).getDay();
    const diasEnMes = new Date(year, month + 1, 0).getDate();

    this.diasCalendario = [];

    // días vacíos antes del primer día
    for (let i = 0; i < primerDia; i++) {
      this.diasCalendario.push({ fecha: new Date(0), tieneTutoria: false });
    }

    for (let d = 1; d <= diasEnMes; d++) {
      const fecha = new Date(year, month, d);
      const fechaStr = fecha.toISOString().split('T')[0];
      const tutoria = this.tutorias.find(t => t.fecha === fechaStr);
      this.diasCalendario.push({
        fecha,
        tieneTutoria: !!tutoria,
        estado: tutoria?.estado
      });
    }
  }
}







