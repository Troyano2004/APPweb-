import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, forkJoin, map, of } from 'rxjs';
import {
  AsignarTribunalRequest,
  NotaTribunalRequest,
  RegistrarResultadoRequest,
  TitulacionWorkflowService
} from '../../../services/titulacion-workflow';
import { CoordinadorService, DirectorCarga, SeguimientoProyecto } from '../../../services/coordinador';

interface DocumentoOption {
  idDocumento: number;
  titulo: string;
  estudiante: string;
  estado: string;
}

@Component({
  selector: 'app-titulacion-workflow',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workflow.html',
  styleUrl: './workflow.scss'
})
export class TitulacionWorkflowComponent {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  estado = signal<string>('SELECCIONA_UN_DOCUMENTO');
  notaFinal = signal<number | null>(null);
  mostrarPostStage = signal(false);

  modalDocumentoOpen = signal(false);
  modalDocenteOpen = signal(false);
  cargandoOpciones = signal(false);

  documentos = signal<DocumentoOption[]>([]);
  docentes = signal<DirectorCarga[]>([]);

  filtroDocumento = signal('');
  filtroDocente = signal('');

  selectedDocumentoLabel = signal('');

  // destino de selección de docente: miembro o nota + índice
  docenteTarget: { type: 'miembro' | 'nota'; index: number } | null = null;

  formBase: FormGroup;
  formTribunal: FormGroup;
  formAgenda: FormGroup;
  formResultado: FormGroup;
  formCierre: FormGroup;

  constructor(
    private fb: FormBuilder,
    private api: TitulacionWorkflowService,
    private coordinadorApi: CoordinadorService
  ) {
    this.formBase = this.fb.group({
      idDocumento: [null, [Validators.required, Validators.min(1)]],
      avalUrlPdf: ['', Validators.required],
      avalComentario: [''],
      porcentajeAntiplagio: [0, [Validators.required, Validators.min(0)]],
      umbralAntiplagio: [20, [Validators.required, Validators.min(0)]],
      urlInformeAntiplagio: ['']
    });

    this.formTribunal = this.fb.group({
      miembros: this.fb.array([this.crearMiembro(), this.crearMiembro(), this.crearMiembro()])
    });

    this.formAgenda = this.fb.group({
      fecha: ['', Validators.required],
      hora: ['', Validators.required],
      lugar: ['', Validators.required],
      observaciones: [''],
      motivoReprogramacion: ['']
    });

    this.formResultado = this.fb.group({
      notaDocente: [0, [Validators.required, Validators.min(0), Validators.max(10)]],
      notasTribunal: this.fb.array([this.crearNota(), this.crearNota(), this.crearNota()]),
      actaUrl: ['', Validators.required],
      actaFirmadaUrl: [''],
      resultado: ['APROBADO'],
      observaciones: ['']
    });

    this.formCierre = this.fb.group({
      resultadoFinal: ['APROBADO', Validators.required],
      observacionesFinales: ['']
    });
  }

  get miembros(): FormArray {
    return this.formTribunal.get('miembros') as FormArray;
  }

  get notasTribunal(): FormArray {
    return this.formResultado.get('notasTribunal') as FormArray;
  }

  get documentosFiltrados(): DocumentoOption[] {
    const f = this.filtroDocumento().trim().toLowerCase();
    if (!f) return this.documentosAprobados;
    return this.documentosAprobados.filter((d) =>
      `${d.idDocumento} ${d.titulo} ${d.estudiante} ${d.estado}`.toLowerCase().includes(f)
    );
  }


  get documentosAprobados(): DocumentoOption[] {
    return this.documentos().filter((d) => d.estado === 'APROBADO_POR_DIRECTOR');
  }

  get docentesFiltrados(): DirectorCarga[] {
    const f = this.filtroDocente().trim().toLowerCase();
    if (!f) return this.docentes();
    return this.docentes().filter((d) => `${d.idDocente} ${d.director}`.toLowerCase().includes(f));
  }


  togglePostStage(): void {
    this.mostrarPostStage.set(!this.mostrarPostStage());
  }

  addMiembro(): void {
    this.miembros.push(this.crearMiembro());
  }

  addNota(): void {
    this.notasTribunal.push(this.crearNota());
  }

  abrirSelectorDocumento(): void {
    this.modalDocumentoOpen.set(true);
    this.filtroDocumento.set('');
    if (this.documentos().length === 0) {
      this.cargarDocumentos();
    }
  }

  cerrarSelectorDocumento(): void {
    this.modalDocumentoOpen.set(false);
  }

  seleccionarDocumento(doc: DocumentoOption): void {
    if (doc.estado !== 'APROBADO_POR_DIRECTOR') {
      this.error.set('Solo puedes seleccionar documentos APROBADO_POR_DIRECTOR en este paso.');
      return;
    }

    this.formBase.patchValue({ idDocumento: doc.idDocumento });
    this.selectedDocumentoLabel.set(`#${doc.idDocumento} · ${doc.titulo} · ${doc.estudiante}`);
    this.estado.set(doc.estado || 'SELECCIONADO');
    this.ok.set(null);
    this.error.set(null);
    this.cerrarSelectorDocumento();
  }

  abrirSelectorDocente(type: 'miembro' | 'nota', index: number): void {
    this.docenteTarget = { type, index };
    this.modalDocenteOpen.set(true);
    this.filtroDocente.set('');
    if (this.docentes().length === 0) {
      this.cargarDocentes();
    }
  }

  cerrarSelectorDocente(): void {
    this.modalDocenteOpen.set(false);
    this.docenteTarget = null;
  }

  seleccionarDocente(docente: DirectorCarga): void {
    if (!this.docenteTarget) return;

    if (this.docenteTarget.type === 'miembro') {
      const row = this.miembros.at(this.docenteTarget.index);
      row.patchValue({ idDocente: docente.idDocente, nombreDocente: docente.director });
    } else {
      const row = this.notasTribunal.at(this.docenteTarget.index);
      row.patchValue({ idDocente: docente.idDocente, nombreDocente: docente.director });
    }

    this.cerrarSelectorDocente();
  }

  listoParaTribunal(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento) return;

    if (this.formBase.invalid) {
      this.formBase.markAllAsTouched();
      const faltantes: string[] = [];
      if (!this.formBase.value.avalUrlPdf) faltantes.push('URL Aval Director (PDF)');
      if (this.formBase.value.porcentajeAntiplagio === null || this.formBase.value.porcentajeAntiplagio === undefined) {
        faltantes.push('% Antiplagio');
      }
      if (this.formBase.value.umbralAntiplagio === null || this.formBase.value.umbralAntiplagio === undefined) {
        faltantes.push('Umbral Antiplagio');
      }
      this.error.set(`Completa los campos requeridos antes de enviar: ${faltantes.join(', ') || 'revisa el formulario'}.`);
      return;
    }

    const payload = {
      avalUrlPdf: this.formBase.value.avalUrlPdf,
      avalComentario: this.formBase.value.avalComentario,
      porcentajeAntiplagio: Number(this.formBase.value.porcentajeAntiplagio),
      umbralAntiplagio: Number(this.formBase.value.umbralAntiplagio),
      urlInformeAntiplagio: this.formBase.value.urlInformeAntiplagio
    };

    this.execute(() => this.api.listoParaTribunal(idDocumento, payload));
  }

  asignarTribunal(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formTribunal.invalid) return;

    const payload: AsignarTribunalRequest = {
      miembros: this.miembros.controls.map((row) => ({
        idDocente: Number(row.value.idDocente),
        cargo: row.value.cargo
      }))
    };

    this.execute(() => this.api.asignarTribunal(idDocumento, payload));
  }

  agendar(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formAgenda.invalid) return;

    this.execute(() => this.api.agendarSustentacion(idDocumento, this.formAgenda.getRawValue()));
  }

  registrarResultado(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formResultado.invalid) return;

    const notasTribunal: NotaTribunalRequest[] = this.notasTribunal.controls.map((row) => ({
      idDocente: Number(row.value.idDocente),
      nota: Number(row.value.nota)
    }));

    const payload: RegistrarResultadoRequest = {
      notaDocente: Number(this.formResultado.value.notaDocente),
      notasTribunal,
      actaUrl: this.formResultado.value.actaUrl,
      actaFirmadaUrl: this.formResultado.value.actaFirmadaUrl,
      resultado: this.formResultado.value.resultado,
      observaciones: this.formResultado.value.observaciones
    };

    this.execute(() => this.api.registrarResultado(idDocumento, payload));
  }

  cerrar(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formCierre.invalid) return;

    this.execute(() => this.api.cerrarExpediente(idDocumento, this.formCierre.getRawValue()));
  }

  onFiltroDocumento(value: string): void {
    this.filtroDocumento.set(value);
  }

  onFiltroDocente(value: string): void {
    this.filtroDocente.set(value);
  }

  private cargarDocumentos(): void {
    this.cargandoOpciones.set(true);
    this.coordinadorApi
      .getSeguimiento()
      .pipe(
        map((rows) => rows ?? []),
        catchError(() => of([] as SeguimientoProyecto[]))
      )
      .subscribe((seguimiento) => {
        if (seguimiento.length === 0) {
          this.cargandoOpciones.set(false);
          this.documentos.set([]);
          return;
        }

        const reqs = seguimiento.map((s) =>
          this.coordinadorApi.getDocumentoProyecto(s.idProyecto).pipe(
            map((doc) => ({
              idDocumento: Number(doc.id ?? doc.idDocumento ?? 0),
              titulo: doc.titulo || s.tituloProyecto || 'Sin título',
              estudiante: s.estudiante || `Proyecto ${s.idProyecto}`,
              estado: String(doc.estado || s.estado || '')
            })),
            catchError(() => of(null))
          )
        );

        forkJoin(reqs).subscribe((all) => {
          const clean = (all.filter((x) => x && x.idDocumento > 0) as DocumentoOption[]).sort(
            (a, b) => a.idDocumento - b.idDocumento
          );
          this.documentos.set(clean);
          if (clean.filter((d) => d.estado === 'APROBADO_POR_DIRECTOR').length === 0) {
            this.ok.set('No hay documentos APROBADO_POR_DIRECTOR disponibles para iniciar LISTO_PARA_TRIBUNAL.');
          }
          this.cargandoOpciones.set(false);
        });
      });
  }

  private cargarDocentes(): void {
    this.cargandoOpciones.set(true);
    this.coordinadorApi.getCargaDirectores().subscribe({
      next: (rows) => {
        this.docentes.set(rows ?? []);
        this.cargandoOpciones.set(false);
      },
      error: () => {
        this.docentes.set([]);
        this.cargandoOpciones.set(false);
      }
    });
  }

  private execute(call: () => any): void {
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    call().subscribe({
      next: (res: any) => {
        this.loading.set(false);
        this.ok.set(res?.mensaje ?? 'Operación completada');
        this.estado.set(res?.estado ?? this.estado());
        this.notaFinal.set(res?.notaFinal ?? null);
      },
      error: (err: any) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo ejecutar la operación');
      }
    });
  }

  private getIdDocumento(): number | null {
    const id = Number(this.formBase.get('idDocumento')?.value);
    if (!id || Number.isNaN(id)) {
      this.error.set('Selecciona un documento desde el botón Buscar documento');
      return null;
    }
    return id;
  }

  private crearMiembro(): FormGroup {
    return this.fb.group({
      idDocente: [null, [Validators.required, Validators.min(1)]],
      nombreDocente: [''],
      cargo: ['VOCAL', Validators.required]
    });
  }

  private crearNota(): FormGroup {
    return this.fb.group({
      idDocente: [null, [Validators.required, Validators.min(1)]],
      nombreDocente: [''],
      nota: [0, [Validators.required, Validators.min(0), Validators.max(10)]]
    });
  }
}
