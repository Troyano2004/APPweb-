import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DocumentoTitulacionService, DocumentoTitulacionDto } from '../../../services/documento-titulacion';

import { RevisionDirectorService, ObservacionDto } from '../../../services/revision-director';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

interface DocumentoSeccion {
  titulo: string;
  contenido: string | null | undefined;
}

@Component({
  selector: 'app-revision-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './revision-detalle.html',
  styleUrl: './revision-detalle.scss'
})
export class RevisionDetalle implements OnInit {
  idDocente = getSessionEntityId(getSessionUser(), 'docente');

  idDocumento = 0;
  documento = signal<DocumentoTitulacionDto | null>(null);

  titulo = signal<string>('');

  loading = signal(false);
  error = signal<string | null>(null);
  observaciones = signal<ObservacionDto[]>([]);

  // form simple (sin reactive forms para que sea rápido)
  seccion = 'METODOLOGIA';
  comentario = '';

  constructor(
    private route: ActivatedRoute,
    private api: RevisionDirectorService,
    private docApi: DocumentoTitulacionService
  ) {}

  ngOnInit(): void {
    const idDocParam = Number(this.route.snapshot.paramMap.get('idDocumento'));

    this.idDocumento = Number.isFinite(idDocParam) && idDocParam > 0 ? idDocParam : 0;

    if (!this.idDocumento) {
      this.error.set('No se envió idDocumento en la URL.');
      return;
    }

    const t = this.route.snapshot.queryParamMap.get('titulo');
    if (t) this.titulo.set(t);

    this.cargarDocumento();
    this.cargarObs();
  }

  cargarObs(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.observaciones(this.idDocumento).subscribe({
      next: (list) => {
        this.observaciones.set(list ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando observaciones');
      }
    });
  }
  cargarDocumento(): void {
    this.loading.set(true);
    this.error.set(null);

    this.docApi.getDocumentoPorId(this.idDocumento).subscribe({
      next: (doc) => {
        this.documento.set(doc);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando documento del estudiante');
      }
    });
  }

  agregar(): void {
    if (!this.comentario.trim() || !this.idDocente) return;

    this.loading.set(true);
    this.error.set(null);

    this.api.agregarObservacion(this.idDocente, this.idDocumento, {
      seccion: this.seccion,
      comentario: this.comentario,
      idAutor: this.idDocente
    }).subscribe({
      next: () => {
        this.comentario = '';
        this.cargarObs();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error agregando observación');
      }
    });
  }

  devolver(): void {
    if (!this.idDocente) return;
    this.loading.set(true);
    this.error.set(null);

    this.api.devolver(this.idDocente, this.idDocumento).subscribe({
      next: () => this.cargarObs(),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error devolviendo documento');
      }
    });
  }

  aprobar(): void {
    if (!this.idDocente) return;
    this.loading.set(true);
    this.error.set(null);

    this.api.aprobar(this.idDocente, this.idDocumento).subscribe({
      next: () => this.cargarObs(),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error aprobando documento');
      }
    });
  }



  exportarPdf(): void {
    const doc = this.documento();
    if (!doc) {
      this.error.set('No hay documento cargado para exportar.');
      return;
    }

    const titulo = this.escapeHtml(doc.titulo || this.titulo() || `Documento #${this.idDocumento}`);
    const secciones = this.obtenerSeccionesDocumento(doc)
      .filter((seccion) => !!(seccion.contenido || '').trim())
      .map((seccion) => `
        <section class="doc-section">
          <h2>${this.escapeHtml(seccion.titulo)}</h2>
          <p>${this.escapeHtml(seccion.contenido || '').replace(/\n/g, '<br/>')}</p>
        </section>
      `)
      .join('');

    const fecha = new Date().toLocaleDateString('es-EC', { year: 'numeric', month: 'long', day: 'numeric' });

    const html = `
      <!doctype html>
      <html lang="es">
      <head>
        <meta charset="utf-8" />
        <title>${titulo}</title>
        <style>
          @page { margin: 2.5cm 2.2cm; }
          body { font-family: 'Times New Roman', Times, serif; font-size: 12pt; line-height: 1.6; color: #111; }
          h1 { font-size: 16pt; text-align: center; margin: 0 0 0.8rem; }
          .meta { text-align: center; font-size: 11pt; margin-bottom: 1.4rem; }
          h2 { font-size: 13pt; text-transform: uppercase; margin: 1.2rem 0 0.5rem; border-bottom: 1px solid #999; padding-bottom: 0.2rem; }
          p { margin: 0; text-align: justify; }
          .doc-section { break-inside: avoid; page-break-inside: avoid; margin-bottom: 0.5rem; }
        </style>
      </head>
      <body>
        <h1>${titulo}</h1>
        <div class="meta">Documento de revisión · ${this.escapeHtml(fecha)}</div>
        ${secciones || '<p>No hay contenido disponible para exportar.</p>'}
      </body>
      </html>
    `;

    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    document.body.appendChild(iframe);

    const frameDoc = iframe.contentDocument || iframe.contentWindow?.document;
    if (!frameDoc) {
      this.error.set('No se pudo preparar la exportación a PDF.');
      document.body.removeChild(iframe);
      return;
    }

    frameDoc.open();
    frameDoc.write(html);
    frameDoc.close();

    const frameWindow = iframe.contentWindow;
    setTimeout(() => {
      frameWindow?.focus();
      frameWindow?.print();
      setTimeout(() => document.body.removeChild(iframe), 500);
    }, 250);
  }

  private obtenerSeccionesDocumento(doc: DocumentoTitulacionDto): DocumentoSeccion[] {
    return [
      { titulo: 'Resumen', contenido: doc.resumen },
      { titulo: 'Abstract', contenido: doc.abstractText },
      { titulo: 'Introducción', contenido: doc.introduccion },
      { titulo: 'Planteamiento del problema', contenido: doc.planteamientoProblema || doc.problema },
      { titulo: 'Objetivo general', contenido: doc.objetivoGeneral || doc.objetivosGenerales },
      { titulo: 'Objetivos específicos', contenido: doc.objetivosEspecificos },
      { titulo: 'Justificación', contenido: doc.justificacion },
      { titulo: 'Marco teórico', contenido: doc.marcoTeorico },
      { titulo: 'Metodología', contenido: doc.metodologia },
      { titulo: 'Resultados', contenido: doc.resultados },
      { titulo: 'Discusión', contenido: doc.discusion },
      { titulo: 'Conclusiones', contenido: doc.conclusiones },
      { titulo: 'Recomendaciones', contenido: doc.recomendaciones },
      { titulo: 'Bibliografía', contenido: doc.bibliografia },
      { titulo: 'Anexos', contenido: doc.anexos }
    ];
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

}
