import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  InfoCoordinadorDt2Dto,
  EstudianteComplexivoSinDocenteDto,
  ComplexivoDocenteAsignacionResponse,
  DocenteOpcionDto
} from '../../../services/complexivo.service';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-asignacion-complexivo-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './asignacion-complexivo-dt2.html',
  styleUrls: ['./asignacion-complexivo-dt2.scss']
})
export class AsignacionComplexivoDt2Component implements OnInit {

  loading  = signal(false);
  error    = signal<string | null>(null);
  ok       = signal<string | null>(null);
  info     = signal<InfoCoordinadorDt2Dto | null>(null);
  estudianteSeleccionado = signal<EstudianteComplexivoSinDocenteDto | null>(null);
  asignacionExistente    = signal<ComplexivoDocenteAsignacionResponse | null>(null);
  formAsignar: FormGroup;
  private idUsuario = 0;

  constructor(private api: ComplexivoService, private fb: FormBuilder) {
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

  seleccionarEstudiante(est: EstudianteComplexivoSinDocenteDto): void {
    this.estudianteSeleccionado.set(est);
    this.asignacionExistente.set(null);
    this.error.set(null); this.ok.set(null);
    this.formAsignar.reset({ idDocente: null, observacion: '' });
  }

  seleccionarAsignado(asig: ComplexivoDocenteAsignacionResponse): void {
    this.estudianteSeleccionado.set(null);
    this.asignacionExistente.set(asig);
    this.error.set(null); this.ok.set(null);
  }

  asignar(): void {
    const est = this.estudianteSeleccionado();
    if (!est || this.formAsignar.invalid) {
      this.formAsignar.markAllAsTouched(); return;
    }
    const v = this.formAsignar.value;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.asignarDt2({
      idEstudiante: est.idEstudiante,
      idDocente: Number(v.idDocente),
      idUsuarioCoordinador: this.idUsuario,
      observacion: v.observacion ?? ''
    }).pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Docente DT2 asignado correctamente.');
          this.estudianteSeleccionado.set(null);
          this.formAsignar.reset();
          this.cargarInfo();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al asignar.')
      });
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

  private cargarInfo(): void {
    this.loading.set(true);
    this.api.getInfoCoordinadorDt2(this.idUsuario)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => this.info.set(data),
        error: (e)   => this.error.set(e?.error?.message ?? 'Error al cargar.')
      });
  }
}
