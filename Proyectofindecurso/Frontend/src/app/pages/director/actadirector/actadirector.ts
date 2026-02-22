import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { DirectorApiService } from '../service';
import { ActaRevisionDirectorRequest } from '../model';
import { getSessionUser } from '../../../services/session';
import { Router } from '@angular/router';

@Component({
  selector: 'app-actadirector',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './actadirector.html',
  styleUrl: './actadirector.scss',
})
export class Actadirector {
  cargando = false;
  mensaje = '';

  idDocente!: number;
  idTutoria!: number;

  form: FormGroup;

  constructor(
    private api: DirectorApiService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {
    this.form = this.fb.group({
      directorCargo: ['Director del Proyecto de Investigación', [Validators.required, Validators.maxLength(200)]],
      directorFirma: ['', [Validators.maxLength(200)]],

      estudianteCargo: ['Autor del Proyecto de Investigación', [Validators.required, Validators.maxLength(200)]],
      estudianteFirma: ['', [Validators.maxLength(200)]],

      tituloProyecto: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(600)]],
      objetivo: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]],
      detalleRevision: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
      observaciones: ['', [Validators.maxLength(8000)]],
      cumplimiento: ['COMPLETO', [Validators.required]],
      conclusion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
    });
  }

  ngOnInit() {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }
    this.idDocente = idUsuario;

    const raw = localStorage.getItem('director_idTutoria');
    this.idTutoria = Number(raw || 0);
    if (!this.idTutoria) { this.mensaje = 'No hay tutoría seleccionada'; return; }

    this.cargarSiExiste();
  }

  cargarSiExiste() {
    this.cargando = true;
    this.mensaje = '';
    this.api.obtenerActa(this.idTutoria, this.idDocente)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (a) => {
          this.form.patchValue(a, { emitEvent:false });
          this.mensaje = 'Acta cargada (puedes editar y guardar).';
        },
        error: () => {
          this.mensaje = 'Aún no existe acta. Llena y guarda.';
        }
      });
  }

  guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mensaje = 'Completa los campos obligatorios.';
      return;
    }

    const req: ActaRevisionDirectorRequest = this.form.getRawValue();

    this.cargando = true;
    this.api.guardarActa(this.idTutoria, this.idDocente, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => this.mensaje = 'Acta guardada. Tutoría marcada como REALIZADA.',
        error: (e) => this.mensaje = this.err(e),
      });
  }

  imprimir() { window.print(); }

  volver() { this.router.navigate(['/app/director/tutorias']); }

  private err(e:any){
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}
