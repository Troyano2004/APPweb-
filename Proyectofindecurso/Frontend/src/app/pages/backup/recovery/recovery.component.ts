import { Component, OnInit, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

interface BackupItem {
  fileId:        string;
  nombre:        string;
  tamano:        string;
  fechaCreacion: string;
  tipo:          string;
  fuente:        'DRIVE' | 'LOCAL';
}

interface Job {
  idJob:        number;
  nombre:       string;
  databases:    string;
  tieneDrive:   boolean;
  esEmergencia: boolean;
}

type Paso   = 'auth' | 'jobs' | 'backups' | 'restore' | 'progreso' | 'resultado';
type Fuente = 'DRIVE' | 'LOCAL';

@Component({
  selector: 'app-recovery',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recovery.component.html',
  styleUrl: './recovery.component.scss'
})
export class RecoveryComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/recovery';

  // ── Estado ──────────────────────────────────────────────────────
  paso   = signal<Paso>('auth');
  fuente = signal<Fuente>('DRIVE');

  // Formularios
  password      = '';
  jobId: number | null = null;
  modoRestore   = 'REEMPLAZAR';
  nombreBdNueva = '';

  // Datos
  jobs:    Job[]        = [];
  backups: BackupItem[] = [];
  backupSeleccionado: BackupItem | null = null;
  jobSeleccionado:    Job | null        = null;

  // Feedback
  errorAuth       = '';
  errorJobs       = '';
  pingOk: boolean | null = null;
  cargandoBackups = false;
  errorBackups    = '';
  resultado: any  = null;
  logRestore      = '';
  horaActual      = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.horaActual = new Date().toLocaleString('es-EC');
    setInterval(() => {
      this.horaActual = new Date().toLocaleString('es-EC');
      this.cdr.markForCheck();
    }, 1000);
  }

  // ── Fuente ───────────────────────────────────────────────────────

  seleccionarFuente(f: Fuente): void {
    this.fuente.set(f);
    this.backups            = [];
    this.backupSeleccionado = null;
    this.errorBackups       = '';
  }

  // ── Paso 1: Auth ─────────────────────────────────────────────────

  autenticar(): void {
    if (!this.password.trim()) { this.errorAuth = 'Ingresa la contraseña'; return; }
    this.errorAuth = '';

    this.http.post<{ ok: boolean; mensaje: string }>(
      `${this.API}/auth`, { password: this.password }
    ).subscribe({
      next: (res) => {
        if (res.ok) {
          this.paso.set('jobs');
          this.cargarJobs();
        } else {
          this.errorAuth = res.mensaje || 'Contraseña incorrecta';
          this.password  = '';
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorAuth = 'Error conectando con el servidor';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Paso 2: Jobs ─────────────────────────────────────────────────

  cargarJobs(): void {
    this.http.get<Job[]>(`${this.API}/jobs`).subscribe({
      next: (jobs) => {
        this.jobs = [...jobs];
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorJobs = 'Error cargando jobs';
        this.cdr.detectChanges();
      }
    });
  }

  seleccionarJob(job: Job): void {
    this.jobSeleccionado = { ...job };
    this.jobId           = job.idJob;
    this.pingOk          = null;
    this.cdr.detectChanges();

    this.http.get<{ ok: boolean }>(`${this.API}/ping/${job.idJob}`).subscribe({
      next: (res) => {
        this.pingOk = res.ok;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pingOk = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarBackups(): void {
    if (!this.jobId) return;

    this.backups         = [];
    this.cargandoBackups = true;
    this.errorBackups    = '';
    this.paso.set('backups');
    this.cdr.detectChanges();

    const fuenteActual = this.fuente();
    const endpoint = fuenteActual === 'DRIVE'
      ? `${this.API}/backups/${this.jobId}`
      : `${this.API}/backups-local/${this.jobId}`;

    this.http.get<any[]>(endpoint).subscribe({
      next: (items) => {
        // Crear nuevo array con fuente asignada — forzar nueva referencia
        this.backups         = items.map(b => ({ ...b, fuente: fuenteActual } as BackupItem));
        this.cargandoBackups = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorBackups    = err?.error?.error || 'Error cargando backups';
        this.cargandoBackups = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Paso 3: Selección backup ─────────────────────────────────────

  seleccionarBackup(b: BackupItem): void {
    this.backupSeleccionado = { ...b };
    this.paso.set('restore');
    this.cdr.detectChanges();
  }

  volverABackups(): void {
    this.backupSeleccionado = null;
    this.paso.set('backups');
    this.cdr.detectChanges();
  }

  // ── Paso 4: Restore ──────────────────────────────────────────────

  confirmarRestore(): void {
    const b = this.backupSeleccionado;
    if (!b) return;
    if (this.modoRestore === 'NUEVA_BD' && !this.nombreBdNueva.trim()) return;

    this.paso.set('progreso');
    this.cdr.detectChanges();

    const body = {
      idJob:         this.jobId,
      fileId:        b.fileId,
      nombreArchivo: b.nombre,
      modo:          this.modoRestore,
      nombreBdNueva: this.nombreBdNueva,
      fuente:        b.fuente
    };

    this.http.post<any>(`${this.API}/restore`, body).subscribe({
      next: (res) => {
        this.resultado  = res;
        this.logRestore = res.log || '';
        this.paso.set('resultado');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.resultado = { exitoso: false, mensaje: err.error?.mensaje || 'Error inesperado' };
        this.paso.set('resultado');
        this.cdr.detectChanges();
      }
    });
  }

  // ── Utilidades ───────────────────────────────────────────────────

  reiniciar(): void {
    this.backupSeleccionado = null;
    this.backups            = [];
    this.resultado          = null;
    this.logRestore         = '';
    this.modoRestore        = 'REEMPLAZAR';
    this.nombreBdNueva      = '';
    this.paso.set('backups');
    this.cdr.detectChanges();
  }

  irAlSistema(): void {
    this.router.navigate(['/login']);
  }

  formatFecha(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-EC', { dateStyle: 'short', timeStyle: 'short' });
  }

  formatBytes(bytes: string): string {
    const n = parseInt(bytes) || 0;
    if (n === 0) return '—';
    if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB';
    return (n / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
