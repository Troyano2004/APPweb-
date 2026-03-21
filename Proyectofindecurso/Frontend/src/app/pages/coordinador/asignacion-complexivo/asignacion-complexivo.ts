
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  InfoCoordinadorComplexivoDto,
  EstudianteComplexivoSinDocenteDto,
  ComplexivoDocenteAsignacionResponse,
  DocenteOpcionDto
} from '../../../services/complexivo.service';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-asignacion-complexivo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './asignacion-complexivo.html',
  styleUrls: ['./asignacion-complexivo.scss']
})
export class AsignacionComplexivoComponent implements OnInit {

  loading  = signal(false);
  error    = signal<string | null>(null);
  ok       = signal<string | null>(null);

  info     = signal<InfoCoordinadorComplexivoDto | null>(null);

  // Estudiante seleccionado en el panel izquierdo
  estudianteSeleccionado = signal<EstudianteComplexivoSinDocenteDto | null>(null);

  // Asignación ya existente del estudiante seleccionado
  asignacionExistente = signal<ComplexivoDocenteAsignacionResponse | null>(null);

  formAsignar: FormGroup;

  private idUsuario = 0;

  constructor(
    private api: ComplexivoService,
    private fb: FormBuilder
  ) {
    this.formAsignar = this.fb.group({
      idDocente:   [null, [Validators.required, Validators.min(1)]],
      observacion: ['']
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idUsuario = Number(user?.['idUsuario'] ?? user?.['id_usuario'] ?? 0);
    this.cargarInfo();
  }

  // ── Panel izquierdo: seleccionar estudiante ──────────────────
  seleccionarEstudiante(est: EstudianteComplexivoSinDocenteDto): void {
    this.estudianteSeleccionado.set(est);
    this.asignacionExistente.set(null);
    this.error.set(null);
    this.ok.set(null);
    this.formAsignar.reset({ idDocente: null, observacion: '' });
  }

  seleccionarAsignado(asig: ComplexivoDocenteAsignacionResponse): void {
    // Permite ver el detalle de una asignación ya hecha
    this.estudianteSeleccionado.set(null);
    this.asignacionExistente.set(asig);
    this.error.set(null);
    this.ok.set(null);
  }

  // ── Asignar docente ──────────────────────────────────────────
  asignar(): void {
    const est = this.estudianteSeleccionado();
    if (!est || this.formAsignar.invalid) {
      this.formAsignar.markAllAsTouched();
      return;
    }
    const v = this.formAsignar.value;
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.api.asignarDocente({
      idEstudiante:         est.idEstudiante,
      idDocente:            Number(v.idDocente),
      idUsuarioCoordinador: this.idUsuario,
      observacion:          v.observacion ?? ''
    }).pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Docente asignado correctamente.');
          this.estudianteSeleccionado.set(null);
          this.formAsignar.reset();
          this.cargarInfo();
        },
        error: (e) => {
          this.error.set(e?.error?.message ?? 'Error al asignar el docente.');
        }
      });
  }

  // ── Getters de ayuda ────────────────────────────────────────
  nombreDocente(idDocente: number): string {
    return this.info()?.docentesDisponibles.find(d => d.idDocente === idDocente)?.nombre
      ?? `Docente #${idDocente}`;
  }

  get sinDocente(): EstudianteComplexivoSinDocenteDto[] {
    return this.info()?.estudiantesSinDocente ?? [];
  }

  get conDocente(): ComplexivoDocenteAsignacionResponse[] {
    return this.info()?.asignacionesActuales ?? [];
  }

  get docentesDisponibles(): DocenteOpcionDto[] {
    return this.info()?.docentesDisponibles ?? [];
  }

  // ── Privados ────────────────────────────────────────────────
  private cargarInfo(): void {
    this.loading.set(true);
    this.api.getInfoCoordinador(this.idUsuario)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next:  (data) => this.info.set(data),
        error: (e)    => this.error.set(e?.error?.message ?? 'Error al cargar información.')
      });
  }
}
