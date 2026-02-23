import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TitulacionWorkflowService } from '../../../services/titulacion-workflow';

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
  estado = signal<string>('SIN_DATOS');
  notaFinal = signal<number | null>(null);

  formBase: FormGroup;
  formTribunal: FormGroup;
  formAgenda: FormGroup;
  formResultado: FormGroup;
  formCierre: FormGroup;

  constructor(private fb: FormBuilder, private api: TitulacionWorkflowService) {
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

  addMiembro(): void {
    this.miembros.push(this.crearMiembro());
  }

  addNota(): void {
    this.notasTribunal.push(this.crearNota());
  }

  listoParaTribunal(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formBase.invalid) return;

    this.execute(() => this.api.listoParaTribunal(idDocumento, this.formBase.getRawValue()));
  }

  asignarTribunal(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formTribunal.invalid) return;

    this.execute(() => this.api.asignarTribunal(idDocumento, this.formTribunal.getRawValue()));
  }

  agendar(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formAgenda.invalid) return;

    this.execute(() => this.api.agendarSustentacion(idDocumento, this.formAgenda.getRawValue()));
  }

  registrarResultado(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formResultado.invalid) return;

    this.execute(() => this.api.registrarResultado(idDocumento, this.formResultado.getRawValue()));
  }

  cerrar(): void {
    const idDocumento = this.getIdDocumento();
    if (!idDocumento || this.formCierre.invalid) return;

    this.execute(() => this.api.cerrarExpediente(idDocumento, this.formCierre.getRawValue()));
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
      this.error.set('Ingresa un ID de documento válido');
      return null;
    }
    return id;
  }

  private crearMiembro(): FormGroup {
    return this.fb.group({
      idDocente: [null, [Validators.required, Validators.min(1)]],
      cargo: ['VOCAL', Validators.required]
    });
  }

  private crearNota(): FormGroup {
    return this.fb.group({
      idDocente: [null, [Validators.required, Validators.min(1)]],
      nota: [0, [Validators.required, Validators.min(0), Validators.max(10)]]
    });
  }
}
