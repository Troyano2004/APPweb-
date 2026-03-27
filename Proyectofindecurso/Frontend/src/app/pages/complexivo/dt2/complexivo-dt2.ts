
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  EstudianteDeDocenteDto,
  ComplexivoInformeDto,
  ComplexivoAsesoriaDto
} from '../../../services/complexivo.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

type TabInforme = 'informe' | 'asesorias';

@Component({
  selector: 'app-complexivo-dt2',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexivo-dt2.html',
  styleUrls: ['./complexivo-dt2.scss']
})
export class ComplexivoDt2Component implements OnInit {

  private idDocente = 0;

  loading   = signal(false);
  error     = signal<string | null>(null);
  ok        = signal<string | null>(null);

  estudiantes        = signal<EstudianteDeDocenteDto[]>([]);
  estudSeleccionado  = signal<EstudianteDeDocenteDto | null>(null);
  informe            = signal<ComplexivoInformeDto | null>(null);
  asesorias          = signal<ComplexivoAsesoriaDto[]>([]);
  tabInforme         = signal<TabInforme>('informe');
  obsAprobar         = '';
  obsRechazar        = '';
  obsAsesoria        = '';
  mostrarFormRechazo = false;

  constructor(private api: ComplexivoService) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = getSessionEntityId(user, 'docente')
      ?? Number(user?.['idUsuario'] ?? user?.['id_usuario'] ?? 0);
    this.cargarEstudiantes();
  }

  seleccionar(est: EstudianteDeDocenteDto): void {
    this.estudSeleccionado.set(est);
    this.informe.set(null); this.asesorias.set([]);
    this.error.set(null); this.ok.set(null);
    this.obsAprobar = ''; this.obsRechazar = '';
    this.obsAsesoria = ''; this.mostrarFormRechazo = false;
    this.tabInforme.set('informe');
    this.cargarInforme(est.idComplexivo);
    this.cargarAsesorias(est.idComplexivo);
  }

  setTabInforme(t: TabInforme): void {
    this.tabInforme.set(t); this.error.set(null); this.ok.set(null);
  }

  aprobarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme) return;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.aprobarInformeDt2(this.idDocente, inf.idInforme, this.obsAprobar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('✅ Informe aprobado correctamente.');
          this.obsAprobar = '';
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme || !this.obsRechazar.trim()) {
      this.error.set('Escribe el motivo del rechazo.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.rechazarInformeDt2(this.idDocente, inf.idInforme, this.obsRechazar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe rechazado. El estudiante será notificado.');
          this.obsRechazar = ''; this.mostrarFormRechazo = false;
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  registrarAsesoria(): void {
    const est = this.estudSeleccionado();
    if (!est || !this.obsAsesoria.trim()) {
      this.error.set('Escribe las observaciones de la sesión.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.registrarAsesoriaDt2(this.idDocente, est.idComplexivo, this.obsAsesoria)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Asesoría registrada.');
          this.obsAsesoria = '';
          this.cargarAsesorias(est.idComplexivo);
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error.')
      });
  }

  // ── PDF profesional del informe ────────────────────────────────
  verPdfInforme(): void {
    const inf = this.informe();
    const est = this.estudSeleccionado();
    if (!inf || !est) return;

    const seccion = (num: string, titulo: string, contenido: string | null) =>
      contenido && contenido.trim()
        ? `<div class="seccion">
             <div class="seccion-titulo">${num}. ${titulo}</div>
             <div class="seccion-texto">${contenido.replace(/\n/g, '<br>')}</div>
           </div>`
        : '';

    const badgeColor = inf.estado === 'APROBADO'
      ? '#d1fae5; color:#065f46; border:1px solid #10b981'
      : inf.estado === 'RECHAZADO'
        ? '#fee2e2; color:#991b1b; border:1px solid #ef4444'
        : '#fef3c7; color:#92400e; border:1px solid #f59e0b';

    const html = `
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Informe Práctico — ${est.nombreEstudiante}</title>
      <style>
        * { margin:0; padding:0; box-sizing:border-box; }
        body { font-family:'Times New Roman',serif; color:#1a1a1a; background:#fff; font-size:12pt; }

        /* ── PORTADA ── */
        .portada {
          display:flex; flex-direction:column; align-items:center;
          justify-content:center; min-height:100vh;
          padding:60px 80px; text-align:center;
          background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
          page-break-after:always;
        }
        .escudo { font-size:56px; margin-bottom:16px; }
        .uni-nombre { font-size:15pt; font-weight:bold; margin-bottom:4px; }
        .uni-facultad { font-size:11pt; color:#555; margin-bottom:6px; }
        .uni-carrera  { font-size:10pt; color:#777; margin-bottom:40px; }
        .linea-verde { width:80px; height:4px; background:#0f7a3a; margin:0 auto 32px; border-radius:2px; }
        .doc-tipo {
          font-size:9pt; letter-spacing:.2em; text-transform:uppercase;
          font-weight:700; color:#0f7a3a; margin-bottom:24px;
          border:1.5px solid #0f7a3a; padding:6px 20px; border-radius:20px;
          display:inline-block;
        }
        .doc-titulo {
          font-size:16pt; font-weight:bold; line-height:1.4;
          max-width:480px; margin:0 auto 48px; color:#0f172a;
        }
        .info-grid {
          width:100%; max-width:440px; margin:0 auto;
          border-collapse:collapse;
        }
        .info-grid td {
          padding:8px 14px; font-size:10pt; border-bottom:1px solid #e2e8f0;
          text-align:left;
        }
        .info-grid .lbl { font-weight:bold; color:#64748b; width:160px; }
        .estado-badge {
          display:inline-block; margin-top:32px;
          padding:6px 24px; border-radius:20px;
          font-size:9pt; font-weight:bold; letter-spacing:.08em;
          text-transform:uppercase;
          background:${badgeColor};
        }

        /* ── CONTENIDO ── */
        .contenido { padding:48px 64px; }
        .seccion { margin-bottom:28px; page-break-inside:avoid; }
        .seccion-titulo {
          font-size:11pt; font-weight:bold; color:#0f7a3a;
          letter-spacing:.1em; text-transform:uppercase;
          border-bottom:2px solid #0f7a3a; padding-bottom:6px;
          margin-bottom:12px;
        }
        .seccion-texto {
          font-size:11pt; color:#1e293b; line-height:1.8;
          text-align:justify; word-break:break-word;
        }

        /* ── OBS DOCENTE ── */
        .obs-bloque {
          margin-bottom:28px; padding:16px 20px;
          background:#fffbeb; border-left:4px solid #f59e0b;
          border-radius:0 8px 8px 0;
        }
        .obs-titulo { font-size:9pt; font-weight:bold; color:#92400e;
          text-transform:uppercase; letter-spacing:.1em; margin-bottom:6px; }
        .obs-texto  { font-size:11pt; color:#78350f; line-height:1.6; }

        /* ── FOOTER ── */
        .footer {
          margin-top:60px; padding-top:16px;
          border-top:1px solid #e2e8f0;
          display:flex; justify-content:space-between;
          font-size:9pt; color:#94a3b8;
        }

        @media print {
          body { -webkit-print-color-adjust:exact; print-color-adjust:exact; }
          .portada { page-break-after:always; }
        }
      </style>
    </head>
    <body>

      <!-- PORTADA -->
      <div class="portada">
        <div class="escudo">🎓</div>
        <div class="uni-nombre">Universidad Técnica Estatal de Quevedo</div>
        <div class="uni-facultad">Facultad de Ciencias de la Ingeniería</div>
        <div class="uni-carrera">${est.carrera}</div>
        <div class="linea-verde"></div>
        <div class="doc-tipo">Informe Práctico — Examen Complexivo</div>
        <div class="doc-titulo">${inf.titulo || 'Sin título definido'}</div>
        <table class="info-grid">
          <tr>
            <td class="lbl">Estudiante:</td>
            <td>${est.nombreEstudiante}</td>
          </tr>
          <tr>
            <td class="lbl">Carrera:</td>
            <td>${est.carrera}</td>
          </tr>
          <tr>
            <td class="lbl">Docente DT2:</td>
            <td>${inf.nombreDocente ?? '—'}</td>
          </tr>
          <tr>
            <td class="lbl">Modalidad:</td>
            <td>Examen Complexivo</td>
          </tr>
        </table>
        <span class="estado-badge">${inf.estado ?? 'BORRADOR'}</span>
      </div>

      <!-- CONTENIDO -->
      <div class="contenido">

        ${inf.observaciones ? `
        <div class="obs-bloque">
          <div class="obs-titulo">Observaciones del Docente</div>
          <div class="obs-texto">${inf.observaciones}</div>
        </div>` : ''}

        ${seccion('I',   'Planteamiento del Problema', inf.planteamientoProblema)}
        ${seccion('II',  'Objetivos',                  inf.objetivos)}
        ${seccion('III', 'Marco Teórico',               inf.marcoTeorico)}
        ${seccion('IV',  'Metodología',                 inf.metodologia)}
        ${seccion('V',   'Resultados y Discusión',      inf.resultados)}
        ${seccion('VI',  'Conclusiones',                inf.conclusiones)}
        ${seccion('VII', 'Bibliografía',                inf.bibliografia)}

        <div class="footer">
          <span>UTEQ — Sistema de Gestión del Proceso de Titulación</span>
          <span>Generado: ${new Date().toLocaleDateString('es-EC',
      { day: '2-digit', month: 'long', year: 'numeric' })}</span>
        </div>
      </div>

    </body>
    </html>`;

    const win = window.open('', '_blank', 'width=960,height=720');
    if (win) {
      win.document.write(html);
      win.document.close();
      win.onload = () => win.print();
    }
  }

  badgeEstado(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR:  'badge-draft',
      ENTREGADO: 'badge-pending',
      APROBADO:  'badge-ok',
      RECHAZADO: 'badge-error'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }

  private cargarEstudiantes(): void {
    this.loading.set(true);
    this.api.getMisEstudiantesDt2(this.idDocente)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: (d) => this.estudiantes.set(d), error: () => {} });
  }

  private cargarInforme(idComplexivo: number): void {
    this.loading.set(true);
    this.api.getInformeDocenteDt2(this.idDocente, idComplexivo)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.informe.set(d),
        error: () => this.informe.set(null)
      });
  }

  private cargarAsesorias(idComplexivo: number): void {
    this.api.listarAsesoriasDt2(this.idDocente, idComplexivo)
      .subscribe({
        next: (d) => this.asesorias.set(d),
        error: () => this.asesorias.set([])
      });
  }
}
