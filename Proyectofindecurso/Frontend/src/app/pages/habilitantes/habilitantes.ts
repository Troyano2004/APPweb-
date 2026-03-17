
import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  DocumentoHabilitanteService,
  HabilitanteDto,
  ResumenHabilitacionDto,
  SubirHabilitanteRequest,
  ValidarHabilitanteRequest
} from '../../services/documento-habilitante.service';

import { getSessionEntityId, getSessionUser } from '../../services/session';

type RolView = 'estudiante' | 'director' | 'coordinador';

@Component({
  selector: 'app-documentos-habilitantes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './habilitantes.html',
  styleUrls: ['./habilitantes.scss']
})
export class DocumentosHabilitantesComponent implements OnInit {

  readonly rol = signal<RolView>('estudiante');
  readonly idEntidad = signal<number | null>(null);

  loading  = signal(false);
  error    = signal<string | null>(null);
  ok       = signal<string | null>(null);

  resumen    = signal<ResumenHabilitacionDto | null>(null);
  pendientes = signal<HabilitanteDto[]>([]);

  modalSubirOpen   = signal(false);
  modalValidarOpen = signal(false);
  docSeleccionado  = signal<HabilitanteDto | null>(null);
  uploadingFile    = signal(false);
  archivoUrl       = signal<string | null>(null);
  archivoNombre    = signal<string | null>(null);

  formSubir!: FormGroup;
  formValidar!: FormGroup;

  esEstudiante = computed(() => this.rol() === 'estudiante');
  esDirector   = computed(() => this.rol() === 'director' || this.rol() === 'coordinador');

  progreso = computed(() => {
    const r = this.resumen();
    if (!r || !r.totalDocumentos) return 0;
    return Math.round((r.aprobados / r.totalDocumentos) * 100);
  });

  constructor(
    private api: DocumentoHabilitanteService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.detectarRol();
    this.buildForms();
    this.cargarDatos();
  }

  private detectarRol(): void {
    const user = getSessionUser();
    const rolRaw = String(user?.['rol'] ?? '')
      .replace('ROLE_', '')
      .trim()
      .toUpperCase();

    if (rolRaw === 'ESTUDIANTE') {
      this.rol.set('estudiante');
      this.idEntidad.set(getSessionEntityId(user, 'estudiante'));
    } else if (rolRaw === 'DOCENTE') {
      this.rol.set('director');
      this.idEntidad.set(getSessionEntityId(user, 'docente'));
    } else {
      // COORDINADOR / ADMIN: no tienen kind propio en getSessionEntityId,
      // pero su id viene en idDocente o idUsuario (mismas keys que 'docente')
      this.rol.set('coordinador');
      this.idEntidad.set(getSessionEntityId(user, 'docente'));
    }
  }

  private buildForms(): void {
    this.formSubir = this.fb.group({
      porcentajeCoincidencia: [null],
      umbralPermitido: [10.0]
    });

    this.formValidar = this.fb.group({
      decision:   ['APROBADO', Validators.required],
      comentario: ['']
    });
  }

  cargarDatos(): void {
    const id = this.idEntidad();
    if (!id) { this.error.set('No se identificó al usuario.'); return; }

    this.loading.set(true);
    this.error.set(null);

    if (this.esEstudiante()) {
      this.api.getResumenEstudiante(id).subscribe({
        next: (r) => { this.resumen.set(r); this.loading.set(false); },
        error: (e) => { this.error.set(e?.error?.message ?? 'Error cargando datos'); this.loading.set(false); }
      });
    } else {
      this.api.getPendientesDirector(id).subscribe({
        next: (p) => { this.pendientes.set(p); this.loading.set(false); },
        error: (e) => { this.error.set(e?.error?.message ?? 'Error cargando pendientes'); this.loading.set(false); }
      });
    }
  }

  abrirModalSubir(doc: HabilitanteDto): void {
    this.docSeleccionado.set(doc);
    this.archivoUrl.set(null);
    this.archivoNombre.set(null);
    this.formSubir.reset({ porcentajeCoincidencia: null, umbralPermitido: 10.0 });
    this.ok.set(null);
    this.error.set(null);
    this.modalSubirOpen.set(true);
  }

  cerrarModalSubir(): void {
    this.modalSubirOpen.set(false);
    this.docSeleccionado.set(null);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;

    if (file.type !== 'application/pdf') { this.error.set('Solo se permiten archivos PDF.'); return; }
    if (file.size > 20 * 1024 * 1024)   { this.error.set('El archivo no debe superar 20 MB.'); return; }

    this.uploadingFile.set(true);
    this.error.set(null);

    this.api.uploadArchivo(file).subscribe({
      next: (res) => {
        this.archivoUrl.set(res.url);
        this.archivoNombre.set(file.name);
        this.uploadingFile.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Error subiendo archivo');
        this.uploadingFile.set(false);
      }
    });
  }

  confirmarSubida(): void {
    const doc    = this.docSeleccionado();
    const id     = this.idEntidad();
    const url    = this.archivoUrl();
    const nombre = this.archivoNombre();

    if (!doc || !id || !url) { this.error.set('Debe seleccionar y subir un archivo PDF primero.'); return; }
    if (!doc.idProyecto)     { this.error.set('El documento no tiene idProyecto.'); return; }

    const req: SubirHabilitanteRequest = {
      idProyecto:    doc.idProyecto,
      tipoDocumento: doc.tipoDocumento,
      urlArchivo:    url,
      nombreArchivo: nombre ?? 'documento.pdf'
    };

    if (doc.tipoDocumento === 'CERTIFICADO_ANTIPLAGIO') {
      const pct = this.formSubir.value.porcentajeCoincidencia;
      if (pct === null || pct === undefined || pct === '') {
        this.error.set('Ingrese el porcentaje de coincidencia del reporte COMPILATIO.');
        return;
      }
      req.porcentajeCoincidencia = Number(pct);
      req.umbralPermitido = Number(this.formSubir.value.umbralPermitido ?? 10);
    }

    this.loading.set(true);
    this.api.subirDocumento(id, req).subscribe({
      next: () => {
        this.loading.set(false);
        this.ok.set('Documento enviado correctamente.');
        this.cerrarModalSubir();
        this.cargarDatos();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Error al registrar el documento.');
      }
    });
  }

  abrirModalValidar(doc: HabilitanteDto): void {
    this.docSeleccionado.set(doc);
    this.formValidar.reset({ decision: 'APROBADO', comentario: '' });
    this.ok.set(null);
    this.error.set(null);
    this.modalValidarOpen.set(true);
  }

  cerrarModalValidar(): void {
    this.modalValidarOpen.set(false);
    this.docSeleccionado.set(null);
  }

  confirmarValidacion(): void {
    const doc = this.docSeleccionado();
    const id  = this.idEntidad();
    if (!doc?.id || !id) { this.error.set('No se encontró el documento o el usuario.'); return; }
    if (this.formValidar.invalid) { this.formValidar.markAllAsTouched(); return; }

    const req: ValidarHabilitanteRequest = {
      decision:   this.formValidar.value.decision,
      comentario: this.formValidar.value.comentario
    };

    this.loading.set(true);
    this.api.validarDocumento(id, doc.id, req).subscribe({
      next: () => {
        this.loading.set(false);
        this.ok.set(`Documento ${req.decision === 'APROBADO' ? 'aprobado' : 'rechazado'} correctamente.`);
        this.cerrarModalValidar();
        this.cargarDatos();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Error al validar.');
      }
    });
  }

  // Detecta documentos que requieren verificación de antiplagio (COMPILATIO)
  esAntiplagio(tipo: string): boolean {
    return tipo === 'CERTIFICADO_ANTIPLAGIO';
  }

// NUEVO — identifica el informe práctico del Complexivo
// Esto permite diferenciarlo del certificado antiplagio
// y asegura que NO se pida porcentaje COMPILATIO
  esInformePractico(tipo: string): boolean {
    return tipo === 'INFORME_PRACTICO_COMPLEXIVO';
  }

  badgeClass(estado: string): string {
    const map: Record<string, string> = {
      APROBADO: 'badge-aprobado', RECHAZADO: 'badge-rechazado',
      ENVIADO:  'badge-enviado',  PENDIENTE: 'badge-pendiente'
    };
    return map[estado] ?? 'badge-pendiente';
  }

  badgeLabel(estado: string): string {
    const map: Record<string, string> = {
      APROBADO: '✓ Aprobado', RECHAZADO: '✗ Rechazado',
      ENVIADO:  '⏳ En revisión', PENDIENTE: '○ Pendiente'
    };
    return map[estado] ?? '○ Pendiente';
  }

  puedeSubir(doc: HabilitanteDto): boolean {
    return doc.estado === 'PENDIENTE' || doc.estado === 'RECHAZADO';
  }

  puedeValidar(doc: HabilitanteDto): boolean { return doc.estado === 'ENVIADO'; }

  trackByTipo(_: number, d: HabilitanteDto): string { return d.tipoDocumento; }
}
