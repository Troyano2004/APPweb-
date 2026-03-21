
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ComplexivoService,
  EstudianteDeDocenteDto,
  ComplexivoInformeDto,
  ComplexivoAsesoriaDto,
  PropuestaComplexivoDto
} from '../../../services/complexivo.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

type TabPrincipal = 'propuestas' | 'informes';
type TabInforme   = 'informe' | 'asesorias';

@Component({
  selector: 'app-complexivo-docente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complexivo-docente.html',
  styleUrls: ['./complexivo-docente.scss']
})
export class ComplexivoDocenteComponent implements OnInit {

  private idDocente = 0;

  loading   = signal(false);
  error     = signal<string | null>(null);
  ok        = signal<string | null>(null);

  tabPrincipal = signal<TabPrincipal>('propuestas');

  // Propuestas — variables SEPARADAS para aprobar y rechazar
  propuestas        = signal<PropuestaComplexivoDto[]>([]);
  propuestaSelec    = signal<PropuestaComplexivoDto | null>(null);
  obsAprobacion     = '';   // ← solo para aprobar
  obsRechazo        = '';   // ← solo para rechazar
  mostrarRechazoP   = false;
  procesandoIdP     = signal<number | null>(null);

  // Informes
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
    this.cargarPropuestas();
    this.cargarEstudiantes();
  }

  setTabPrincipal(t: TabPrincipal): void {
    this.tabPrincipal.set(t);
    this.error.set(null); this.ok.set(null);
  }

  // ── Propuestas ─────────────────────────────────────────────────
  seleccionarPropuesta(p: PropuestaComplexivoDto): void {
    this.propuestaSelec.set(p);
    this.obsAprobacion = '';
    this.obsRechazo    = '';
    this.mostrarRechazoP = false;
    this.error.set(null); this.ok.set(null);
  }

  descargarPdfPropuesta(): void {
    const p = this.propuestaSelec();
    if (!p) return;

    const contenido = `
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Propuesta de Titulación</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body { font-family: 'Times New Roman', serif; color: #1a1a1a; background: #fff; }

          .portada {
            display: flex; flex-direction: column; align-items: center;
            justify-content: center; min-height: 100vh;
            padding: 60px 80px; text-align: center;
            border-bottom: 3px solid #0f7a3a;
          }
          .portada .logo-text {
            font-size: 14px; font-weight: bold; color: #0f7a3a;
            letter-spacing: 3px; text-transform: uppercase; margin-bottom: 8px;
          }
          .portada .universidad {
            font-size: 18px; font-weight: bold; color: #1a1a1a;
            margin-bottom: 4px;
          }
          .portada .facultad {
            font-size: 14px; color: #555; margin-bottom: 40px;
          }
          .portada .linea { width: 80px; height: 3px; background: #0f7a3a; margin: 20px auto; }
          .portada .tipo-doc {
            font-size: 13px; letter-spacing: 2px; text-transform: uppercase;
            color: #0f7a3a; font-weight: 600; margin-bottom: 20px;
          }
          .portada .titulo-propuesta {
            font-size: 22px; font-weight: bold; color: #1a1a1a;
            line-height: 1.4; max-width: 500px; margin: 0 auto 40px;
          }
          .portada .info-tabla { width: 100%; max-width: 420px; margin: 0 auto; }
          .portada .info-tabla tr td {
            padding: 6px 12px; font-size: 13px; text-align: left;
          }
          .portada .info-tabla .lbl { font-weight: bold; color: #555; width: 130px; }
          .portada .info-tabla .val { color: #1a1a1a; }
          .portada .estado-badge {
            display: inline-block; margin-top: 30px;
            padding: 6px 20px; border-radius: 20px; font-size: 12px;
            font-weight: bold; letter-spacing: 1px; text-transform: uppercase;
          }
          .estado-EN_REVISION { background: #fef3c7; color: #92400e; border: 1px solid #f59e0b; }
          .estado-APROBADA    { background: #d1fae5; color: #065f46; border: 1px solid #10b981; }
          .estado-RECHAZADA   { background: #fee2e2; color: #991b1b; border: 1px solid #ef4444; }

          .contenido { padding: 50px 70px; }

          .seccion-titulo {
            font-size: 11px; font-weight: bold; color: #0f7a3a;
            letter-spacing: 2px; text-transform: uppercase;
            border-bottom: 2px solid #0f7a3a; padding-bottom: 6px;
            margin: 30px 0 14px;
          }
          .seccion-texto {
            font-size: 13px; color: #2d3748; line-height: 1.8;
            text-align: justify; white-space: pre-wrap;
            word-break: break-word;
          }

          .footer {
            margin-top: 60px; padding-top: 20px;
            border-top: 1px solid #e2e8f0;
            display: flex; justify-content: space-between;
            font-size: 11px; color: #999;
          }

          @media print {
            .portada { page-break-after: always; }
          }
        </style>
      </head>
      <body>

        <!-- PORTADA -->
        <div class="portada">
          <div class="logo-text">UTEQ</div>
          <div class="universidad">Universidad Técnica Estatal de Quevedo</div>
          <div class="facultad">Facultad de Ciencias de la Ingeniería</div>
          <div class="linea"></div>
          <div class="tipo-doc">Propuesta de Titulación — Examen Complexivo</div>
          <div class="titulo-propuesta">${p.titulo || 'Sin título'}</div>
          <table class="info-tabla">
            <tr>
              <td class="lbl">Estudiante:</td>
              <td class="val">${p.nombreEstudiante}</td>
            </tr>
            <tr>
              <td class="lbl">Fecha envío:</td>
              <td class="val">${p.fechaEnvio ? new Date(p.fechaEnvio).toLocaleDateString('es-EC', {day:'2-digit',month:'long',year:'numeric'}) : '—'}</td>
            </tr>
            <tr>
              <td class="lbl">Modalidad:</td>
              <td class="val">Examen Complexivo</td>
            </tr>
          </table>
          <span class="estado-badge estado-${p.estado}">${p.estado}</span>
        </div>

        <!-- CONTENIDO -->
        <div class="contenido">

          ${p.planteamientoProblema ? `
          <div class="seccion-titulo">I. Planteamiento del Problema</div>
          <div class="seccion-texto">${p.planteamientoProblema}</div>
          ` : ''}

          ${p.objetivosGenerales ? `
          <div class="seccion-titulo">II. Objetivos Generales</div>
          <div class="seccion-texto">${p.objetivosGenerales}</div>
          ` : ''}

          ${p.objetivosEspecificos ? `
          <div class="seccion-titulo">III. Objetivos Específicos</div>
          <div class="seccion-texto">${p.objetivosEspecificos}</div>
          ` : ''}

          ${p.metodologia ? `
          <div class="seccion-titulo">IV. Metodología</div>
          <div class="seccion-texto">${p.metodologia}</div>
          ` : ''}

          ${p.resultadosEsperados ? `
          <div class="seccion-titulo">V. Resultados Esperados</div>
          <div class="seccion-texto">${p.resultadosEsperados}</div>
          ` : ''}

          ${p.bibliografia ? `
          <div class="seccion-titulo">VI. Bibliografía</div>
          <div class="seccion-texto">${p.bibliografia}</div>
          ` : ''}

          ${p.observacionesComision ? `
          <div class="seccion-titulo">Observaciones del Docente</div>
          <div class="seccion-texto">${p.observacionesComision}</div>
          ` : ''}

          <div class="footer">
            <span>UTEQ — Sistema de Gestión de Titulación</span>
            <span>Generado: ${new Date().toLocaleDateString('es-EC', {day:'2-digit',month:'long',year:'numeric'})}</span>
          </div>
        </div>

      </body>
      </html>
    `;

    const ventana = window.open('', '_blank', 'width=900,height=700');
    if (ventana) {
      ventana.document.write(contenido);
      ventana.document.close();
      ventana.onload = () => ventana.print();
    }
  }

  aprobarPropuesta(): void {
    const p = this.propuestaSelec();
    if (!p) return;
    this.procesandoIdP.set(p.idPropuesta);
    this.error.set(null); this.ok.set(null);
    this.api.decidirPropuesta(this.idDocente, p.idPropuesta, 'APROBADA', this.obsAprobacion)
      .pipe(finalize(() => this.procesandoIdP.set(null)))
      .subscribe({
        next: () => {
          this.ok.set('✅ Propuesta aprobada. El estudiante puede avanzar a Titulación II.');
          this.propuestaSelec.set(null);  // ← limpiar panel derecho
          this.obsAprobacion = '';
          this.cargarPropuestas();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al aprobar.')
      });
  }

  rechazarPropuesta(): void {
    const p = this.propuestaSelec();
    if (!p || !this.obsRechazo.trim()) {
      this.error.set('Escribe el motivo del rechazo.');
      return;
    }
    this.procesandoIdP.set(p.idPropuesta);
    this.error.set(null); this.ok.set(null);
    this.api.decidirPropuesta(this.idDocente, p.idPropuesta, 'RECHAZADA', this.obsRechazo)
      .pipe(finalize(() => this.procesandoIdP.set(null)))
      .subscribe({
        next: () => {
          this.ok.set('Propuesta rechazada. El estudiante deberá corregirla.');
          this.propuestaSelec.set(null);  // ← limpiar panel derecho
          this.obsRechazo = '';
          this.mostrarRechazoP = false;
          this.cargarPropuestas();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  // ── Informes ───────────────────────────────────────────────────
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
    this.tabInforme.set(t);
    this.error.set(null); this.ok.set(null);
  }

  aprobarInforme(): void {
    const inf = this.informe();
    if (!inf?.idInforme) return;
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.aprobarInforme(this.idDocente, inf.idInforme, this.obsAprobar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe aprobado.');
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
    this.api.rechazarInforme(this.idDocente, inf.idInforme, this.obsRechazar)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => {
          this.informe.set(d);
          this.ok.set('Informe rechazado.');
          this.obsRechazar = ''; this.mostrarFormRechazo = false;
          this.cargarEstudiantes();
        },
        error: (e) => this.error.set(e?.error?.message ?? 'Error al rechazar.')
      });
  }

  registrarAsesoria(): void {
    const est = this.estudSeleccionado();
    if (!est || !this.obsAsesoria.trim()) {
      this.error.set('Escribe las observaciones.');
      return;
    }
    this.loading.set(true); this.error.set(null); this.ok.set(null);
    this.api.registrarAsesoria(this.idDocente, est.idComplexivo, this.obsAsesoria)
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

  badgeEstado(estado: string | null): string {
    const map: Record<string, string> = {
      BORRADOR: 'badge-draft',   ENTREGADO: 'badge-pending',
      APROBADO: 'badge-ok',      RECHAZADO: 'badge-error',
      APROBADA: 'badge-ok',      RECHAZADA: 'badge-error',
      EN_REVISION: 'badge-pending', ENVIADA: 'badge-pending',
      EN_CURSO: 'badge-ok'
    };
    return map[estado ?? ''] ?? 'badge-draft';
  }

  propuestaBloqueada(idPropuesta: number): boolean {
    return this.procesandoIdP() === idPropuesta;
  }

  private cargarPropuestas(): void {
    this.api.getPropuestasDocente(this.idDocente).subscribe({
      next: (d) => this.propuestas.set(d),
      error: () => this.propuestas.set([])
    });
  }

  private cargarEstudiantes(): void {
    this.loading.set(true);
    this.api.getMisEstudiantes(this.idDocente)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.estudiantes.set(d),
        error: (e) => this.error.set(e?.error?.message ?? 'Error al cargar.')
      });
  }

  private cargarInforme(idComplexivo: number): void {
    this.loading.set(true);
    this.api.getInformeDocente(this.idDocente, idComplexivo)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.informe.set(d),
        error: () => this.informe.set(null)
      });
  }

  private cargarAsesorias(idComplexivo: number): void {
    this.api.listarAsesorias(this.idDocente, idComplexivo).subscribe({
      next: (d) => this.asesorias.set(d),
      error: () => this.asesorias.set([])
    });
  }
}
