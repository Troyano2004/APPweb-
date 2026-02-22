import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DocumentoTitulacionService, DocumentoTitulacionDto } from '../../../services/documento-titulacion';
import { RevisionDirectorService, ObservacionDto } from '../../../services/revision-director';
import { getSessionEntityId, getSessionUser } from '../../../services/session';
import { RichTextEditorComponent } from './rich-text-editor.component';

type TabKey =
  | 'portada'
  | 'resumen'
  | 'introduccion'
  | 'marco_metodo'
  | 'resultados'
  | 'conclusiones'
  | 'biblio_anexos';

@Component({
  selector: 'app-documento',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RichTextEditorComponent],
  templateUrl: './documento.html',
  styleUrl: './documento.scss'
})
export class Documento implements OnInit {
  private readonly idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');

  loading = signal(false);
  error = signal<string | null>(null);

  documento = signal<DocumentoTitulacionDto | null>(null);
  observaciones = signal<ObservacionDto[]>([]);

  activeTab = signal<TabKey>('portada');

  estado = computed(() => this.documento()?.estado ?? 'BORRADOR');

  puedeEditar = computed(() => {
    const e = this.estado();
    return e === 'BORRADOR' || e === 'CORRECCION_REQUERIDA';
  });

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private api: DocumentoTitulacionService,
    private revisionApi: RevisionDirectorService
  ) {
    this.form = this.fb.group({
      titulo: [''],
      resumen: [''],
      abstractText: [''],

      introduccion: [''],
      problema: [''],
      objetivosGenerales: [''],
      objetivosEspecificos: [''],
      justificacion: [''],

      marcoTeorico: [''],
      metodologia: [''],

      resultados: [''],
      discusion: [''],

      conclusiones: [''],
      recomendaciones: [''],

      bibliografia: [''],
      anexos: ['']
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.getDocumento(this.idEstudiante).subscribe({
      next: (doc) => {
        this.documento.set(doc);

        this.form.patchValue({
          titulo: doc.titulo ?? '',
          resumen: doc.resumen ?? '',
          abstractText: doc.abstractText ?? '',

          introduccion: doc.introduccion ?? '',
          problema: doc.planteamientoProblema ?? doc.problema ?? '',
          objetivosGenerales: doc.objetivoGeneral ?? doc.objetivosGenerales ?? '',
          objetivosEspecificos: doc.objetivosEspecificos ?? '',
          justificacion: doc.justificacion ?? '',

          marcoTeorico: doc.marcoTeorico ?? '',
          metodologia: doc.metodologia ?? '',

          resultados: doc.resultados ?? '',
          discusion: doc.discusion ?? '',

          conclusiones: doc.conclusiones ?? '',
          recomendaciones: doc.recomendaciones ?? '',

          bibliografia: doc.bibliografia ?? '',
          anexos: doc.anexos ?? ''
        });

        if (this.puedeEditar()) this.form.enable();
        else this.form.disable();

        this.cargarObservaciones(doc);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando documento');
      }
    });
  }

  private cargarObservaciones(doc: DocumentoTitulacionDto | null): void {
    const idDocumento = Number(doc?.id ?? doc?.idDocumento ?? 0);
    if (!idDocumento) {
      this.observaciones.set([]);
      return;
    }

    this.revisionApi.observaciones(idDocumento).subscribe({
      next: (list) => this.observaciones.set(list ?? []),
      error: () => this.observaciones.set([])
    });
  }

  guardar(): void {
    if (!this.puedeEditar()) return;
    if (!this.idEstudiante) return;

    this.loading.set(true);
    this.error.set(null);

    const raw = this.form.getRawValue();
    const payload = {
      titulo: raw.titulo,
      resumen: raw.resumen,
      abstractText: raw.abstractText,
      introduccion: raw.introduccion,
      planteamientoProblema: raw.problema,
      objetivoGeneral: raw.objetivosGenerales,
      objetivosEspecificos: raw.objetivosEspecificos,
      justificacion: raw.justificacion,
      marcoTeorico: raw.marcoTeorico,
      metodologia: raw.metodologia,
      resultados: raw.resultados,
      discusion: raw.discusion,
      conclusiones: raw.conclusiones,
      recomendaciones: raw.recomendaciones,
      bibliografia: raw.bibliografia,
      anexos: raw.anexos
    };

    this.api.updateDocumento(this.idEstudiante, payload).subscribe({
      next: (doc) => {
        this.documento.set(doc);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error guardando');
      }
    });
  }

  enviarRevision(): void {
    if (!this.puedeEditar()) return;
    if (!this.idEstudiante) return;

    this.loading.set(true);
    this.error.set(null);

    this.api.enviarRevision(this.idEstudiante).subscribe({
      next: (doc) => {
        this.documento.set(doc);
        this.form.disable();
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error enviando a revisión');
      }
    });
  }

  setTab(tab: TabKey): void {
    this.activeTab.set(tab);
  }
}
