import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  ConfiguracionProyectoDto,
  MiembroTribunalDto
} from '../../../services/dt2.service';
import { CoordinadorService, DirectorCarga } from '../../../services/coordinador';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

@Component({
  selector: 'app-configuracion-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuracion-dt2.html',
  styleUrl: './configuracion-dt2.scss'
})
export class ConfiguracionDt2Component implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  proyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  docentes = signal<DirectorCarga[]>([]);

  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  configuracion = signal<ConfiguracionProyectoDto | null>(null);

  tab = signal<'docente' | 'director' | 'tribunal'>('docente');

  formDocente: FormGroup;
  formDirector: FormGroup;
  formTribunal: FormGroup;

  private idRealizadoPor = 0;
  periodo = '';

  constructor(private dt2: Dt2Service, private coordinadorApi: CoordinadorService, private fb: FormBuilder) {
    this.formDocente = this.fb.group({
      idDocenteDt2: [null, [Validators.required, Validators.min(1)]],
      periodo: ['', Validators.required],
      observacion: ['']
    });

    this.formDirector = this.fb.group({
      idDirector: [null, [Validators.required, Validators.min(1)]],
      periodo: ['', Validators.required],
      motivo: ['']
    });

    this.formTribunal = this.fb.group({
      periodo: ['', Validators.required],
      miembros: this.fb.array([
        this.crearMiembro(),
        this.crearMiembro(),
        this.crearMiembro()
      ])
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idRealizadoPor = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
    this.cargarDocentes();
  }

  get miembros(): FormArray {
    return this.formTribunal.get('miembros') as FormArray;
  }

  addMiembro(): void {
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
  }

  setTab(t: 'docente' | 'director' | 'tribunal'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
  }

  asignarDocente(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formDocente.invalid) {
      this.formDocente.markAllAsTouched();
      return;
    }
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
    if (!p || this.formDirector.invalid) {
      this.formDirector.markAllAsTouched();
      return;
    }
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
    if (!p || this.formTribunal.invalid) {
      this.formTribunal.markAllAsTouched();
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

  nombreDocente(idDocente: number): string {
    return this.docentes().find(d => d.idDocente === idDocente)?.director ?? `Docente #${idDocente}`;
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    this.dt2.listarPendientesConfiguracion().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => this.proyectos.set(data),
      error: () => this.error.set('Error al cargar proyectos pendientes')
    });
  }

  private cargarDocentes(): void {
    this.coordinadorApi.getCargaDirectores().subscribe({
      next: data => this.docentes.set(data),
      error: () => {}
    });
  }

  private cargarConfiguracion(idProyecto: number): void {
    this.dt2.getConfiguracion(idProyecto).subscribe({
      next: data => this.configuracion.set(data),
      error: () => {}
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
        this.cargarProyectos();
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
      cargo: ['VOCAL', Validators.required]
    });
  }
}
