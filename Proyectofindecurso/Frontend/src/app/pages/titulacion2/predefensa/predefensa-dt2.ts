import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  PredefensaDto,
  ConfiguracionProyectoDto,
  ProgramarPredefensaRequest
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId, getUserRoles } from '../../../services/session';

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

  tab = signal<'estado' | 'programar' | 'calificar-dt2' | 'calificar-tribunal'>('estado');

  // ── Rol del usuario en este proyecto ──────────────────────
  esCoordinador = false;
  esDocenteDt2  = false;  // es el DT2 asignado a este proyecto
  esTribunal    = false;  // es miembro del tribunal de este proyecto

  private idDocente = 0;
  private idProyectoActual = 0;

  formProgramar: FormGroup;
  formCalificarDocente: FormGroup;
  formCalificarTribunal: FormGroup;

  constructor(private dt2: Dt2Service, private fb: FormBuilder) {
    this.formProgramar = this.fb.group({
      fecha:         ['', Validators.required],
      hora:          ['', Validators.required],
      lugar:         ['', [Validators.required, Validators.minLength(3)]],
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
    const roles = getUserRoles().map(r => r.replace('ROLE_', ''));
    this.esCoordinador = roles.includes('COORDINADOR');
    this.idDocente = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.idProyectoActual = p.idProyecto;
    this.error.set(null);
    this.ok.set(null);
    this.esDocenteDt2 = false;
    this.esTribunal = false;
    this.tab.set('estado');
    this.cargarPredefensa(p.idProyecto);
    if (this.esCoordinador) {
      this.tab.set('programar');
    } else {
      this.detectarRolEnProyecto(p.idProyecto);
    }
  }

  setTab(t: 'estado' | 'programar' | 'calificar-dt2' | 'calificar-tribunal'): void {
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

  // ── Detecta si el docente logueado es DT2 o Tribunal en este proyecto ──
  private detectarRolEnProyecto(idProyecto: number): void {
    this.dt2.getConfiguracion(idProyecto).subscribe({
      next: (config: ConfiguracionProyectoDto) => {
        // ¿Es el DT2 asignado?
        this.esDocenteDt2 = config.idDocenteDt2 === this.idDocente;

        // ¿Es miembro del tribunal?
        this.esTribunal = config.tribunal?.some(m => m.idDocente === this.idDocente) ?? false;

        // Pre-seleccionar tab según rol
        if (this.esDocenteDt2) {
          this.tab.set('calificar-dt2');
        } else if (this.esTribunal) {
          this.tab.set('calificar-tribunal');
        } else {
          this.tab.set('estado');
        }
      },
      error: () => {
        // Si no puede cargar configuración, mostrar solo estado
        this.tab.set('estado');
      }
    });
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    if (this.esCoordinador) {
      // ✅ Coordinador usa endpoint dedicado para proyectos en PREDEFENSA
      this.dt2.listarProyectosEnPredefensa()
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: data => this.proyectos.set(data),
          error: () => this.error.set('Error al cargar proyectos')
        });
    } else {
      // Docente ve proyectos donde es director/DT2 en PREDEFENSA
      this.dt2.listarProyectosDirector(this.idDocente)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
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
        this.formCalificarDocente.reset();
        this.formCalificarTribunal.reset();
      },
      error: (err: any) => this.error.set(err?.error?.mensaje ?? err?.error?.message ?? 'Error en la operación')
    });
  }
}
