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
        next: (r) => this.tutorias = r,
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
        next: () => { this.mensaje = 'Tutoría programada'; this.form.reset({ modalidad:'PRESENCIAL' }); this.cargar(); },
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

  volver() {
    this.router.navigate(['/app/director/mis-anteproyectos']);
  }

  private err(e:any){
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}







