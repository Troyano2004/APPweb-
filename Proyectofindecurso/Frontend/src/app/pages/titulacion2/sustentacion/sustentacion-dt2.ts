import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  DocumentosPreviosDto,
  ResultadoSustentacionDto
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId, normalizeRole } from '../../../services/session';
@Component({
  selector: 'app-sustentacion-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './sustentacion-dt2.html',
  styleUrl: './sustentacion-dt2.scss'
})
export class SustentacionDt2Component implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  proyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  documentos = signal<DocumentosPreviosDto | null>(null);
  resultado = signal<ResultadoSustentacionDto | null>(null);
  tab = signal<'docs' | 'programar' | 'calificar' | 'resultado'>('docs');
  esCoordinador = false;
  private idDocente = 0;
  private idProyectoActual = 0;
  formDocs: FormGroup;
  formProgramar: FormGroup;
  formCalificar: FormGroup;
  formSegunda: FormGroup;
  constructor(private dt2: Dt2Service, private fb: FormBuilder) {
    this.formDocs = this.fb.group({
      ejemplarImpreso: [false],
      copiaDigitalBiblioteca: [false],
      copiasDigitalesTribunal: [false],
      informeCompilatioFirmado: [false],
      observaciones: ['']
    });
    this.formProgramar = this.fb.group({
      fecha: ['', Validators.required],
      hora: ['', Validators.required],
      lugar: ['', [Validators.required, Validators.minLength(3)]],
      observaciones: ['']
    });
    this.formCalificar = this.fb.group({
      calidadTrabajo: [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      originalidad:   [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      dominioTema:    [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      preguntas:      [null, [Validators.required, Validators.min(0), Validators.max(10)]],
      observaciones:  ['']
    });
    this.formSegunda = this.fb.group({
      fechaSustentacion: ['', Validators.required],
      hora: ['', Validators.required],
      lugar: ['', Validators.required],
      observaciones: ['']
    });
  }
  ngOnInit(): void {
    const user = getSessionUser();
    const rol = normalizeRole(user?.['rol']);
    this.esCoordinador = rol === 'ROLE_COORDINADOR' || rol === 'COORDINADOR';
    this.idDocente = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }
  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.idProyectoActual = p.idProyecto;
    this.error.set(null);
    this.ok.set(null);
    this.cargarDocumentos(p.idProyecto);
    this.tab.set('docs');
  }
  setTab(t: 'docs' | 'programar' | 'calificar' | 'resultado'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
    if (t === 'resultado') this.cargarResultado(this.idProyectoActual);
  }
  guardarDocumentos(): void {
    const v = this.formDocs.value;
    this.ejecutar(() => this.dt2.registrarDocumentosPrevios(this.idProyectoActual, {
      idRealizadoPor: this.idDocente,
      ejemplarImpreso: v.ejemplarImpreso,
      copiaDigitalBiblioteca: v.copiaDigitalBiblioteca,
      copiasDigitalesTribunal: v.copiasDigitalesTribunal,
      informeCompilatioFirmado: v.informeCompilatioFirmado,
      observaciones: v.observaciones
    }), () => this.cargarDocumentos(this.idProyectoActual));
  }
  programar(): void {
    if (this.formProgramar.invalid) { this.formProgramar.markAllAsTouched(); return; }
    const v = this.formProgramar.value;
    this.ejecutar(() => this.dt2.programarSustentacion(this.idProyectoActual, {
      idRealizadoPor: this.idDocente,
      fecha: v.fecha,
      hora: v.hora + ':00',
      lugar: v.lugar,
      observaciones: v.observaciones
    }));
  }
  calificar(): void {
    if (this.formCalificar.invalid) { this.formCalificar.markAllAsTouched(); return; }
    const v = this.formCalificar.value;
    this.ejecutar(() => this.dt2.calificarSustentacion(this.idProyectoActual, {
      idDocente: this.idDocente,
      calidadTrabajo: v.calidadTrabajo,
      originalidad: v.originalidad,
      dominioTema: v.dominioTema,
      preguntas: v.preguntas,
      observaciones: v.observaciones
    }), () => this.cargarResultado(this.idProyectoActual));
  }
  consolidar(): void {
    this.ejecutar(() => this.dt2.consolidarResultado(this.idProyectoActual),
      () => this.cargarResultado(this.idProyectoActual));
  }
  habilitarSegunda(): void {
    if (this.formSegunda.invalid) { this.formSegunda.markAllAsTouched(); return; }
    const v = this.formSegunda.value;
    this.ejecutar(() => this.dt2.habilitarSegundaOportunidad(this.idProyectoActual, {
      idRealizadoPor: this.idDocente,
      fechaSustentacion: v.fechaSustentacion,
      hora: v.hora + ':00',
      lugar: v.lugar,
      observaciones: v.observaciones
    }));
  }
  private cargarProyectos(): void {
    this.loading.set(true);
    if (this.esCoordinador) {
      this.dt2.listarPendientesConfiguracion().pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.proyectos.set(data.filter(p =>
          p.estadoProyecto === 'SUSTENTACION' || p.estadoProyecto === 'PREDEFENSA')),
        error: () => this.error.set('Error al cargar proyectos')
      });
    } else {
      this.dt2.listarProyectosDirector(this.idDocente).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.proyectos.set(data),
        error: () => this.error.set('Error al cargar proyectos')
      });
    }
  }
  private cargarDocumentos(idProyecto: number): void {
    this.dt2.getDocumentosPrevios(idProyecto).subscribe({
      next: data => {
        this.documentos.set(data);
        this.formDocs.patchValue({
          ejemplarImpreso: data.ejemplarImpreso,
          copiaDigitalBiblioteca: data.copiaDigitalBiblioteca,
          copiasDigitalesTribunal: data.copiasDigitalesTribunal,
          informeCompilatioFirmado: data.informeCompilatioFirmado,
          observaciones: data.observaciones ?? ''
        });
      },
      error: () => this.documentos.set(null)
    });
  }
  private cargarResultado(idProyecto: number): void {
    this.dt2.getResultado(idProyecto).subscribe({
      next: data => this.resultado.set(data),
      error: () => {}
    });
  }
  private ejecutar(call: () => any, onSuccess?: () => void): void {
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    call().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (res: any) => {
        this.ok.set(res?.mensaje ?? 'Operación completada');
        if (onSuccess) onSuccess();
      },
      error: (err: any) => this.error.set(err?.error?.mensaje ?? 'Error en la operación')
    });
  }
}
