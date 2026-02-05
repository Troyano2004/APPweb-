import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DocumentoTitulacionService, DocumentoTitulacionDto } from '../../../services/documento-titulacion';

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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './documento.html',
  styleUrl: './documento.scss'
})
export class Documento implements OnInit {
  private readonly idEstudiante = 1;

  // ✅ señales para el HTML
  loading = signal(false);
  error = signal<string | null>(null);

  documento = signal<DocumentoTitulacionDto | null>(null);

  // ✅ en tu HTML estás usando activeTab() y setTab('x')
  activeTab = signal<TabKey>('portada');

  // ✅ estado() en el HTML
  estado = computed(() => this.documento()?.estado ?? 'BORRADOR');

  // ✅ puedeEditar() en el HTML
  puedeEditar = computed(() => {
    const e = this.estado();
    return e === 'BORRADOR' || e === 'CORRECCION_REQUERIDA';
  });

  // ✅ form en constructor para evitar error fb
  form!: FormGroup;

  constructor(private fb: FormBuilder, private api: DocumentoTitulacionService) {
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

  // ✅ tu HTML llama cargar()
  cargar(): void {
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
          problema: doc.problema ?? '',
          objetivosGenerales: doc.objetivosGenerales ?? '',
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

        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando documento');
      }
    });
  }

  // ✅ tu HTML llama guardar()
  guardar(): void {
    if (!this.puedeEditar()) return;

    this.loading.set(true);
    this.error.set(null);

    this.api.updateDocumento(this.idEstudiante, this.form.getRawValue()).subscribe({
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

  // ✅ tu HTML llama enviarRevision()
  enviarRevision(): void {
    if (!this.puedeEditar()) return;

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

  // ✅ tu HTML llama setTab('...')
  setTab(tab: TabKey): void {
    this.activeTab.set(tab);
  }
}
