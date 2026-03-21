import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  ConfiguracionProyectoDto,
  PredefensaDto,
  ProgramarPredefensaRequest
} from '../../../services/dt2.service';
import { CoordinadorService, DirectorCarga } from '../../../services/coordinador';
import { CatalogosBasicosService, PeriodoTitulacion } from '../../../services/catalogos-basicos.service';
import {
  SemanaPredefensaService,
  SemanaPredefensaDto,
  GuardarSemanaRequest,
  ExtenderSemanaRequest
} from '../../../services/semana-predefensa.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

interface OpcionPeriodo {
  etiqueta: string;
  valor: string;
  idPeriodo?: number;
}

type FiltroLista = 'todos' | 'pendientes' | 'completos';

@Component({
  selector: 'app-configuracion-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuracion-dt2.html',
  styleUrl: './configuracion-dt2.scss'
})
export class ConfiguracionDt2Component implements OnInit {
  loading    = signal(false);
  error      = signal<string | null>(null);
  ok         = signal<string | null>(null);

  // ── Proyectos — ahora carga TODOS ────────────────────────────────────────
  todosLosProyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  filtroLista       = signal<FiltroLista>('todos');

  // Proyectos filtrados según pestaña activa
  proyectosFiltrados = computed(() => {
    const todos = this.todosLosProyectos();
    switch (this.filtroLista()) {
      case 'pendientes': return todos.filter(p => !p.configuracionCompleta);
      case 'completos':  return todos.filter(p =>  p.configuracionCompleta);
      default:           return todos;
    }
  });

  get totalPendientes(): number {
    return this.todosLosProyectos().filter(p => !p.configuracionCompleta).length;
  }
  get totalCompletos(): number {
    return this.todosLosProyectos().filter(p => p.configuracionCompleta).length;
  }

  docentes               = signal<DirectorCarga[]>([]);
  proyectoSeleccionado   = signal<ProyectoPendienteConfiguracionDto | null>(null);
  configuracion          = signal<ConfiguracionProyectoDto | null>(null);
  tab                    = signal<'docente' | 'director' | 'tribunal' | 'predefensa' | 'semana'>('docente');

  // Periodos
  periodosActivos: PeriodoTitulacion[] = [];
  opcionesPeriodo: OpcionPeriodo[]     = [];

  predefensaActual = signal<PredefensaDto | null>(null);

  // Semana predefensas
  semanaActual        = signal<SemanaPredefensaDto | null>(null);
  cargandoSemana      = false;
  mostrarFormExtender = false;

  formDocente:   FormGroup;
  formPredefensa: FormGroup;
  formDirector:  FormGroup;
  formTribunal:  FormGroup;
  formSemana:    FormGroup;
  formExtender:  FormGroup;

  private idRealizadoPor = 0;

  constructor(
    private dt2:                   Dt2Service,
    private coordinadorApi:        CoordinadorService,
    private catalogosBasicosService: CatalogosBasicosService,
    private semanaService:          SemanaPredefensaService,
    private fb:                    FormBuilder
  ) {
    this.formDocente = this.fb.group({
      idDocenteDt2: [null, [Validators.required, Validators.min(1)]],
      periodo:      ['', Validators.required],
      observacion:  ['']
    });

    this.formDirector = this.fb.group({
      idDirector: [null, [Validators.required, Validators.min(1)]],
      periodo:    ['', Validators.required],
      motivo:     ['']
    });

    this.formPredefensa = this.fb.group({
      fecha:         ['', Validators.required],
      hora:          ['', Validators.required],
      lugar:         ['', [Validators.required, Validators.minLength(3)]],
      observaciones: ['']
    });

    this.formTribunal = this.fb.group({
      periodo:  ['', Validators.required],
      miembros: this.fb.array([
        this.crearMiembro(),
        this.crearMiembro(),
        this.crearMiembro()
      ])
    });

    this.formSemana = this.fb.group({
      fechaInicio:     ['', Validators.required],
      fechaFin:        ['', Validators.required],
      horaInicio:      ['08:00', Validators.required],
      horaFin:         ['18:00', Validators.required],
      duracionMinutos: [60, [Validators.required, Validators.min(15), Validators.max(240)]],
      lugarDefecto:    [''],
      observaciones:   [''],
      idPeriodo:       [null]
    });

    this.formExtender = this.fb.group({
      fechaFin:        [''],
      horaInicio:      [''],
      horaFin:         [''],
      duracionMinutos: [null, [Validators.min(15), Validators.max(240)]],
      lugarDefecto:    [''],
      observaciones:   ['']
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idRealizadoPor = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarPeriodosActivos();
    this.cargarTodosProyectos();   // ← carga TODOS en vez de solo pendientes
    this.cargarDocentes();
    this.cargarSemanaActual();
  }

  get miembros(): FormArray {
    return this.formTribunal.get('miembros') as FormArray;
  }

  // ── Filtro de lista ────────────────────────────────────────────────────────

  setFiltroLista(f: FiltroLista): void {
    this.filtroLista.set(f);
  }

  // ── Carga de proyectos ─────────────────────────────────────────────────────

  private cargarTodosProyectos(): void {
    this.loading.set(true);
    this.dt2.listarTodosConfiguracion()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next:  data  => this.todosLosProyectos.set(data),
        error: ()    => this.error.set('Error al cargar proyectos')
      });
  }

  addMiembro(): void {
    const seleccionados = this.miembros.controls
      .filter(c => c.get('idDocente')?.value != null).length;
    if (this.docentes().length > 0 && seleccionados >= this.docentes().length) {
      this.error.set('Ya no hay docentes disponibles para agregar.');
      return;
    }
    this.miembros.push(this.crearMiembro());
  }

  removeMiembro(i: number): void {
    if (this.miembros.length > 3) this.miembros.removeAt(i);
  }

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.error.set(null);
    this.ok.set(null);
    this.cargarConfiguracion(p.idProyecto);
    this.cargarPredefensa(p.idProyecto);
    // Si el proyecto ya está completo, ir directamente a docente para editar
    this.tab.set('docente');
  }

  setTab(t: 'docente' | 'director' | 'tribunal' | 'predefensa' | 'semana'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
    if (t === 'semana') this.cargarSemanaActual();
  }

  // ── Semana de predefensas ──────────────────────────────────────────────────

  cargarSemanaActual(): void {
    this.cargandoSemana = true;
    this.semanaService.obtenerSemana().subscribe({
      next: semana => {
        this.semanaActual.set(semana);
        if (semana) {
          this.formSemana.patchValue({
            fechaInicio:     semana.fechaInicio,
            fechaFin:        semana.fechaFin,
            horaInicio:      semana.horaInicio,
            horaFin:         semana.horaFin,
            duracionMinutos: semana.duracionMinutos,
            lugarDefecto:    semana.lugarDefecto ?? '',
            observaciones:   semana.observaciones ?? ''
          });
        }
        this.cargandoSemana = false;
      },
      error: () => { this.cargandoSemana = false; }
    });
  }

  abrirFormExtender(): void {
    const s = this.semanaActual();
    if (s) {
      this.formExtender.patchValue({
        fechaFin:        s.fechaFin,
        horaInicio:      s.horaInicio,
        horaFin:         s.horaFin,
        duracionMinutos: s.duracionMinutos,
        lugarDefecto:    s.lugarDefecto ?? '',
        observaciones:   s.observaciones ?? ''
      });
    }
    this.mostrarFormExtender = true;
  }

  extenderSemana(): void {
    const v = this.formExtender.value;
    const req: ExtenderSemanaRequest = {};
    if (v.fechaFin)        req.fechaFin        = v.fechaFin;
    if (v.horaInicio)      req.horaInicio      = v.horaInicio;
    if (v.horaFin)         req.horaFin         = v.horaFin;
    if (v.duracionMinutos) req.duracionMinutos  = Number(v.duracionMinutos);
    if (v.lugarDefecto)    req.lugarDefecto     = v.lugarDefecto;
    if (v.observaciones)   req.observaciones    = v.observaciones;

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.semanaService.extenderSemana(req).subscribe({
      next: semana => {
        this.semanaActual.set(semana);
        this.mostrarFormExtender = false;
        this.ok.set(`✅ Semana actualizada: ${semana.fechaInicio} al ${semana.fechaFin}`);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error al modificar la semana');
        this.loading.set(false);
      }
    });
  }

  guardarSemana(): void {
    if (this.formSemana.invalid) {
      this.formSemana.markAllAsTouched();
      return;
    }

    const v = this.formSemana.value;
    const req: GuardarSemanaRequest = {
      fechaInicio:     v.fechaInicio,
      fechaFin:        v.fechaFin,
      horaInicio:      v.horaInicio,
      horaFin:         v.horaFin,
      duracionMinutos: Number(v.duracionMinutos),
      lugarDefecto:    v.lugarDefecto,
      observaciones:   v.observaciones,
      idPeriodo:       v.idPeriodo ?? this.opcionesPeriodo[0]?.idPeriodo
    };

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.semanaService.guardarSemana(req).subscribe({
      next: semana => {
        this.semanaActual.set(semana);
        this.ok.set(`✅ Semana configurada: ${semana.fechaInicio} al ${semana.fechaFin}. Total: ${semana.totalSlots} slots`);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error al guardar la semana');
        this.loading.set(false);
      }
    });
  }

  // ── Acciones de configuración ─────────────────────────────────────────────

  asignarDocente(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formDocente.invalid) { this.formDocente.markAllAsTouched(); return; }
    const v = this.formDocente.value;
    this.ejecutar(() =>
      this.dt2.asignarDocenteDt2(p.idProyecto, {
        idDocenteDt2: v.idDocenteDt2,
        idRealizadoPor: this.idRealizadoPor,
        periodo: v.periodo,
        observacion: v.observacion
      }).pipe(finalize(() => {}))
    );
  }

  asignarDirector(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formDirector.invalid) { this.formDirector.markAllAsTouched(); return; }
    const v = this.formDirector.value;
    this.ejecutar(() =>
      this.dt2.asignarDirector(p.idProyecto, {
        idDirector: v.idDirector,
        idRealizadoPor: this.idRealizadoPor,
        periodo: v.periodo,
        motivo: v.motivo
      })
    );
  }

  asignarTribunal(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formTribunal.invalid) { this.formTribunal.markAllAsTouched(); return; }
    const ids = this.miembros.controls
      .map(c => c.get('idDocente')?.value)
      .filter(id => id != null && id > 0);
    if (new Set(ids).size !== ids.length) {
      this.error.set('No se puede repetir el mismo docente en el tribunal.');
      return;
    }
    const v = this.formTribunal.value;
    this.ejecutar(() =>
      this.dt2.asignarTribunal(p.idProyecto, {
        idRealizadoPor: this.idRealizadoPor,
        periodo: v.periodo,
        miembros: v.miembros.map((m: any) => ({ idDocente: Number(m.idDocente), cargo: m.cargo }))
      })
    );
  }

  programarPredefensa(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formPredefensa.invalid) { this.formPredefensa.markAllAsTouched(); return; }
    const v = this.formPredefensa.value;
    const req: ProgramarPredefensaRequest = {
      idRealizadoPor: this.idRealizadoPor,
      fecha: v.fecha,
      hora:  v.hora + ':00',
      lugar: v.lugar,
      observaciones: v.observaciones
    };
    this.ejecutar(() => this.dt2.programarPredefensa(p.idProyecto, req));
  }

  nombreDocente(idDocente: number): string {
    return this.docentes().find(d => d.idDocente === idDocente)?.director ?? `Docente #${idDocente}`;
  }

  getDocentesParaFila(indexFila: number): DirectorCarga[] {
    const seleccionActual = this.miembros.at(indexFila)?.get('idDocente')?.value ?? null;
    const idsSeleccionados = new Set(
      this.miembros.controls
        .map(c => c.get('idDocente')?.value)
        .filter((id): id is number => id !== null && id !== undefined && id > 0)
    );
    return this.docentes().filter(d => {
      if (d?.idDocente == null) return false;
      if (seleccionActual !== null && d.idDocente === seleccionActual) return true;
      return !idsSeleccionados.has(d.idDocente);
    });
  }

  calcularSlotsPreview(): number {
    const inicio   = this.formSemana.get('horaInicio')?.value as string;
    const fin      = this.formSemana.get('horaFin')?.value as string;
    const duracion = Number(this.formSemana.get('duracionMinutos')?.value);
    if (!inicio || !fin || !duracion || duracion < 15) return 0;
    const [hi, mi] = inicio.split(':').map(Number);
    const [hf, mf] = fin.split(':').map(Number);
    const totalMin = (hf * 60 + mf) - (hi * 60 + mi);
    return totalMin > 0 ? Math.floor(totalMin / duracion) : 0;
  }

  // ── Etiqueta visual del estado del proyecto ────────────────────────────────

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      ANTEPROYECTO: 'Anteproyecto',
      BORRADOR:     'Borrador',
      DESARROLLO:   'En desarrollo',
      PREDEFENSA:   'En predefensa',
      SUSTENTACION: 'Sustentación',
      FINALIZADO:   'Finalizado',
      REPROBADO:    'Reprobado'
    };
    return map[estado?.toUpperCase()] ?? estado;
  }

  claseEstado(estado: string): string {
    const map: Record<string, string> = {
      ANTEPROYECTO: 'estado-pendiente',
      BORRADOR:     'estado-pendiente',
      DESARROLLO:   'estado-desarrollo',
      PREDEFENSA:   'estado-predefensa',
      SUSTENTACION: 'estado-sustentacion',
      FINALIZADO:   'estado-finalizado',
      REPROBADO:    'estado-reprobado'
    };
    return map[estado?.toUpperCase()] ?? '';
  }

  // ── Privados ──────────────────────────────────────────────────────────────

  private cargarPeriodosActivos(): void {
    this.catalogosBasicosService.listarPeriodosActivos().subscribe({
      next: data => {
        this.periodosActivos = data;
        this.opcionesPeriodo = data.map(p => ({
          etiqueta:  p.descripcion,
          valor:     this.construirPeriodo(p),
          idPeriodo: p.idPeriodo
        }));
        const primerValor = this.opcionesPeriodo.length > 0 ? this.opcionesPeriodo[0].valor     : '';
        const primerId    = this.opcionesPeriodo.length > 0 ? this.opcionesPeriodo[0].idPeriodo : null;
        this.formDocente.patchValue({ periodo: primerValor });
        this.formDirector.patchValue({ periodo: primerValor });
        this.formTribunal.patchValue({ periodo: primerValor });
        this.formSemana.patchValue({ idPeriodo: primerId });
      },
      error: () => this.error.set('No se pudo cargar el listado de periodos activos.')
    });
  }

  private construirPeriodo(periodo: PeriodoTitulacion): string {
    const descripcionLimpia = (periodo.descripcion ?? '').trim().replace(/\s+/g, ' ');
    if (descripcionLimpia.length <= 20) return descripcionLimpia;
    const anioInicio = (periodo.fechaInicio ?? '').toString().slice(0, 4);
    const anioFin    = (periodo.fechaFin    ?? '').toString().slice(0, 4);
    const etiqueta   = anioInicio && anioFin ? `${anioInicio}-${anioFin}` : '';
    if (etiqueta && etiqueta.length <= 20) return etiqueta;
    return descripcionLimpia.slice(0, 20);
  }

  private cargarDocentes(): void {
    this.coordinadorApi.getCargaDirectores().subscribe({
      next:  data => this.docentes.set(data),
      error: ()   => {}
    });
  }

  private cargarConfiguracion(idProyecto: number): void {
    this.dt2.getConfiguracion(idProyecto).subscribe({
      next:  data => this.configuracion.set(data),
      error: ()   => {}
    });
  }

  private cargarPredefensa(idProyecto: number): void {
    this.dt2.getPredefensa(idProyecto).subscribe({
      next:  data => this.predefensaActual.set(data),
      error: ()   => this.predefensaActual.set(null)
    });
  }

  private ejecutar(call: () => any): void {
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    call().subscribe({
      next: (res: any) => {
        this.loading.set(false);
        this.ok.set(res?.mensaje ?? 'Operación completada');
        const p = this.proyectoSeleccionado();
        if (p) this.cargarConfiguracion(p.idProyecto);
        this.cargarTodosProyectos();  // recarga todos para actualizar contadores
      },
      error: (err: any) => {
        this.loading.set(false);
        this.error.set(err?.error?.mensaje ?? err?.error?.message ?? 'Error en la operación');
      }
    });
  }

  private crearMiembro(): FormGroup {
    return this.fb.group({
      idDocente: [null, [Validators.required, Validators.min(1)]],
      cargo:     ['VOCAL', Validators.required]
    });
  }
}
