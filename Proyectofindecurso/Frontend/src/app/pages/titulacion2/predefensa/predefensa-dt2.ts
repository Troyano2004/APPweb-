import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  PredefensaDto,
  ProgramarPredefensaRequest
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId, normalizeRole } from '../../../services/session';
@Component({
  selector: 'app-predefensa-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './predefensa-dt2.html',
  styleUrl: './predefensa-dt2.scss'
})
export class PredefensaDt2Component implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  proyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  predefensa = signal<PredefensaDto | null>(null);
  tab = signal<'estado' | 'programar' | 'calificar'>('estado');
  esCoordinador = false;
  esDocenteDt2 = false;
  esTribunal = false;
  private idDocente = 0;
  private idProyectoActual = 0;
  formProgramar: FormGroup;
  formCalificarDocente: FormGroup;
  formCalificarTribunal: FormGroup;
  constructor(private dt2: Dt2Service, private fb: FormBuilder) {
    this.formProgramar = this.fb.group({
      fecha: ['', Validators.required],
      hora: ['', Validators.required],
      lugar: ['', [Validators.required, Validators.minLength(3)]],
      observaciones: ['']
    });
    this.formCalificarDocente = this.fb.group({
      nota: [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      observaciones: ['']
    });
    this.formCalificarTribunal = this.fb.group({
      nota: [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      observaciones: [''],
      solicitudCorrecciones: [false]
    });
  }
  ngOnInit(): void {
    const user = getSessionUser();
    const rol = normalizeRole(user?.['rol']);
    this.esCoordinador = rol === 'ROLE_COORDINADOR' || rol === 'COORDINADOR';
    this.idDocente = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectosSegunRol();
  }
  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.idProyectoActual = p.idProyecto;
    this.error.set(null);
    this.ok.set(null);
    this.cargarPredefensa(p.idProyecto);
    this.tab.set('estado');
  }
  setTab(t: 'estado' | 'programar' | 'calificar'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
  }
  programar(): void {
    if (this.formProgramar.invalid) {
      this.formProgramar.markAllAsTouched();
      return;
    }
    const v = this.formProgramar.value;
    const req: ProgramarPredefensaRequest = {
      idRealizadoPor: this.idDocente,
      fecha: v.fecha,
      hora: v.hora + ':00',
      lugar: v.lugar,
      observaciones: v.observaciones
    };
    this.ejecutar(() => this.dt2.programarPredefensa(this.idProyectoActual, req));
  }
  calificarDocente(): void {
    if (this.formCalificarDocente.invalid) {
      this.formCalificarDocente.markAllAsTouched();
      return;
    }
    const v = this.formCalificarDocente.value;
    this.ejecutar(() => this.dt2.calificarPredefensaDocente(this.idProyectoActual, {
      idDocenteDt2: this.idDocente,
      nota: v.nota,
      observaciones: v.observaciones
    }));
  }
  calificarTribunal(): void {
    if (this.formCalificarTribunal.invalid) {
      this.formCalificarTribunal.markAllAsTouched();
      return;
    }
    const v = this.formCalificarTribunal.value;
    this.ejecutar(() => this.dt2.calificarPredefensaTribunal(this.idProyectoActual, {
      idDocente: this.idDocente,
      nota: v.nota,
      observaciones: v.observaciones,
      solicitudCorrecciones: v.solicitudCorrecciones
    }));
  }
  private cargarProyectosSegunRol(): void {
    if (this.esCoordinador) {
      this.loading.set(true);
      this.dt2.listarPendientesConfiguracion().pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.proyectos.set(data.filter(p => p.estadoProyecto === 'PREDEFENSA')),
        error: () => this.error.set('Error al cargar proyectos')
      });
    } else {
      this.loading.set(true);
      this.dt2.listarProyectosDirector(this.idDocente).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.proyectos.set(data.filter(p => p.estadoProyecto === 'PREDEFENSA')),
        error: () => this.error.set('Error al cargar proyectos')
      });
    }
  }
  private cargarPredefensa(idProyecto: number): void {
    this.dt2.getPredefensa(idProyecto).subscribe({
      next: data => this.predefensa.set(data),
      error: () => this.predefensa.set(null)
    });
  }
  private ejecutar(call: () => any): void {
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    call().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (res: any) => {
        this.ok.set(res?.mensaje ?? res?.estado ?? 'Operación completada');
        this.cargarPredefensa(this.idProyectoActual);
      },
      error: (err: any) => this.error.set(err?.error?.mensaje ?? 'Error en la operación')
    });
  }
}
