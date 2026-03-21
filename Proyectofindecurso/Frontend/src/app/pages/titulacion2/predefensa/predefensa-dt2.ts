import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  PredefensaDto,
  ConfiguracionProyectoDto,
  ProgramarPredefensaRequest
} from '../../../services/dt2.service';
import {
  SemanaPredefensaService,
  CalendarioSemanaDto,
  DiaCalendarioDto,
  SlotDto,
  AsignarSlotRequest
} from '../../../services/semana-predefensa.service';
import { getSessionUser, getSessionEntityId, getUserRoles } from '../../../services/session';

@Component({
  selector: 'app-predefensa-dt2',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
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

  esCoordinador = false;
  esDocenteDt2  = false;
  esTribunal    = false;

  private idDocente = 0;
  private idProyectoActual = 0;

  // Calendario
  calendario = signal<CalendarioSemanaDto | null>(null);
  cargandoCalendario = false;
  slotSeleccionado = signal<SlotDto | null>(null);
  lugarSlot = '';
  asignando = false;

  formProgramar: FormGroup;
  formCalificarDocente: FormGroup;
  formCalificarTribunal: FormGroup;

  constructor(
    private dt2: Dt2Service,
    private semanaService: SemanaPredefensaService,
    private fb: FormBuilder
  ) {
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
    this.esCoordinador = roles.includes('COORDINADOR') || roles.includes('ADMIN');
    this.idDocente = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
    if (this.esCoordinador) {
      this.cargarCalendario();
    }
  }

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.idProyectoActual = p.idProyecto;
    this.error.set(null);
    this.ok.set(null);
    this.esDocenteDt2 = false;
    this.esTribunal = false;
    this.slotSeleccionado.set(null);
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
    if (t === 'programar' && this.esCoordinador && !this.calendario()) {
      this.cargarCalendario();
    }
  }

  // ── Calendario de slots ─────────────────────────────────────────────────────

  cargarCalendario(): void {
    this.cargandoCalendario = true;
    this.semanaService.obtenerCalendario().subscribe({
      next: cal  => { this.calendario.set(cal); this.cargandoCalendario = false; },
      error: ()  => { this.cargandoCalendario = false; }
    });
  }

  seleccionarSlot(slot: SlotDto): void {
    if (slot.ocupado || !this.proyectoSeleccionado()) return;
    this.slotSeleccionado.set(slot);
    this.lugarSlot = this.calendario()?.semana?.lugarDefecto ?? '';
    this.error.set(null);
    this.ok.set(null);
  }

  cancelarSlot(): void {
    this.slotSeleccionado.set(null);
    this.lugarSlot = '';
  }

  confirmarSlot(): void {
    const slot = this.slotSeleccionado();
    const proyecto = this.proyectoSeleccionado();
    if (!slot || !proyecto) return;

    this.asignando = true;
    const req: AsignarSlotRequest = {
      idProyecto:     proyecto.idProyecto,
      fecha:          slot.fechaSlot,
      hora:           slot.horaInicio,
      lugar:          this.lugarSlot || this.calendario()?.semana?.lugarDefecto || 'Por definir',
      idRealizadoPor: this.idDocente
    };

    this.semanaService.asignarSlot(req).subscribe({
      next: res => {
        this.ok.set(res?.mensaje ?? 'Predefensa programada correctamente');
        this.slotSeleccionado.set(null);
        this.asignando = false;
        this.cargarCalendario();
        this.cargarPredefensa(proyecto.idProyecto);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error al asignar el slot');
        this.asignando = false;
      }
    });
  }

  // ── Formulario manual (fallback si no hay semana configurada) ───────────────

  programar(): void {
    if (this.formProgramar.invalid) { this.formProgramar.markAllAsTouched(); return; }
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
    if (this.formCalificarDocente.invalid) { this.formCalificarDocente.markAllAsTouched(); return; }
    const v = this.formCalificarDocente.value;
    this.ejecutar(() => this.dt2.calificarPredefensaDocente(this.idProyectoActual, {
      idDocenteDt2: this.idDocente,
      nota: v.nota,
      observaciones: v.observaciones
    }));
  }

  calificarTribunal(): void {
    if (this.formCalificarTribunal.invalid) { this.formCalificarTribunal.markAllAsTouched(); return; }
    const v = this.formCalificarTribunal.value;
    this.ejecutar(() => this.dt2.calificarPredefensaTribunal(this.idProyectoActual, {
      idDocente: this.idDocente,
      nota: v.nota,
      observaciones: v.observaciones,
      solicitudCorrecciones: v.solicitudCorrecciones
    }));
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  formatHora(hora: string): string {
    return hora ? hora.substring(0, 5) : '';
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '';
    const d = new Date(fecha + 'T00:00:00');
    return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
  }

  porcentajeOcupacion(dia: DiaCalendarioDto): number {
    if (!dia.totalSlots) return 0;
    return Math.round((dia.ocupados / dia.totalSlots) * 100);
  }

  // ── Privados ───────────────────────────────────────────────────────────────

  private detectarRolEnProyecto(idProyecto: number): void {
    this.dt2.getConfiguracion(idProyecto).subscribe({
      next: (config: ConfiguracionProyectoDto) => {
        this.esDocenteDt2 = config.idDocenteDt2 === this.idDocente;
        this.esTribunal   = config.tribunal?.some(m => m.idDocente === this.idDocente) ?? false;
        if (this.esDocenteDt2)       this.tab.set('calificar-dt2');
        else if (this.esTribunal)    this.tab.set('calificar-tribunal');
        else                         this.tab.set('estado');
      },
      error: () => this.tab.set('estado')
    });
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    if (this.esCoordinador) {
      this.dt2.listarProyectosEnPredefensa()
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: data => this.proyectos.set(data),
          error: () => this.error.set('Error al cargar proyectos')
        });
      return;
    }

    forkJoin({
      comoDirector: this.dt2.listarProyectosDirector(this.idDocente),
      comoDt2:      this.dt2.listarProyectosDocenteDt2(this.idDocente),
      comoTribunal: this.dt2.listarProyectosTribunal(this.idDocente)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ comoDirector, comoDt2, comoTribunal }) => {
          const todos  = [...comoDirector, ...comoDt2, ...comoTribunal];
          const unicos = todos.filter(
            (p, i, arr) => arr.findIndex(x => x.idProyecto === p.idProyecto) === i
          );
          this.proyectos.set(unicos.filter(p => p.estadoProyecto === 'PREDEFENSA'));
        },
        error: () => this.error.set('Error al cargar proyectos')
      });
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
