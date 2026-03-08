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
  aiResult = signal<string | null>(null);

  // ✅ NUEVO: Asistente IA con modos
  showAiAssistant = signal(false);
  selectedAiMode = signal('evaluacion-integral');
  aiLastModeLabel = signal<string | null>(null);
  aiPromptSugerido = signal<string | null>(null);
  customAiPrompt = '';
  private aiPromptFinal = '';

  seccion = 'METODOLOGIA';
  comentario = '';

  // ✅ NUEVO: Modos de análisis IA disponibles
  readonly aiModes = [
    {
      id: 'evaluacion-integral',
      titulo: 'Evaluación integral',
      descripcion: 'Revisión global con fortalezas, riesgos y recomendaciones accionables.',
      prompt:
        'Analiza el documento de forma integral como evaluador académico. Resume fortalezas, puntos críticos y recomienda mejoras priorizadas por impacto.'
    },
    {
      id: 'estilo-academico',
      titulo: 'Redacción y estilo académico',
      descripcion: 'Enfocado en claridad, cohesión, tono formal y calidad del lenguaje.',
      prompt:
        'Evalúa redacción académica, coherencia, gramática y estilo. Sugiere cambios concretos para elevar la calidad del texto.'
    },
    {
      id: 'metodologia-rigor',
      titulo: 'Metodología y rigor',
      descripcion: 'Detecta debilidades metodológicas y propone cómo robustecerlas.',
      prompt:
        'Revisa con enfoque metodológico: pertinencia del enfoque, variables, técnicas, validez y limitaciones. Propón mejoras medibles.'
    },
    {
      id: 'innovacion-impacto',
      titulo: 'Innovación e impacto',
      descripcion: 'Valora originalidad, aporte y potencial de aplicación real.',
      prompt:
        'Examina nivel de innovación, factibilidad e impacto social/académico. Sugiere ideas creativas para diferenciar el proyecto.'
    }
  ] as const;

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
    if (t) this.titulo.set(this.toPlainText(t));

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
        this.aiResult.set(doc?.feedbackIa?.trim() || null);
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

    this.api
      .agregarObservacion(this.idDocente, this.idDocumento, {
        seccion: this.seccion,
        comentario: this.comentario,
        idAutor: this.idDocente
      })
      .subscribe({
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

  // ✅ NUEVO: Abrir/cerrar panel asistente IA
  abrirAsistenteIa(): void {
    this.showAiAssistant.set(true);
  }

  cerrarAsistenteIa(): void {
    this.showAiAssistant.set(false);
  }

  // ✅ NUEVO: Lanzar análisis desde el formulario del asistente
  analizarConIaDesdeFormulario(): void {
    const modo = this.aiModes.find((item) => item.id === this.selectedAiMode()) ?? this.aiModes[0];
    const promptFinal = (this.customAiPrompt || '').trim() || modo.prompt;

    this.aiLastModeLabel.set(modo.titulo);
    this.aiPromptSugerido.set(promptFinal);
    this.aiPromptFinal = promptFinal;
    this.revisarConIa(modo.id);
  }

  // ✅ ACTUALIZADO: revisarConIa ahora acepta modo y prompt personalizado
  revisarConIa(modoSeleccionado?: string): void {
    if (!this.idDocumento) return;
    this.loading.set(true);
    this.error.set(null);

    this.api.revisarConIa(this.idDocumento, {
      modo: modoSeleccionado || this.selectedAiMode(),
      promptPersonalizado: this.aiPromptFinal || undefined
    }).subscribe({
      next: (doc) => {
        this.documento.set(doc);
        this.aiResult.set(doc?.feedbackIa?.trim() || 'La IA no devolvió observaciones para este documento.');
        this.customAiPrompt = '';
        this.cerrarAsistenteIa();
        this.cargarObs();
      },
      error: (err) => {
        this.loading.set(false);
        this.aiResult.set(null);
        this.error.set(err?.error?.message ?? 'Error revisando documento con IA');
      }
    });
  }

  renderContenido(contenido: string | null | undefined): string {
    const value = (contenido ?? '').trim();
    if (!value) return '';

    if (this.pareceHtml(value)) {
      return this.sanitizeRichHtml(value);
    }

    return this.escapeHtml(value).replace(/\n/g, '<br/>');
  }

  exportarPdf(): void {
    const doc = this.documento();
    if (!doc) {
      this.error.set('No hay documento cargado para exportar.');
      return;
    }

    const tituloDocumento = this.toPlainText(doc.titulo || this.titulo() || `Documento #${this.idDocumento}`);
    const idEstudiante = this.route.snapshot.queryParamMap.get('idEstudiante') || 'No registrado';
    const fechaLarga = new Date().toLocaleDateString('es-EC', { year: 'numeric', month: 'long', day: 'numeric' });
    const anio = new Date().getFullYear();

    const secciones = this.obtenerSeccionesDocumento(doc)
      .filter((seccion) => !!(seccion.contenido || '').trim())
      .map((seccion) => {
        const contenido = this.renderContenido(seccion.contenido);
        return `
          <section class="doc-section">
            <h2>${this.escapeHtml(seccion.titulo)}</h2>
            <div class="doc-body">${contenido}</div>
          </section>
        `;
      })
      .join('');

    const logoUteqUrl = `${window.location.origin}/assets/img/logo.png`;

    const html = `
      <!doctype html>
      <html lang="es">
      <head>
        <meta charset="utf-8" />
        <title>${this.escapeHtml(tituloDocumento)}</title>
        <style>
          @page { margin: 2.2cm 2cm; }
          * { box-sizing: border-box; }
          body {
            font-family: 'Times New Roman', Times, serif;
            color: #121212;
            font-size: 12pt;
            line-height: 1.6;
            margin: 0;
          }
          .cover-page {
            min-height: calc(100vh - 4.4cm);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-between;
            text-align: center;
            page-break-after: always;
            padding: 0.3cm 0 0.8cm;
          }
          .cover-logo { margin-bottom: 0.3cm; }
          .cover-logo img { width: 110px; height: 110px; object-fit: contain; }
          .cover-university {
            font-size: 18pt;
            font-weight: 700;
            letter-spacing: 0.02em;
            text-transform: uppercase;
            margin: 0;
          }
          .cover-faculty {
            margin: 0.15cm 0 0;
            font-size: 12.2pt;
          }
          .cover-career {
            margin: 0.05cm 0 0;
            font-size: 11.5pt;
          }
          .cover-divider {
            width: 100%;
            border: 0;
            border-top: 1.5px solid #4f4f4f;
            margin: 0.9cm 0;
          }
          .cover-title {
            margin: 0;
            font-size: 20pt;
            font-weight: 700;
            text-transform: uppercase;
          }
          .cover-subtitle {
            margin: 0.28cm 0 0;
            font-size: 13pt;
          }
          .cover-box {
            text-align: left;
            width: 100%;
            max-width: 14cm;
            font-size: 12pt;
            line-height: 1.55;
          }
          .cover-box strong { font-weight: 700; }
          .cover-footer {
            font-size: 12pt;
            text-align: center;
          }
          .doc-main {
            position: relative;
            padding-bottom: 2cm;
          }
          .doc-header {
            text-align: center;
            border-bottom: 2px solid #1d7f43;
            margin-bottom: 0.9rem;
            padding-bottom: 0.5rem;
          }
          .doc-header h1 {
            margin: 0;
            font-size: 15.5pt;
            text-transform: uppercase;
            color: #14532d;
          }
          .doc-meta {
            margin-top: 0.22rem;
            font-size: 10.8pt;
            color: #303030;
          }
          .doc-section {
            break-inside: avoid;
            page-break-inside: avoid;
            margin-bottom: 0.85rem;
          }
          h2 {
            font-size: 12.5pt;
            text-transform: uppercase;
            color: #14532d;
            margin: 1rem 0 0.45rem;
            border-bottom: 1px solid #8ea894;
            padding-bottom: 0.15rem;
          }
          .doc-body { text-align: justify; }
          .doc-body p { margin: 0 0 0.48rem; }
          .doc-body table { width: 100%; border-collapse: collapse; margin: 0.55rem 0; }
          .doc-body td, .doc-body th { border: 1px solid #8b8b8b; padding: 0.25rem; }
          .doc-body img { max-width: 100%; height: auto; }
          .pdf-footer {
            position: fixed;
            bottom: 0.6cm;
            left: 0;
            right: 0;
            text-align: center;
            font-size: 9.7pt;
            color: #4a4a4a;
            border-top: 1px solid #bdbdbd;
            padding-top: 0.2cm;
          }
          .pdf-footer .page::after {
            content: counter(page);
          }
        </style>
      </head>
      <body>
        <section class="cover-page">
          <div style="width: 100%;">
            <div class="cover-logo"><img src="${logoUteqUrl}" alt="Logo UTEQ" /></div>
            <p class="cover-university">UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO</p>
            <p class="cover-faculty">Facultad de Ciencias de la Computación y Diseño Digital</p>
            <p class="cover-career">Ingeniería en Software</p>
            <hr class="cover-divider" />
            <h1 class="cover-title">INFORME DE REVISIÓN</h1>
            <p class="cover-subtitle">Documento de titulación para evaluación académica</p>
            <hr class="cover-divider" />
            <div class="cover-box">
              <div><strong>Título del proyecto:</strong> ${this.escapeHtml(tituloDocumento)}</div>
              <div><strong>Documento:</strong> #${this.idDocumento}</div>
              <div><strong>Estudiante (ID):</strong> ${this.escapeHtml(idEstudiante)}</div>
              <div><strong>Fecha de emisión:</strong> ${this.escapeHtml(fechaLarga)}</div>
            </div>
          </div>
          <div class="cover-footer">
            Quevedo - Ecuador<br/>
            ${anio}
          </div>
        </section>

        <main class="doc-main">
          <header class="doc-header">
            <h1>${this.escapeHtml(tituloDocumento)}</h1>
            <div class="doc-meta">Documento de revisión · ${this.escapeHtml(fechaLarga)}</div>
          </header>
          ${secciones || '<p>No hay contenido disponible para exportar.</p>'}
        </main>

        <footer class="pdf-footer">
          Universidad Técnica Estatal de Quevedo · Sistema de Titulación · Página <span class="page"></span>
        </footer>
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

  private sanitizeRichHtml(rawHtml: string): string {
    const parser = new DOMParser();
    const doc = parser.parseFromString(rawHtml, 'text/html');

    doc.querySelectorAll('script,style,iframe,object,embed').forEach((el) => el.remove());

    doc.querySelectorAll('*').forEach((el) => {
      [...el.attributes].forEach((attr) => {
        const name = attr.name.toLowerCase();
        const value = attr.value;

        if (name.startsWith('on')) {
          el.removeAttribute(attr.name);
          return;
        }

        if ((name === 'href' || name === 'src') && /^javascript:/i.test(value.trim())) {
          el.removeAttribute(attr.name);
          return;
        }

        if (name === 'style') {
          el.removeAttribute('style');
        }
      });
    });

    return doc.body.innerHTML;
  }

  private pareceHtml(text: string): boolean {
    return /<[^>]+>/.test(text);
  }

  private toPlainText(value: string): string {
    return (value || '')
      .replace(/<[^>]*>/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
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
