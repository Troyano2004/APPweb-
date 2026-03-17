import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

// ── Interfaces ───────────────────────────────────────────────────────────────
interface ResumenReporte {
  total: number;
  enviadas: number;
  enRevision: number;
  aprobadas: number;
  rechazadas: number;
  porcentajeAprobacion: number;
}

interface ItemReporte {
  idPropuesta: number;
  estudiante: string;
  cedula: string;
  carrera: string;
  titulo: string;
  temaInvestigacion: string;
  estado: string;
  fechaEnvio: string;
  fechaRevision: string;
  observacionesComision: string;
  decisionDirector: string;
  observacionesDirector: string;
  nombreDirector: string;
}

interface RespuestaCompleta {
  resumen: ResumenReporte;
  propuestas: ItemReporte[];
}

@Component({
  selector: 'app-reporte-propuestas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="rp-page">

      <!-- Encabezado -->
      <header class="rp-header">
        <div class="rp-header-left">
          <h1 class="rp-title">Reporte de Propuestas</h1>
          <p class="rp-sub">Módulo Propuesta y Anteproyecto — Estado general y dictámenes</p>
        </div>
        <div class="rp-header-actions">
          <button class="btn-pdf" (click)="exportarPdf()" [disabled]="cargando">
            <span class="btn-icon">⬇</span> Exportar PDF
          </button>
        </div>
      </header>

      <!-- Filtros -->
      <div class="rp-filtros">
        <label class="filtro-label">Filtrar por estado:</label>
        <div class="filtro-tabs">
          <button
            *ngFor="let op of opcionesEstado"
            class="tab"
            [class.active]="filtroEstado === op.valor"
            (click)="cambiarFiltro(op.valor)">
            {{ op.etiqueta }}
          </button>
        </div>
      </div>

      <!-- KPIs -->
      <div class="rp-kpis" *ngIf="datos">
        <div class="kpi">
          <span class="kpi-num">{{ datos.resumen.total }}</span>
          <span class="kpi-lab">Total</span>
        </div>
        <div class="kpi kpi-yellow">
          <span class="kpi-num">{{ datos.resumen.enviadas }}</span>
          <span class="kpi-lab">Enviadas</span>
        </div>
        <div class="kpi kpi-blue">
          <span class="kpi-num">{{ datos.resumen.enRevision }}</span>
          <span class="kpi-lab">En revisión</span>
        </div>
        <div class="kpi kpi-green">
          <span class="kpi-num">{{ datos.resumen.aprobadas }}</span>
          <span class="kpi-lab">Aprobadas</span>
        </div>
        <div class="kpi kpi-red">
          <span class="kpi-num">{{ datos.resumen.rechazadas }}</span>
          <span class="kpi-lab">Rechazadas</span>
        </div>
        <div class="kpi kpi-green">
          <span class="kpi-num">{{ datos.resumen.porcentajeAprobacion }}%</span>
          <span class="kpi-lab">Aprobación</span>
        </div>
      </div>

      <!-- Búsqueda rápida -->
      <div class="rp-search-bar" *ngIf="datos">
        <input
          class="search-input"
          type="text"
          placeholder="Buscar por estudiante, título o carrera…"
          [(ngModel)]="textoBusqueda"
          (input)="filtrarTabla()" />
        <span class="search-count">{{ propuestasFiltradas.length }} registros</span>
      </div>

      <!-- Cargando -->
      <div class="rp-loading" *ngIf="cargando">
        <div class="spinner"></div>
        <span>Cargando datos…</span>
      </div>

      <!-- Error -->
      <div class="rp-error" *ngIf="error">
        <span>⚠ {{ error }}</span>
      </div>

      <!-- Tabla -->
      <div class="rp-table-wrap" *ngIf="datos && !cargando">
        <table class="rp-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Estudiante</th>
              <th>Carrera</th>
              <th>Título de la propuesta</th>
              <th>Estado</th>
              <th>Fecha envío</th>
              <th>Fecha revisión</th>
              <th>Dictamen director</th>
              <th>Observaciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of propuestasFiltradas; let i = index">
              <td class="td-num">{{ i + 1 }}</td>
              <td>
                <strong>{{ p.estudiante || '—' }}</strong>
                <br/><small>{{ p.cedula }}</small>
              </td>
              <td>{{ p.carrera || '—' }}</td>
              <td class="td-titulo">{{ p.titulo }}</td>
              <td>
                <span class="badge" [ngClass]="claseEstado(p.estado)">
                  {{ etiquetaEstado(p.estado) }}
                </span>
              </td>
              <td>{{ formatFecha(p.fechaEnvio) }}</td>
              <td>{{ formatFecha(p.fechaRevision) }}</td>
              <td>
                <span *ngIf="p.decisionDirector" class="badge" [ngClass]="claseDecision(p.decisionDirector)">
                  {{ p.decisionDirector }}
                </span>
                <span *ngIf="!p.decisionDirector" class="txt-muted">—</span>
                <div *ngIf="p.nombreDirector" class="txt-director">{{ p.nombreDirector }}</div>
              </td>
              <td class="td-obs">{{ p.observacionesComision || '—' }}</td>
            </tr>
            <tr *ngIf="propuestasFiltradas.length === 0">
              <td colspan="9" class="td-empty">No se encontraron propuestas con los filtros aplicados.</td>
            </tr>
          </tbody>
        </table>
      </div>

    </section>
  `,
  styles: [`
    .rp-page {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      padding: 0.5rem 0;
    }

    /* Header */
    .rp-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .rp-title { margin: 0 0 0.2rem; font-size: 1.4rem; font-weight: 600; color: #111; }
    .rp-sub   { margin: 0; color: #6b7280; font-size: 0.9rem; }

    /* Botón PDF */
    .btn-pdf {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      background: #0f7a3a;
      color: #fff;
      border: none;
      border-radius: 0.6rem;
      padding: 0.55rem 1.1rem;
      font-size: 0.88rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-pdf:hover   { background: #0c6a32; }
    .btn-pdf:disabled{ opacity: 0.6; cursor: not-allowed; }
    .btn-icon { font-size: 1rem; }

    /* Filtros tabs */
    .rp-filtros {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .filtro-label { font-size: 0.85rem; color: #6b7280; font-weight: 500; white-space: nowrap; }
    .filtro-tabs  { display: flex; gap: 0.4rem; flex-wrap: wrap; }
    .tab {
      border: 1px solid #d1d5db;
      background: #fff;
      border-radius: 999px;
      padding: 0.3rem 0.85rem;
      font-size: 0.82rem;
      cursor: pointer;
      color: #374151;
      transition: all 0.15s;
    }
    .tab:hover  { border-color: #0f7a3a; color: #0f7a3a; }
    .tab.active { background: #0f7a3a; color: #fff; border-color: #0f7a3a; font-weight: 600; }

    /* KPIs */
    .rp-kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
      gap: 0.75rem;
    }
    .kpi {
      background: #fff;
      border: 1px solid #e5e7eb;
      border-radius: 0.75rem;
      padding: 1rem 0.75rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .kpi-num { font-size: 1.75rem; font-weight: 700; color: #111; }
    .kpi-lab { font-size: 0.78rem; color: #9ca3af; text-transform: uppercase; letter-spacing: .04em; }
    .kpi-green .kpi-num { color: #0f7a3a; }
    .kpi-red   .kpi-num { color: #dc2626; }
    .kpi-blue  .kpi-num { color: #2563eb; }
    .kpi-yellow.kpi-num { color: #d97706; }

    /* Búsqueda */
    .rp-search-bar {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .search-input {
      flex: 1;
      border: 1px solid #d1d5db;
      border-radius: 0.5rem;
      padding: 0.5rem 0.85rem;
      font-size: 0.88rem;
      outline: none;
      transition: border-color 0.15s;
    }
    .search-input:focus { border-color: #0f7a3a; }
    .search-count { font-size: 0.82rem; color: #9ca3af; white-space: nowrap; }

    /* Loading */
    .rp-loading {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 2rem; justify-content: center; color: #6b7280;
    }
    .spinner {
      width: 22px; height: 22px;
      border: 3px solid #e5e7eb;
      border-top-color: #0f7a3a;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Error */
    .rp-error {
      background: #fef2f2; border: 1px solid #fecaca;
      border-radius: 0.6rem; padding: 0.75rem 1rem;
      color: #991b1b; font-size: 0.88rem;
    }

    /* Tabla */
    .rp-table-wrap {
      background: #fff;
      border: 1px solid #e5e7eb;
      border-radius: 0.75rem;
      overflow-x: auto;
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
    }
    .rp-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.875rem;
    }
    .rp-table thead tr { background: #f9fafb; }
    .rp-table th {
      padding: 0.75rem 0.9rem;
      text-align: left;
      font-size: 0.78rem;
      font-weight: 600;
      color: #374151;
      text-transform: uppercase;
      letter-spacing: .04em;
      border-bottom: 2px solid #e5e7eb;
      white-space: nowrap;
    }
    .rp-table td {
      padding: 0.7rem 0.9rem;
      border-bottom: 1px solid #f3f4f6;
      color: #374151;
      vertical-align: top;
    }
    .rp-table tbody tr:hover td { background: #f9fafb; }
    .td-num   { color: #9ca3af; font-size: 0.78rem; width: 36px; text-align: center; }
    .td-titulo{ max-width: 220px; line-height: 1.4; }
    .td-obs   { max-width: 180px; font-size: 0.8rem; color: #6b7280; line-height: 1.4; }
    .td-empty { text-align: center; padding: 2rem; color: #9ca3af; }
    small     { font-size: 0.75rem; color: #9ca3af; }
    .txt-muted    { color: #9ca3af; }
    .txt-director { font-size: 0.75rem; color: #6b7280; margin-top: 2px; }

    /* Badges */
    .badge {
      display: inline-block;
      padding: 0.2rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: .03em;
      white-space: nowrap;
    }
    .badge-aprobada  { background: #d1fae5; color: #065f46; }
    .badge-rechazada { background: #fee2e2; color: #991b1b; }
    .badge-revision  { background: #dbeafe; color: #1e40af; }
    .badge-enviada   { background: #fef9c3; color: #92400e; }
    .badge-observada { background: #ede9fe; color: #5b21b6; }
  `]
})
export class ReportePropuestasComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/reportes/propuestas';

  datos: RespuestaCompleta | null = null;
  propuestasFiltradas: ItemReporte[] = [];
  cargando = false;
  error = '';
  filtroEstado = '';
  textoBusqueda = '';

  opcionesEstado = [
    { valor: '',           etiqueta: 'Todas' },
    { valor: 'ENVIADA',    etiqueta: 'Enviadas' },
    { valor: 'EN_REVISION',etiqueta: 'En revisión' },
    { valor: 'APROBADA',   etiqueta: 'Aprobadas' },
    { valor: 'RECHAZADA',  etiqueta: 'Rechazadas' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.error = '';
    const params = this.filtroEstado ? `?estado=${this.filtroEstado}` : '';
    this.http.get<RespuestaCompleta>(`${this.API}${params}`).subscribe({
      next: (res) => {
        this.datos = res;
        this.propuestasFiltradas = res.propuestas;
        this.textoBusqueda = '';
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el reporte. Verifica que el servidor esté activo.';
        this.cargando = false;
      }
    });
  }

  cambiarFiltro(estado: string): void {
    this.filtroEstado = estado;
    this.cargarDatos();
  }

  filtrarTabla(): void {
    if (!this.datos) return;
    const q = this.textoBusqueda.toLowerCase().trim();
    if (!q) {
      this.propuestasFiltradas = this.datos.propuestas;
      return;
    }
    this.propuestasFiltradas = this.datos.propuestas.filter(p =>
      (p.estudiante  || '').toLowerCase().includes(q) ||
      (p.titulo      || '').toLowerCase().includes(q) ||
      (p.carrera     || '').toLowerCase().includes(q) ||
      (p.cedula      || '').toLowerCase().includes(q)
    );
  }

  exportarPdf(): void {
    const params = this.filtroEstado ? `?estado=${this.filtroEstado}` : '';
    window.open(`${this.API}/pdf${params}`, '_blank');
  }

  formatFecha(f: string | null): string {
    if (!f) return '—';
    const d = new Date(f);
    return d.toLocaleDateString('es-EC', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  claseEstado(estado: string): Record<string, boolean> {
    return {
      'badge-aprobada':  estado === 'APROBADA',
      'badge-rechazada': estado === 'RECHAZADA',
      'badge-revision':  estado === 'EN_REVISION',
      'badge-enviada':   estado === 'ENVIADA',
    };
  }

  etiquetaEstado(estado: string): string {
    const map: Record<string, string> = {
      'APROBADA':    'APROBADA',
      'RECHAZADA':   'RECHAZADA',
      'EN_REVISION': 'EN REVISIÓN',
      'ENVIADA':     'ENVIADA',
    };
    return map[estado] ?? estado;
  }

  claseDecision(dec: string): Record<string, boolean> {
    return {
      'badge-aprobada':  dec === 'APROBADA',
      'badge-rechazada': dec === 'RECHAZADA',
      'badge-observada': dec === 'OBSERVADA',
    };
  }
}
