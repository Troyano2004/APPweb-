import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  DocumentoHabilitanteService,
  HabilitanteDto,
  ResumenHabilitacionDto,
  SubirHabilitanteRequest,
  SubirAntiplagioPorDirectorRequest,
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

  readonly rol       = signal<RolView>('estudiante');
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

  // Modal antiplagio — exclusivo del Director (Art. 57 num.2)
  modalAntiplagio     = signal(false);
  antiplagioPct       = signal<number | null>(null);
  antiplagioUrl       = signal<string | null>(null);
  antiplagioNombre    = signal<string | null>(null);
  antiplagioProyecto  = signal<number | null>(null);
  uploadingAntiplagio = signal(false);

  // indica si el estudiante es de modalidad Complexivo
  esComplexivo = false;

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
    this.buildForms();
    this.detectarRol();
  }

  // ── Detectar rol y modalidad ────────────────────────────────
  private detectarRol(): void {
    const user   = getSessionUser();
    const rolRaw = String(user?.['rol'] ?? '')
      .replace('ROLE_', '').trim().toUpperCase();

    if (rolRaw === 'ESTUDIANTE') {
      this.rol.set('estudiante');
      const idEst = getSessionEntityId(user, 'estudiante');
      this.idEntidad.set(idEst);

      if (!idEst) {
        this.error.set('No se identificó al estudiante.');
        return;
      }

      // Detectar si es complexivo consultando el endpoint
      this.api.getResumenComplexivo(idEst).subscribe({
        next: () => {
          this.esComplexivo = true;
          this.cargarDatos();
        },
        error: () => {
          this.esComplexivo = false;
          this.cargarDatos();
        }
      });

    } else if (rolRaw === 'DOCENTE' || rolRaw === 'DOCENTE_TITULADO') {
      this.rol.set('director');
      this.idEntidad.set(getSessionEntityId(user, 'docente'));
      this.cargarDatos();
    } else {
      this.rol.set('coordinador');
      this.idEntidad.set(getSessionEntityId(user, 'docente'));
      this.cargarDatos();
    }
  }

  private buildForms(): void {
    // Estudiante: formulario vacío, no hay campos extra
    this.formSubir = this.fb.group({});

    // Director: validar otros documentos habilitantes
    // porcentajeCoincidencia se eliminó porque el antiplagio
    // ya tiene su propio modal independiente (abrirModalAntiplagio)
    this.formValidar = this.fb.group({
      decision:   ['APROBADO', Validators.required],
      comentario: ['']
    });
  }

  // ── Cargar datos ────────────────────────────────────────────
  cargarDatos(): void {
    const id = this.idEntidad();
    if (!id) { this.error.set('No se identificó al usuario.'); return; }

    this.loading.set(true);
    this.error.set(null);

    if (this.esEstudiante()) {
      const resumen$ = this.esComplexivo
        ? this.api.getResumenComplexivo(id)
        : this.api.getResumenEstudiante(id);

      resumen$.subscribe({
        next:  (r) => { this.resumen.set(r); this.loading.set(false); },
        error: (e) => {
          this.error.set(e?.error?.message ?? 'Error cargando datos');
          this.loading.set(false);
        }
      });

    } else {
      this.api.getPendientesDirector(id).subscribe({
        next:  (p) => { this.pendientes.set(p); this.loading.set(false); },
        error: (e) => {
          this.error.set(e?.error?.message ?? 'Error cargando pendientes');
          this.loading.set(false);
        }
      });
    }
  }

  // ── Modal subir (Estudiante) ────────────────────────────────
  abrirModalSubir(doc: HabilitanteDto): void {
    this.docSeleccionado.set(doc);
    this.archivoUrl.set(null);
    this.archivoNombre.set(null);
    this.formSubir.reset();
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

    if (file.type !== 'application/pdf') {
      this.error.set('Solo se permiten archivos PDF.');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      this.error.set('El archivo no debe superar 20 MB.');
      return;
    }

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

    if (!doc || !id || !url) {
      this.error.set('Debe seleccionar y subir un archivo PDF primero.');
      return;
    }

    const req: SubirHabilitanteRequest = {
      idProyecto:    doc.idProyecto!,
      tipoDocumento: doc.tipoDocumento,
      urlArchivo:    url,
      nombreArchivo: nombre ?? 'documento.pdf'
    };

    this.loading.set(true);

    const subir$ = this.esComplexivo
      ? this.api.subirDocumentoComplexivo(id, req)
      : this.api.subirDocumento(id, req);

    subir$.subscribe({
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

  // ── Modal validar (Director — documentos que NO son antiplagio) ──
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

    if (!doc?.id || !id) {
      this.error.set('No se encontró el documento o el usuario.');
      return;
    }
    if (this.formValidar.invalid) {
      this.formValidar.markAllAsTouched();
      return;
    }

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

  // ── Modal antiplagio (Director — Art. 57 num.2) ─────────────
  // El Director corre COMPILATIO, emite el certificado firmado
  // y lo registra aquí directamente. El estudiante no interviene.
  abrirModalAntiplagio(idProyecto: number): void {
    this.antiplagioProyecto.set(idProyecto);
    this.antiplagioPct.set(null);
    this.antiplagioUrl.set(null);
    this.antiplagioNombre.set(null);
    this.ok.set(null);
    this.error.set(null);
    this.modalAntiplagio.set(true);
  }

  cerrarModalAntiplagio(): void {
    this.modalAntiplagio.set(false);
    this.antiplagioProyecto.set(null);
  }

  onFileAntiplagio(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.error.set('Solo se permiten archivos PDF.');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      this.error.set('El archivo no debe superar 20 MB.');
      return;
    }

    this.uploadingAntiplagio.set(true);
    this.error.set(null);
    this.api.uploadArchivo(file).subscribe({
      next: (res) => {
        this.antiplagioUrl.set(res.url);
        this.antiplagioNombre.set(file.name);
        this.uploadingAntiplagio.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Error subiendo archivo');
        this.uploadingAntiplagio.set(false);
      }
    });
  }

  confirmarAntiplagio(): void {
    const id     = this.idEntidad();
    const idProy = this.antiplagioProyecto();
    const url    = this.antiplagioUrl();
    const nombre = this.antiplagioNombre();
    const pct    = this.antiplagioPct();

    if (!id || !idProy || !url) {
      this.error.set('Debe subir el PDF del certificado primero.');
      return;
    }
    if (pct === null || pct === undefined) {
      this.error.set('Debe ingresar el porcentaje de coincidencia.');
      return;
    }

    const req: SubirAntiplagioPorDirectorRequest = {
      urlArchivo:             url,
      nombreArchivo:          nombre ?? 'certificado_antiplagio.pdf',
      porcentajeCoincidencia: Number(pct)
    };

    this.loading.set(true);
    this.api.subirCertificadoAntiplagio(id, idProy, req).subscribe({
      next: () => {
        this.loading.set(false);
        this.ok.set('Certificado antiplagio registrado correctamente.');
        this.cerrarModalAntiplagio();
        this.cargarDatos();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Error al registrar el certificado.');
      }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────
  esAntiplagio(tipo: string): boolean { return tipo === 'CERTIFICADO_ANTIPLAGIO'; }

  badgeClass(estado: string): string {
    const map: Record<string, string> = {
      APROBADO:  'badge-aprobado',
      RECHAZADO: 'badge-rechazado',
      ENVIADO:   'badge-enviado',
      PENDIENTE: 'badge-pendiente'
    };
    return map[estado] ?? 'badge-pendiente';
  }

  badgeLabel(estado: string): string {
    const map: Record<string, string> = {
      APROBADO:  '✓ Aprobado',
      RECHAZADO: '✗ Rechazado',
      ENVIADO:   '⏳ En revisión',
      PENDIENTE: '○ Pendiente'
    };
    return map[estado] ?? '○ Pendiente';
  }

  puedeSubir(doc: HabilitanteDto): boolean {
    return doc.estado === 'PENDIENTE' || doc.estado === 'RECHAZADO';
  }

  puedeValidar(doc: HabilitanteDto): boolean { return doc.estado === 'ENVIADO'; }

  trackByTipo(_: number, d: HabilitanteDto): string { return d.tipoDocumento; }
}
