import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  SeguimientoDto,
  AsesoriaDto,
  ActaCorteDto
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

@Component({
  selector: 'app-seguimiento-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './seguimiento-dt2.html',
  styleUrl: './seguimiento-dt2.scss'
})
export class SeguimientoDt2Component implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  proyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  seguimiento = signal<SeguimientoDto | null>(null);
  asesorias = signal<AsesoriaDto[]>([]);

  tab = signal<'asesorias' | 'registrar' | 'corte'>('asesorias');

  formAsesoria: FormGroup;
  formCorte: FormGroup;

  private idDirector = 0;

  constructor(private dt2: Dt2Service, private fb: FormBuilder) {
    this.formAsesoria = this.fb.group({
      fecha: ['', Validators.required],
      observaciones: ['', Validators.required],
      evidenciaUrl: [''],
      porcentajeAvance: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      numeroCorte: [1, [Validators.required, Validators.min(1), Validators.max(2)]],
      calificacion: [null]
    });

    this.formCorte = this.fb.group({
      numeroCorte: [1, [Validators.required, Validators.min(1), Validators.max(2)]],
      notaCorte: [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      observaciones: ['']
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDirector = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.error.set(null);
    this.ok.set(null);
    this.cargarSeguimiento(p.idProyecto);
    this.cargarAsesorias(p.idProyecto);
    this.tab.set('asesorias');
  }

  setTab(t: 'asesorias' | 'registrar' | 'corte'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
  }

  registrarAsesoria(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formAsesoria.invalid) {
      this.formAsesoria.markAllAsTouched();
      return;
    }
    const v = this.formAsesoria.value;
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.dt2.registrarAsesoria(p.idProyecto, {
      idDirector: this.idDirector,
      fecha: v.fecha + ':00',
      observaciones: v.observaciones,
      evidenciaUrl: v.evidenciaUrl,
      porcentajeAvance: v.porcentajeAvance,
      numeroCorte: v.numeroCorte,
      calificacion: v.calificacion
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.ok.set('Asesoría registrada correctamente');
        this.formAsesoria.reset({ numeroCorte: 1, porcentajeAvance: 0 });
        this.cargarAsesorias(p.idProyecto);
        this.cargarSeguimiento(p.idProyecto);
        this.tab.set('asesorias');
      },
      error: (err: any) => this.error.set(err?.error?.mensaje ?? 'Error al registrar asesoría')
    });
  }

  cerrarCorte(): void {
    const p = this.proyectoSeleccionado();
    if (!p || this.formCorte.invalid) {
      this.formCorte.markAllAsTouched();
      return;
    }
    const v = this.formCorte.value;
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.dt2.cerrarCorte(p.idProyecto, {
      idDirector: this.idDirector,
      numeroCorte: v.numeroCorte,
      notaCorte: v.notaCorte,
      observaciones: v.observaciones
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (acta: ActaCorteDto) => {
        const advertencia = acta.advertencia ? ` ⚠ ${acta.advertencia}` : '';
        this.ok.set(`Corte ${acta.numeroCorte} cerrado. Nota: ${acta.notaCorte}.${advertencia}`);
        this.cargarSeguimiento(p.idProyecto);
        this.tab.set('asesorias');
      },
      error: (err: any) => this.error.set(err?.error?.mensaje ?? 'Error al cerrar corte')
    });
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    this.dt2.listarProyectosDirector(this.idDirector).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => this.proyectos.set(data),
      error: () => this.error.set('Error al cargar proyectos')
    });
  }

  private cargarSeguimiento(idProyecto: number): void {
    this.dt2.getSeguimiento(idProyecto).subscribe({
      next: data => this.seguimiento.set(data),
      error: () => {}
    });
  }

  private cargarAsesorias(idProyecto: number): void {
    this.dt2.listarAsesorias(idProyecto).subscribe({
      next: data => this.asesorias.set(data),
      error: () => {}
    });
  }
}
