import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BackupService,
  BackupJobDto,
  RestoreResponse,
  RestoreResultado
} from '../../../services/backup.service';

// ── Interfaces Timeline ────────────────────────────────────────────────────
export interface BackupNodoDto {
  idExecution:         number;
  tipo:                'FULL' | 'DIFERENCIAL';
  estado:              string;
  databaseNombre:      string;
  iniciadoEn:          string;
  finalizadoEn:        string;
  duracionSegundos:    number;
  tamanoBytes:         number;
  archivoNombre:       string;
  archivoDisponible:   boolean;
  destinoTipo:         string;
  tablasIncluidas:     string | null;
  idBackupPadre:       number | null;
  manual:              boolean;
  errorMensaje:        string | null;
  numeroDiferenciales: number;
}

export interface BackupCadenaDto {
  full:               BackupNodoDto;
  diferenciales:      BackupNodoDto[];
  totalDiferenciales: number;
  tamanoTotalBytes:   number;
}

export interface TimelineResponseDto {
  idJob:              number;
  nombreJob:          string;
  cadenas:            BackupCadenaDto[];
  totalFull:          number;
  totalDiferenciales: number;
  tamanoTotalBytes:   number;
}

// ── Interfaces Calendario ─────────────────────────────────────────────────
interface CalendarDay {
  date:         Date;
  hasBackup:    boolean;
  hasExito:     boolean;
  hasFallo:     boolean;
  backups:      RestoreResponse[];
  isToday:      boolean;
  isOtherMonth: boolean;
}

interface CalendarMonth {
  year:  number;
  month: number;
  days:  CalendarDay[];
}

@Component({
  selector: 'app-backup-restore',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backup-restore.component.html',
  styleUrls: ['./backup-restore.component.scss']
})
export class BackupRestoreComponent implements OnInit {

  // ── Data ───────────────────────────────────────────────────────────────────
  jobs:      BackupJobDto[]    = [];
  historial: RestoreResponse[] = [];
  jobSeleccionado: BackupJobDto | null = null;

  // ── Calendario ─────────────────────────────────────────────────────────────
  calendarios:        CalendarMonth[]    = [];
  diaSeleccionado:    CalendarDay | null = null;
  backupSeleccionado: RestoreResponse | null = null;

  // ── Timeline git-log ───────────────────────────────────────────────────────
  timeline:           TimelineResponseDto | null = null;
  cargandoTL          = false;
  cadenaExpandida     = new Set<number>();
  nodoTLSeleccionado: BackupNodoDto | null = null;
  fullPadreTL:        BackupNodoDto | null = null;

  // ── Modal ──────────────────────────────────────────────────────────────────
  modo: 'REEMPLAZAR' | 'NUEVA_BD' = 'REEMPLAZAR';
  nombreBdNueva = '';
  mostrarModal  = false;
  mostrarLog    = false;
  resultado:    RestoreResultado | null = null;

  // ── UI ─────────────────────────────────────────────────────────────────────
  cargando        = false;
  cargandoRestore = false;

  // ── Verificación integridad ────────────────────────────────────────────────
  verificando:         { [id: number]: boolean } = {};
  resultadoIntegridad: { [id: number]: any }     = {};

  readonly MESES       = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
  readonly DIAS_SEMANA = ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'];

  constructor(
    private svc: BackupService,
    private cdr: ChangeDetectorRef,
    private zone: NgZone
  ) {}

  ngOnInit(): void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.svc.listarJobs().subscribe({
      next: jobs => this.zone.run(() => {
        this.jobs = jobs; this.cargando = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false; this.cdr.detectChanges();
      })
    });
  }

  // ── Seleccionar job ────────────────────────────────────────────────────────

  seleccionarJob(job: BackupJobDto): void {
    this.jobSeleccionado    = job;
    this.diaSeleccionado    = null;
    this.backupSeleccionado = null;
    this.resultado          = null;
    this.historial          = [];
    this.timeline           = null;
    this.nodoTLSeleccionado = null;
    this.fullPadreTL        = null;
    this.cargando           = true;
    this.cdr.detectChanges();

    this.svc.historialRestore(job.idJob).subscribe({
      next: h => this.zone.run(() => {
        this.historial   = h;
        this.calendarios = this.construirCalendarios(h);
        this.cargando    = false;
        this.cdr.detectChanges();
        this.cargarTimeline(job.idJob);
      }),
      error: () => this.zone.run(() => {
        this.cargando = false;
        this.cdr.detectChanges();
        this.cargarTimeline(job.idJob);
      })
    });
  }

  // ── Timeline ───────────────────────────────────────────────────────────────

  cargarTimeline(idJob: number): void {
    this.cargandoTL = true;
    this.cdr.detectChanges();
    this.svc.obtenerTimeline(idJob).subscribe({
      next: tl => this.zone.run(() => {
        this.timeline = tl; this.cargandoTL = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargandoTL = false; this.cdr.detectChanges();
      })
    });
  }

  toggleCadena(idFull: number): void {
    this.cadenaExpandida.has(idFull)
      ? this.cadenaExpandida.delete(idFull)
      : this.cadenaExpandida.add(idFull);
    this.cdr.detectChanges();
  }

  estaExpandida(idFull: number): boolean {
    return this.cadenaExpandida.has(idFull);
  }

  seleccionarNodoTL(nodo: BackupNodoDto, cadena: BackupCadenaDto): void {
    if (!nodo.archivoDisponible) return;
    this.nodoTLSeleccionado = nodo;
    this.fullPadreTL        = nodo.tipo === 'DIFERENCIAL' ? cadena.full : null;
    this.backupSeleccionado = null;
    this.modo               = 'REEMPLAZAR';
    this.nombreBdNueva      = '';
    this.resultado          = null;
    this.mostrarModal       = true;
    this.mostrarLog         = false;
    this.cdr.detectChanges();
  }

  // ── Calendario ─────────────────────────────────────────────────────────────

  private construirCalendarios(historial: RestoreResponse[]): CalendarMonth[] {
    if (!historial.length) return [];
    const hoy = new Date();
    const meses: CalendarMonth[] = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      meses.push(this.construirMes(d.getFullYear(), d.getMonth(), historial));
    }
    return meses;
  }

  private construirMes(year: number, month: number, historial: RestoreResponse[]): CalendarMonth {
    const primerDia = new Date(year, month, 1);
    const ultimoDia = new Date(year, month + 1, 0);
    const days: CalendarDay[] = [];
    for (let i = 0; i < primerDia.getDay(); i++) {
      days.push(this.crearDia(new Date(year, month, -primerDia.getDay() + i + 1), historial, true));
    }
    for (let d = 1; d <= ultimoDia.getDate(); d++) {
      days.push(this.crearDia(new Date(year, month, d), historial, false));
    }
    for (let i = 1; i <= 42 - days.length; i++) {
      days.push(this.crearDia(new Date(year, month + 1, i), historial, true));
    }
    return { year, month, days };
  }

  private crearDia(date: Date, historial: RestoreResponse[], isOtherMonth: boolean): CalendarDay {
    const hoy        = new Date();
    const dateStr    = this.fechaStr(date);
    const backupsDia = historial.filter(h => this.fechaStr(new Date(h.iniciadoEn)) === dateStr);
    return {
      date, isOtherMonth,
      hasBackup: backupsDia.length > 0,
      hasExito:  backupsDia.some(b => b.estado === 'EXITOSO'),
      hasFallo:  backupsDia.some(b => b.estado === 'FALLIDO'),
      backups:   backupsDia,
      isToday:   this.fechaStr(date) === this.fechaStr(hoy)
    };
  }

  private fechaStr(d: Date): string {
    return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
  }

  seleccionarDia(dia: CalendarDay): void {
    if (!dia.hasBackup || dia.isOtherMonth) return;
    this.diaSeleccionado    = dia;
    this.backupSeleccionado = null;
    this.resultado          = null;
    this.nodoTLSeleccionado = null;
    this.cdr.detectChanges();
  }

  seleccionarBackup(backup: RestoreResponse): void {
    if (!backup.archivoDisponible) return;
    this.backupSeleccionado = backup;
    this.nodoTLSeleccionado = null;
    this.cdr.detectChanges();
  }

  // ── Modal ──────────────────────────────────────────────────────────────────

  abrirModal(): void {
    if (!this.backupSeleccionado) return;
    this.modo = 'REEMPLAZAR'; this.nombreBdNueva = '';
    this.resultado = null; this.mostrarModal = true; this.mostrarLog = false;
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    if (this.cargandoRestore) return;
    this.mostrarModal = false; this.nodoTLSeleccionado = null; this.fullPadreTL = null;
    this.cdr.detectChanges();
  }

  confirmarRestore(): void {
    if (!this.jobSeleccionado) return;
    const idExecution = this.nodoTLSeleccionado
      ? this.nodoTLSeleccionado.idExecution
      : this.backupSeleccionado?.idExecution;
    if (!idExecution) return;
    if (this.modo === 'NUEVA_BD' && !this.nombreBdNueva.trim()) return;

    this.cargandoRestore = true;
    this.cdr.detectChanges();

    this.svc.ejecutarRestore({
      idExecution,
      idJob:         this.jobSeleccionado.idJob,
      modo:          this.modo,
      nombreBdNueva: this.modo === 'NUEVA_BD' ? this.nombreBdNueva.trim() : undefined
    }).subscribe({
      next: r => this.zone.run(() => {
        this.resultado = r; this.cargandoRestore = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.resultado = { exitoso: false, mensaje: 'Error de comunicación con el servidor',
          log: null, bdRestaurada: null, duracionSegundos: 0 };
        this.cargandoRestore = false; this.cdr.detectChanges();
      })
    });
  }

  // ── Verificar integridad ───────────────────────────────────────────────────

  verificarIntegridad(backup: RestoreResponse): void {
    if (!this.jobSeleccionado) return;
    this.verificando[backup.idExecution] = true;
    this.cdr.detectChanges();
    this.svc.verificarIntegridad(backup.idExecution, this.jobSeleccionado.idJob).subscribe({
      next: r => this.zone.run(() => {
        this.resultadoIntegridad[backup.idExecution] = r;
        this.verificando[backup.idExecution] = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.resultadoIntegridad[backup.idExecution] = { valido: false, mensaje: 'Error al verificar', objetosEncontrados: 0 };
        this.verificando[backup.idExecution] = false; this.cdr.detectChanges();
      })
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  formatBytes(bytes: number | null): string {
    if (!bytes) return '—';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1048576)    return `${(bytes/1024).toFixed(1)} KB`;
    if (bytes < 1073741824) return `${(bytes/1048576).toFixed(1)} MB`;
    return `${(bytes/1073741824).toFixed(2)} GB`;
  }

  formatDuracion(seg: number | null): string {
    if (!seg) return '—';
    if (seg < 60) return `${seg}s`;
    return `${Math.floor(seg/60)}m ${seg % 60}s`;
  }

  tablasList(tablas: string | null): string[] {
    if (!tablas || tablas === '(sin cambios)') return [];
    return tablas.split(',').slice(0, 5);
  }

  hayMasTablas(tablas: string | null): boolean {
    return tablas ? tablas.split(',').length > 5 : false;
  }

  masTablas(tablas: string | null): number {
    return tablas ? tablas.split(',').length - 5 : 0;
  }

  formatearTamano(bytes?: number | null): string {
    if (!bytes) return '-';
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1048576)    return `${(bytes/1024).toFixed(1)} KB`;
    if (bytes < 1073741824) return `${(bytes/1048576).toFixed(1)} MB`;
    return `${(bytes/1073741824).toFixed(1)} GB`;
  }

  formatearHora(fecha: string): string {
    const d = new Date(fecha);
    return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  }

  formatearFecha(fecha: string): string {
    const d = new Date(fecha);
    return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()} ${this.formatearHora(fecha)}`;
  }

  badgeClase(estado: string): string {
    return estado === 'EXITOSO' ? 'badge-ok' : estado === 'FALLIDO' ? 'badge-error'
      : estado === 'EN_PROCESO' ? 'badge-warning' : 'badge-info';
  }

  contarBackupsMes(cal: CalendarMonth): number {
    return cal.days.filter(d => d.hasBackup && !d.isOtherMonth).length;
  }

  get esRestoreDesdeTimeline(): boolean { return this.nodoTLSeleccionado !== null; }
}
