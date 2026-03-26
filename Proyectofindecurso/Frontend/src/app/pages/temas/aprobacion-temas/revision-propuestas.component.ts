
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionUser, getSessionEntityId, getUserRoles } from '../../../services/session';
import { ComisionTemasService, PropuestaTemaDto } from '../../../services/comision-temas';

type FiltroEstado = 'TODAS' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA';
type ModoRevision  = 'COMISION' | 'COMPLEXIVO';

@Component({
  selector: 'app-revision-propuestas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './revision-propuestas.component.html',
  styleUrls: ['./revision-propuestas.component.scss']
})
export class RevisionPropuestasComponent implements OnInit {

  propuestas: PropuestaTemaDto[] = [];
  propuestaEnRevision: PropuestaTemaDto | null = null;
  decisionEstado: 'APROBADA' | 'RECHAZADA' | null = null;
  decisionObservaciones = '';
  errorDecision = '';

  cargando   = false;
  procesando = false;
  filtroActual: FiltroEstado = 'TODAS';

  modoRevision: ModoRevision = 'COMISION';
  esDocenteComplexivo = false;
  esMiembroComision   = false;

  readonly filtros: { valor: FiltroEstado; etiqueta: string }[] = [
    { valor: 'TODAS',       etiqueta: 'Todas' },
    { valor: 'EN_REVISION', etiqueta: 'En revisión' },
    { valor: 'APROBADA',    etiqueta: 'Aprobadas' },
    { valor: 'RECHAZADA',   etiqueta: 'Rechazadas' },
  ];

  private idDocente: number = getSessionEntityId(getSessionUser(), 'docente') ?? 0;

  constructor(private readonly comisionService: ComisionTemasService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.detectarModo(); }

  private detectarModo(): void {
    this.comisionService.listarPropuestasComision(this.idDocente).subscribe({
      next: data => {
        this.esMiembroComision = true;
        if (this.modoRevision === 'COMISION') {
          this.propuestas = data;
          this.cargando = false;
        }
      },
      error: () => { this.esMiembroComision = false; }
    });

    this.comisionService.listarPropuestasComplexivo(this.idDocente).subscribe({
      next: data => {
        this.esDocenteComplexivo = true;
        if (!this.esMiembroComision) {
          this.modoRevision = 'COMPLEXIVO';
          this.propuestas   = data;
          this.cargando     = false;
        }
      },
      error: () => { this.esDocenteComplexivo = false; this.cargando = false; }
    });
  }

  cambiarModo(modo: ModoRevision): void {
    this.modoRevision = modo;
    this.cargarPropuestas();
  }

  cargarPropuestas(): void {
    this.cargando = true;
    this.propuestas = [];
    this.cdr.detectChanges();

    if (this.modoRevision === 'COMPLEXIVO') {
      this.comisionService.listarPropuestasComplexivo(this.idDocente).subscribe({
        next: data => { this.propuestas = data; this.cargando = false; this.cdr.detectChanges(); },
        error: ()   => { this.cargando = false; this.cdr.detectChanges(); }
      });
    } else {
      this.comisionService.listarPropuestasComision(this.idDocente).subscribe({
        next: data => { this.propuestas = data; this.cargando = false; this.cdr.detectChanges(); },
        error: ()   => { this.cargando = false; this.cdr.detectChanges(); }
      });
    }
  }

  get propuestasFiltradas(): PropuestaTemaDto[] {
    if (this.filtroActual === 'TODAS') return this.propuestas;
    return this.propuestas.filter(p => p.estado === this.filtroActual);
  }

  contarPorEstado(estado: FiltroEstado): number {
    if (estado === 'TODAS') return this.propuestas.length;
    return this.propuestas.filter(p => p.estado === estado).length;
  }

  cambiarFiltro(f: FiltroEstado): void { this.filtroActual = f; }

  abrirDecision(p: PropuestaTemaDto): void {
    this.propuestaEnRevision   = p;
    this.decisionEstado        = null;
    this.decisionObservaciones = '';
    this.errorDecision         = '';
  }

  cerrarModal(): void {
    this.propuestaEnRevision   = null;
    this.decisionEstado        = null;
    this.decisionObservaciones = '';
    this.errorDecision         = '';
  }

  confirmarDecision(): void {
    if (!this.propuestaEnRevision || !this.decisionEstado) return;
    this.errorDecision = '';
    this.procesando    = true;

    const obs$ = this.modoRevision === 'COMPLEXIVO'
      ? this.comisionService.decidirPropuestaComplexivo(
        this.idDocente,
        this.propuestaEnRevision.idPropuesta,
        this.decisionEstado,
        this.decisionObservaciones)
      : this.comisionService.decidirPropuesta(
        this.idDocente,
        this.propuestaEnRevision.idPropuesta,
        this.decisionEstado,
        this.decisionObservaciones);

    obs$.subscribe({
      next: () => {
        this.procesando = false;
        this.cerrarModal();
        this.cargarPropuestas();
      },
      error: (err) => {
        this.procesando    = false;
        this.errorDecision = err?.error?.message || 'Error al registrar la decisión.';
      }
    });
  }

  // ── PDF profesional ────────────────────────────────────────────
  verPdfPropuesta(p: PropuestaTemaDto): void {
    const fechaFormateada = p.fechaEnvio
      ? new Date(p.fechaEnvio).toLocaleDateString('es-EC',
        { day: '2-digit', month: 'long', year: 'numeric' })
      : '—';

    const seccion = (titulo: string, contenido: string | null) =>
      contenido ? `
        <div class="seccion">
          <div class="seccion-titulo">${titulo}</div>
          <div class="seccion-texto">${contenido.replace(/\n/g, '<br>')}</div>
        </div>` : '';

    const html = `
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Propuesta — ${p.titulo}</title>
        <style>
          * { margin:0; padding:0; box-sizing:border-box; }
          body { font-family:'Times New Roman',serif; color:#1a1a1a; background:#fff; }

          .portada {
            display:flex; flex-direction:column; align-items:center;
            justify-content:center; min-height:100vh;
            padding:60px 80px; text-align:center;
            border-bottom:3px solid #0f7a3a; page-break-after:always;
          }
          .logo { font-size:13px; font-weight:bold; color:#0f7a3a;
            letter-spacing:3px; text-transform:uppercase; margin-bottom:8px; }
          .universidad { font-size:18px; font-weight:bold; margin-bottom:4px; }
          .facultad { font-size:13px; color:#555; margin-bottom:40px; }
          .linea { width:80px; height:3px; background:#0f7a3a; margin:20px auto; }
          .tipo-doc { font-size:12px; letter-spacing:2px; text-transform:uppercase;
            color:#0f7a3a; font-weight:600; margin-bottom:20px; }
          .titulo-prop { font-size:22px; font-weight:bold; line-height:1.4;
            max-width:500px; margin:0 auto 40px; }
          .info-tabla { width:100%; max-width:420px; margin:0 auto; }
          .info-tabla td { padding:6px 12px; font-size:13px; text-align:left; }
          .lbl { font-weight:bold; color:#555; width:140px; }
          .estado-badge { display:inline-block; margin-top:30px;
            padding:6px 20px; border-radius:20px; font-size:12px;
            font-weight:bold; letter-spacing:1px; text-transform:uppercase; }
          .EN_REVISION, .ENVIADA { background:#fef3c7; color:#92400e; border:1px solid #f59e0b; }
          .APROBADA { background:#d1fae5; color:#065f46; border:1px solid #10b981; }
          .RECHAZADA { background:#fee2e2; color:#991b1b; border:1px solid #ef4444; }

          .contenido { padding:50px 70px; }
          .seccion { margin-bottom:28px; }
          .seccion-titulo {
            font-size:11px; font-weight:bold; color:#0f7a3a;
            letter-spacing:2px; text-transform:uppercase;
            border-bottom:2px solid #0f7a3a; padding-bottom:6px; margin-bottom:12px;
          }
          .seccion-texto {
            font-size:13px; color:#2d3748; line-height:1.8;
            text-align:justify; word-break:break-word;
          }
          .footer {
            margin-top:60px; padding-top:16px; border-top:1px solid #e2e8f0;
            display:flex; justify-content:space-between; font-size:11px; color:#999;
          }
          @media print { body { -webkit-print-color-adjust:exact; } }
        </style>
      </head>
      <body>
        <div class="portada">
          <div class="logo">UTEQ</div>
          <div class="universidad">Universidad Técnica Estatal de Quevedo</div>
          <div class="facultad">Facultad de Ciencias de la Ingeniería</div>
          <div class="linea"></div>
          <div class="tipo-doc">Propuesta de Titulación — ${p.modalidad ?? 'Examen Complexivo'}</div>
          <div class="titulo-prop">${p.titulo}</div>
          <table class="info-tabla">
            <tr><td class="lbl">Estudiante:</td><td>${p.estudiante}</td></tr>
            <tr><td class="lbl">Carrera:</td><td>${p.carrera}</td></tr>
            <tr><td class="lbl">Fecha de envío:</td><td>${fechaFormateada}</td></tr>
            <tr><td class="lbl">Modalidad:</td><td>${p.modalidad ?? '—'}</td></tr>
          </table>
          <span class="estado-badge ${p.estado}">${p.estado}</span>
        </div>

        <div class="contenido">
          ${seccion('I. Planteamiento del Problema', p.planteamientoProblema)}
          ${seccion('II. Objetivos Generales', p.objetivosGenerales)}
          ${seccion('III. Objetivos Específicos', p.objetivosEspecificos)}
          ${seccion('IV. Marco Teórico', p.marcoTeorico)}
          ${seccion('V. Metodología', p.metodologia)}
          ${seccion('VI. Resultados Esperados', p.resultadosEsperados)}
          ${seccion('VII. Bibliografía', p.bibliografia)}
          ${p.observaciones ? seccion('Observaciones del Docente', p.observaciones) : ''}

          <div class="footer">
            <span>UTEQ — Sistema de Gestión de Titulación</span>
            <span>Generado: ${new Date().toLocaleDateString('es-EC',
      { day: '2-digit', month: 'long', year: 'numeric' })}</span>
          </div>
        </div>
      </body>
      </html>`;

    const win = window.open('', '_blank', 'width=900,height=700');
    if (win) {
      win.document.write(html);
      win.document.close();
      win.onload = () => win.print();
    }
  }

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      EN_REVISION: 'En revisión', APROBADA: 'Aprobada',
      RECHAZADA: 'Rechazada', TODAS: 'Todas'
    };
    return map[estado] ?? estado;
  }

  get tituloVista(): string {
    return this.modoRevision === 'COMPLEXIVO'
      ? 'Propuestas — Examen Complexivo'
      : 'Propuestas — Comisión Formativa';
  }

  get subtituloVista(): string {
    return this.modoRevision === 'COMPLEXIVO'
      ? 'Propuestas de tus estudiantes asignados en Complexivo'
      : 'Propuestas de titulación (TIC) pendientes de revisión por la comisión';
  }
}
