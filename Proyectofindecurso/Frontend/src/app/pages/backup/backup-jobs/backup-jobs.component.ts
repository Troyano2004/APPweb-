import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BackupService,
  BackupJobDto,
  BackupJobRequest,
  BackupDestinationRequest,
  BackupExecutionResponse,
  TipoDestino,
  EstadoEjecucion
} from '../../../services/backup.service';

@Component({
  selector: 'app-backup-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backup-jobs.component.html',
  styleUrls: ['./backup-jobs.component.scss']
})
export class BackupJobsComponent implements OnInit {

  jobs:            BackupJobDto[]            = [];
  jobSeleccionado: BackupJobDto | null        = null;
  historial:       BackupExecutionResponse[] = [];
  zonasHorarias:   string[]                  = [];

  // Bases de datos
  databasesDisponibles:   string[] = [];
  databasesSeleccionadas: string[] = [];
  cargandoDatabases = false;
  errorDatabases    = '';

  // ── Scheduler ──────────────────────────────────────────────────────────────
  // El modelo mental: UN FULL como base, diferenciales diarios entre FULLs.
  // Ejemplo clásico: FULL semanal (domingos), DIFE diario (lunes–sábado).

  tipoFrecuenciaFull: 'dias' | 'semanas' | 'meses' | 'anual' = 'semanas';

  // FULL — cuándo hacerlo
  schedFullHoraInicio = '02:00';   // hora del FULL (y de los diferenciales)
  schedFullCadaDias   = 7;         // cada N días (modo "dias")
  schedFullCadaSemanas = 1;        // cada N semanas (modo "semanas")
  schedFullCadaMeses  = 1;         // cada N meses (modo "meses")
  schedFullDiaMes     = 1;         // día del mes para modos meses/anual
  schedFullDiaSemana  = 0;         // día de semana del FULL (0=Dom…6=Sáb) para modo semanas

  // DIFERENCIAL
  schedDifCadaDias    = 1;         // cada N días (siempre relativo al FULL)
  ventanaActiva       = false;

  diasSemana = [
    { label: 'Dom', value: 0 }, { label: 'Lun', value: 1 },
    { label: 'Mar', value: 2 }, { label: 'Mié', value: 3 },
    { label: 'Jue', value: 4 }, { label: 'Vie', value: 5 },
    { label: 'Sáb', value: 6 },
  ];

  proximasEjecuciones: { fecha: string; tipo: 'Full' | 'Dif' }[] = [];

  // UI
  cargando          = false;
  cargandoRun       = false;
  cargandoTest      = false;
  mensajeTest       = '';
  exitoTest         = false;
  mostrarFormulario = false;
  mostrarHistorial  = false;
  modoEdicion       = false;
  destinoActivo: TipoDestino = 'LOCAL';

  form:         BackupJobRequest         = this.formVacio();
  destinoLocal: BackupDestinationRequest = this.destinoVacio('LOCAL');
  destinoAzure: BackupDestinationRequest = this.destinoVacio('AZURE');
  destinoS3:    BackupDestinationRequest = this.destinoVacio('S3');
  destinoDrive: BackupDestinationRequest = this.destinoVacio('GOOGLE_DRIVE');
  cargandoOAuth   = false;
  carpetasDrive:  { id: string; name: string }[] = [];
  cargandoCarpetas = false;
  mostrarSelectorCarpetas = false;

  constructor(
    private svc: BackupService,
    private cdr: ChangeDetectorRef,
    private zone: NgZone
  ) {}

  ngOnInit(): void {
    this.cargarJobs();
    this.svc.zonasHorarias().subscribe(z => {
      this.zone.run(() => { this.zonasHorarias = z; this.cdr.detectChanges(); });
    });
    this.recalcularCron();
  }

  // ── LISTA ──────────────────────────────────────────────────────────────────

  cargarJobs(): void {
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

  nuevoJob(): void {
    this.resetForm();
    this.modoEdicion = false; this.jobSeleccionado = null;
    this.mostrarFormulario = true; this.mostrarHistorial = false;
    this.cdr.detectChanges();
  }

  editarJob(job: BackupJobDto): void {
    this.cargando = true; this.mostrarFormulario = false;
    this.cdr.detectChanges();
    this.svc.obtenerJob(job.idJob).subscribe({
      next: j => this.zone.run(() => {
        this.cargarEnFormulario(j);
        this.modoEdicion = true; this.jobSeleccionado = j;
        this.mostrarFormulario = true; this.mostrarHistorial = false;
        this.cargando = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false; this.cdr.detectChanges();
      })
    });
  }

  private cargarEnFormulario(j: BackupJobDto): void {
    this.form = {
      nombre:               j.nombre        ?? '',
      pgDumpPath:           j.pgDumpPath    ?? '',
      pgHost:               j.pgHost        ?? 'localhost',
      pgPort:               j.pgPort        ?? 5432,
      pgUsuario:            j.pgUsuario     ?? '',
      pgPassword:           '',
      databases:            j.databases     ?? '',
      comprimir:            j.comprimir     ?? true,
      cronFull:             j.cronFull      ?? '0 0 2 * * 0',
      cronDiferencial:      j.cronDiferencial ?? '',
      diferencialActivo:    j.diferencialActivo ?? false,
      zonaHoraria:          j.zonaHoraria   ?? 'America/Guayaquil',
      ventanaExcluirInicio: j.ventanaExcluirInicio ?? '',
      ventanaExcluirFin:    j.ventanaExcluirFin    ?? '',
      maxReintentos:        j.maxReintentos  ?? 2,
      emailExito:           j.emailExito     ?? '',
      emailFallo:           j.emailFallo     ?? '',
      activo:               j.activo         ?? true,
      destinos:             []
    };

    this.databasesSeleccionadas = (j.databases ?? '')
      .split(',').map(d => d.trim()).filter(d => d.length > 0);
    this.databasesDisponibles   = [...this.databasesSeleccionadas];
    this.errorDatabases = ''; this.mensajeTest = '';

    this.destinoLocal = this.destinoVacio('LOCAL');
    this.destinoAzure = this.destinoVacio('AZURE');
    this.destinoS3    = this.destinoVacio('S3');
    this.destinoDrive = this.destinoVacio('GOOGLE_DRIVE');

    for (const d of (j.destinos ?? [])) {
      if (d.tipo === 'LOCAL')        Object.assign(this.destinoLocal, d);
      if (d.tipo === 'AZURE')        Object.assign(this.destinoAzure, d);
      if (d.tipo === 'S3')           Object.assign(this.destinoS3, d);
      if (d.tipo === 'GOOGLE_DRIVE') Object.assign(this.destinoDrive, d);
    }

    this.recalcularCron();
  }

  guardar(): void {
    this.form.databases = this.databasesSeleccionadas.join(',');
    const destinos: BackupDestinationRequest[] = [];
    if (this.destinoLocal.activo)  destinos.push({ ...this.destinoLocal });
    if (this.destinoAzure.activo)  destinos.push({ ...this.destinoAzure });
    if (this.destinoS3.activo)     destinos.push({ ...this.destinoS3 });
    if (this.destinoDrive.activo)  destinos.push({ ...this.destinoDrive });
    this.form.destinos = destinos;

    this.cargando = true; this.cdr.detectChanges();

    const op = this.modoEdicion
      ? this.svc.actualizarJob(this.jobSeleccionado!.idJob, this.form)
      : this.svc.crearJob(this.form);

    op.subscribe({
      next: () => this.zone.run(() => {
        this.mostrarFormulario = false; this.cargando = false; this.cargarJobs();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false; this.cdr.detectChanges();
      })
    });
  }

  cancelar(): void { this.mostrarFormulario = false; this.mensajeTest = ''; this.cdr.detectChanges(); }

  eliminar(job: BackupJobDto): void {
    if (!confirm(`¿Eliminar "${job.nombre}"? No se puede deshacer.`)) return;
    this.svc.eliminarJob(job.idJob).subscribe(() =>
      this.zone.run(() => this.cargarJobs())
    );
  }

  toggleActivo(job: BackupJobDto): void {
    this.svc.toggleEstado(job.idJob, !job.activo).subscribe(() =>
      this.zone.run(() => this.cargarJobs())
    );
  }

  ejecutarAhora(job: BackupJobDto): void {
    if (!confirm(`¿Ejecutar backup FULL de "${job.nombre}" ahora?`)) return;
    this.cargandoRun = true; this.cdr.detectChanges();
    this.svc.ejecutarAhora(job.idJob).subscribe({
      next: () => this.zone.run(() => { this.cargandoRun = false; this.cargarJobs(); }),
      error: () => this.zone.run(() => { this.cargandoRun = false; this.cdr.detectChanges(); })
    });
  }

  verHistorial(job: BackupJobDto): void {
    this.jobSeleccionado = job; this.mostrarHistorial = true;
    this.mostrarFormulario = false; this.cdr.detectChanges();
    this.svc.historialJob(job.idJob).subscribe(h =>
      this.zone.run(() => { this.historial = h; this.cdr.detectChanges(); })
    );
  }

  cerrarHistorial(): void {
    this.mostrarHistorial = false; this.historial = []; this.cdr.detectChanges();
  }

  // ── TESTS ──────────────────────────────────────────────────────────────────

  probarPostgres(): void {
    this.cargandoTest = true; this.mensajeTest = ''; this.cdr.detectChanges();
    this.svc.probarPostgres(this.form.pgHost, this.form.pgPort,
      this.form.pgUsuario, this.form.pgPassword ?? '').subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest = r.exitoso; this.mensajeTest = r.mensaje;
        this.cargandoTest = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest = false; this.mensajeTest = 'Error al conectar';
        this.cargandoTest = false; this.cdr.detectChanges();
      })
    });
  }

  cargarDatabases(): void {
    if (!this.form.pgHost || !this.form.pgUsuario || !this.form.pgPassword) {
      this.errorDatabases = 'Completa host, usuario y contraseña primero';
      this.cdr.detectChanges(); return;
    }
    this.cargandoDatabases = true; this.errorDatabases = ''; this.cdr.detectChanges();
    this.svc.listarDatabases(this.form.pgHost, this.form.pgPort,
      this.form.pgUsuario, this.form.pgPassword ?? '').subscribe({
      next: r => this.zone.run(() => {
        if (r.exitoso) {
          this.databasesDisponibles   = r.databases;
          this.databasesSeleccionadas = this.databasesSeleccionadas.filter(d => r.databases.includes(d));
        } else {
          this.errorDatabases = r.mensaje ?? 'No se pudo cargar';
        }
        this.cargandoDatabases = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.errorDatabases = 'Error al conectar con PostgreSQL';
        this.cargandoDatabases = false; this.cdr.detectChanges();
      })
    });
  }

  toggleDatabase(db: string): void {
    this.databasesSeleccionadas = this.databasesSeleccionadas.includes(db)
      ? this.databasesSeleccionadas.filter(d => d !== db)
      : [...this.databasesSeleccionadas, db];
  }

  isDatabaseSeleccionada(db: string): boolean {
    return this.databasesSeleccionadas.includes(db);
  }

  probarDestino(tipo: TipoDestino): void {
    this.cargandoTest = true; this.mensajeTest = ''; this.cdr.detectChanges();
    let payload: any = { tipo };
    if (tipo === 'LOCAL') payload.rutaLocal = this.destinoLocal.rutaLocal;
    if (tipo === 'AZURE') {
      payload.azureAccount   = this.destinoAzure.azureAccount;
      payload.azureKey       = this.destinoAzure.azureKey;
      payload.azureContainer = this.destinoAzure.azureContainer;
    }
    if (tipo === 'S3') {
      payload.s3Bucket    = this.destinoS3.s3Bucket;
      payload.s3Region    = this.destinoS3.s3Region;
      payload.s3AccessKey = this.destinoS3.s3AccessKey;
      payload.s3SecretKey = this.destinoS3.s3SecretKey;
    }
    this.svc.probarDestino(payload).subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest = r.exitoso; this.mensajeTest = r.mensaje;
        this.cargandoTest = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest = false; this.mensajeTest = 'Error al probar el destino';
        this.cargandoTest = false; this.cdr.detectChanges();
      })
    });
  }

  probarEmail(): void {
    const email = this.form.emailExito || this.form.emailFallo;
    if (!email) { this.mensajeTest = 'Ingresa un email primero'; this.exitoTest = false; this.cdr.detectChanges(); return; }
    this.cargandoTest = true; this.cdr.detectChanges();
    this.svc.probarEmail(email).subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest = r.exitoso; this.mensajeTest = r.mensaje;
        this.cargandoTest = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest = false; this.mensajeTest = 'Error al enviar email';
        this.cargandoTest = false; this.cdr.detectChanges();
      })
    });
  }

  // ── SCHEDULER ──────────────────────────────────────────────────────────────

  /**
   * Construye los crons y la preview de próximas ejecuciones.
   *
   * Modelo: UN FULL como base → diferenciales hasta el siguiente FULL.
   *
   *  FULL semanal (domingos 02:00) + DIFE cada 1 día:
   *    Dom 02:00 FULL
   *    Lun 02:00 DIFE
   *    Mar 02:00 DIFE
   *    ...
   *    Sáb 02:00 DIFE
   *    Dom 02:00 FULL  ← nueva base
   */
  recalcularCron(): void {
    const [h, m] = this.parseHora(this.schedFullHoraInicio);

    // ── Cron FULL ──────────────────────────────────────────────────────────
    switch (this.tipoFrecuenciaFull) {
      case 'dias':
        this.form.cronFull = this.schedFullCadaDias === 1
          ? `0 ${m} ${h} * * *`
          : `0 ${m} ${h} */${this.schedFullCadaDias} * *`;
        break;

      case 'semanas':
        // Día de semana específico cada N semanas
        this.form.cronFull = this.schedFullCadaSemanas === 1
          ? `0 ${m} ${h} * * ${this.schedFullDiaSemana}`
          : `0 ${m} ${h} */${this.schedFullCadaSemanas * 7} * ${this.schedFullDiaSemana}`;
        break;

      case 'meses':
        this.form.cronFull = this.schedFullCadaMeses === 1
          ? `0 ${m} ${h} ${this.schedFullDiaMes} * *`
          : `0 ${m} ${h} ${this.schedFullDiaMes} */${this.schedFullCadaMeses} *`;
        break;

      case 'anual':
        this.form.cronFull = `0 ${m} ${h} ${this.schedFullDiaMes} 1 *`;
        break;
    }

    // ── Cron DIFERENCIAL ───────────────────────────────────────────────────
    // El diferencial se ejecuta cada N días a la misma hora que el FULL.
    // El sistema backend se encarga de saltar si ese día toca FULL.
    if (this.form.diferencialActivo) {
      const cadaDias = Math.max(1, this.schedDifCadaDias);
      this.form.cronDiferencial = cadaDias === 1
        ? `0 ${m} ${h} * * *`          // todos los días → el backend filtra el día del FULL
        : `0 ${m} ${h} */${cadaDias} * *`;
    } else {
      this.form.cronDiferencial = '';
    }

    this.calcularProximasEjecuciones();
    this.cdr.detectChanges();
  }

  private parseHora(s: string): [number, number] {
    const p = (s || '02:00').split(':');
    return [parseInt(p[0], 10) || 0, parseInt(p[1], 10) || 0];
  }

  /**
   * Genera la preview de próximas 12 ejecuciones mostrando la cadena
   * FULL → DIFE → DIFE → ... → FULL → DIFE → ...
   */
  private calcularProximasEjecuciones(): void {
    const result: { fecha: string; tipo: 'Full' | 'Dif' }[] = [];
    const ahora = new Date();
    const [h, m] = this.parseHora(this.schedFullHoraInicio);

    const fmt = (d: Date) =>
      `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/` +
      `${d.getFullYear()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;

    // Calcular las próximas fechas de FULL
    const fullDates: Date[] = [];
    if (this.tipoFrecuenciaFull === 'semanas') {
      // Buscar el próximo día de semana correcto
      let c = new Date(ahora);
      c.setHours(h, m, 0, 0);
      for (let i = 0; i < 60; i++) {
        if (c > ahora && c.getDay() === this.schedFullDiaSemana) {
          fullDates.push(new Date(c)); break;
        }
        c.setDate(c.getDate() + 1);
      }
      // Agregar siguientes FULLs (cada N semanas)
      for (let i = 1; fullDates.length < 4; i++) {
        const next = new Date(fullDates[0]);
        next.setDate(next.getDate() + (this.schedFullCadaSemanas * 7 * i));
        fullDates.push(next);
      }
    } else if (this.tipoFrecuenciaFull === 'dias') {
      let c = new Date(ahora); c.setHours(h, m, 0, 0);
      if (c <= ahora) c.setDate(c.getDate() + this.schedFullCadaDias);
      for (let i = 0; fullDates.length < 4; i++) {
        fullDates.push(new Date(c));
        c.setDate(c.getDate() + this.schedFullCadaDias);
      }
    } else if (this.tipoFrecuenciaFull === 'meses') {
      let c = new Date(ahora.getFullYear(), ahora.getMonth(), this.schedFullDiaMes, h, m, 0);
      if (c <= ahora) c.setMonth(c.getMonth() + this.schedFullCadaMeses);
      for (let i = 0; fullDates.length < 3; i++) {
        fullDates.push(new Date(c));
        c.setMonth(c.getMonth() + this.schedFullCadaMeses);
      }
    } else {
      let c = new Date(ahora.getFullYear(), 0, this.schedFullDiaMes, h, m, 0);
      if (c <= ahora) c.setFullYear(c.getFullYear() + 1);
      for (let i = 0; fullDates.length < 2; i++) {
        fullDates.push(new Date(c)); c.setFullYear(c.getFullYear() + 1);
      }
    }

    // Generar la cadena FULL → DIFEs → FULL → DIFEs...
    const cadaDias = Math.max(1, this.schedDifCadaDias);

    for (let fi = 0; fi < fullDates.length && result.length < 12; fi++) {
      const fullDate   = fullDates[fi];
      const nextFull   = fullDates[fi + 1];

      result.push({ fecha: fmt(fullDate), tipo: 'Full' });

      // Solo insertar diferenciales si está activado y hay un siguiente FULL
      if (this.form.diferencialActivo && nextFull) {
        let dif = new Date(fullDate);
        dif.setDate(dif.getDate() + cadaDias);

        while (dif < nextFull && result.length < 12) {
          result.push({ fecha: fmt(dif), tipo: 'Dif' });
          dif.setDate(dif.getDate() + cadaDias);
        }
      }
    }

    this.proximasEjecuciones = result;
  }

  private enVentana(d: Date): boolean {
    if (!this.ventanaActiva || !this.form.ventanaExcluirInicio || !this.form.ventanaExcluirFin) return false;
    const hc  = d.getHours() * 60 + d.getMinutes();
    const ini = this.form.ventanaExcluirInicio.split(':').map(Number);
    const fin = this.form.ventanaExcluirFin.split(':').map(Number);
    return hc >= ini[0] * 60 + ini[1] && hc <= fin[0] * 60 + fin[1];
  }

  // ── Google Drive OAuth ─────────────────────────────────────────────────────

  conectarGoogleDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? []).find(d => d.tipo === 'GOOGLE_DRIVE');
    if (driveDestino?.idDestination) {
      this.abrirVentanaOAuth(driveDestino.idDestination);
    } else if (this.jobSeleccionado) {
      this.cargandoOAuth = true; this.cdr.detectChanges();
      this.guardarYConectar();
    } else {
      this.mensajeTest = 'Guarda el job primero, luego edítalo y conecta Google Drive.';
      this.exitoTest = false; this.cdr.detectChanges();
    }
  }

  private guardarYConectar(): void {
    this.form.databases = this.databasesSeleccionadas.join(',');
    const destinos: BackupDestinationRequest[] = [];
    if (this.destinoLocal.activo)  destinos.push({ ...this.destinoLocal });
    if (this.destinoAzure.activo)  destinos.push({ ...this.destinoAzure });
    if (this.destinoS3.activo)     destinos.push({ ...this.destinoS3 });
    if (this.destinoDrive.activo)  destinos.push({ ...this.destinoDrive });
    this.form.destinos = destinos;

    const op = this.modoEdicion
      ? this.svc.actualizarJob(this.jobSeleccionado!.idJob, this.form)
      : this.svc.crearJob(this.form);

    op.subscribe({
      next: jobGuardado => this.zone.run(() => {
        this.jobSeleccionado = jobGuardado; this.modoEdicion = true;
        this.svc.obtenerJob(jobGuardado.idJob).subscribe({
          next: jobCompleto => this.zone.run(() => {
            this.jobSeleccionado = jobCompleto;
            const driveGuardado = (jobCompleto.destinos ?? []).find(d => d.tipo === 'GOOGLE_DRIVE');
            if (driveGuardado) Object.assign(this.destinoDrive, driveGuardado);
            const id = driveGuardado?.idDestination;
            if (id) { this.abrirVentanaOAuth(id); }
            else { this.cargandoOAuth = false; this.mensajeTest = 'No se pudo obtener el ID del destino Google Drive'; this.exitoTest = false; this.cdr.detectChanges(); }
          }),
          error: () => this.zone.run(() => { this.cargandoOAuth = false; this.cdr.detectChanges(); })
        });
      }),
      error: () => this.zone.run(() => {
        this.cargandoOAuth = false; this.mensajeTest = 'Error guardando el job'; this.exitoTest = false; this.cdr.detectChanges();
      })
    });
  }

  private abrirVentanaOAuth(destinationId: number): void {
    this.svc.iniciarOAuthDrive(destinationId).subscribe({
      next: r => this.zone.run(() => {
        const popup = window.open(r.url, 'google-oauth', 'width=500,height=600,scrollbars=yes,resizable=yes');
        const handler = (event: MessageEvent) => {
          if (event.data?.type === 'GDRIVE_CONNECTED') {
            window.removeEventListener('message', handler);
            this.zone.run(() => {
              this.destinoDrive.gdriveConectado = true; this.destinoDrive.gdriveCuenta = event.data.email;
              this.cargandoOAuth = false; this.mensajeTest = `Cuenta conectada: ${event.data.email}`; this.exitoTest = true; this.cdr.detectChanges();
            });
          } else if (event.data?.type === 'GDRIVE_ERROR') {
            window.removeEventListener('message', handler);
            this.zone.run(() => {
              this.cargandoOAuth = false; this.mensajeTest = `Error: ${event.data.mensaje}`; this.exitoTest = false; this.cdr.detectChanges();
            });
          }
        };
        window.addEventListener('message', handler);
        const timer = setInterval(() => {
          if (popup?.closed) { clearInterval(timer); window.removeEventListener('message', handler); this.zone.run(() => { this.cargandoOAuth = false; this.cdr.detectChanges(); }); }
        }, 1000);
        this.cargandoOAuth = false; this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargandoOAuth = false; this.mensajeTest = 'Error iniciando OAuth con Google'; this.exitoTest = false; this.cdr.detectChanges();
      })
    });
  }

  cargarCarpetasDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? []).find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;
    this.cargandoCarpetas = true; this.mostrarSelectorCarpetas = true; this.cdr.detectChanges();
    this.svc.listarCarpetasDrive(driveDestino.idDestination).subscribe({
      next: r => this.zone.run(() => { this.carpetasDrive = r.exitoso ? r.carpetas : []; this.cargandoCarpetas = false; this.cdr.detectChanges(); }),
      error: () => this.zone.run(() => { this.cargandoCarpetas = false; this.cdr.detectChanges(); })
    });
  }

  seleccionarCarpeta(carpeta: { id: string; name: string }): void {
    this.destinoDrive.gdriveFolderNombre = carpeta.name; this.destinoDrive.gdriveFolderId = carpeta.id;
    this.mostrarSelectorCarpetas = false; this.cdr.detectChanges();
  }

  desconectarGoogleDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? []).find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;
    this.svc.desconectarDrive(driveDestino.idDestination).subscribe(() =>
      this.zone.run(() => {
        this.destinoDrive.gdriveConectado = false; this.destinoDrive.gdriveCuenta = '';
        this.mensajeTest = 'Cuenta de Google Drive desconectada'; this.exitoTest = true; this.cdr.detectChanges();
      })
    );
  }

  probarGoogleDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? []).find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;
    this.cargandoTest = true; this.cdr.detectChanges();
    this.svc.probarDrive(driveDestino.idDestination).subscribe({
      next: r => this.zone.run(() => { this.exitoTest = r.exitoso; this.mensajeTest = r.mensaje; this.cargandoTest = false; this.cdr.detectChanges(); }),
      error: () => this.zone.run(() => { this.exitoTest = false; this.mensajeTest = 'Error probando Google Drive'; this.cargandoTest = false; this.cdr.detectChanges(); })
    });
  }

  onCarpetaSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const ruta = (input.files[0] as any).webkitRelativePath as string || input.files[0].name;
    this.destinoLocal.rutaLocal = ruta.split('/')[0];
    input.value = ''; this.cdr.detectChanges();
  }

  onPgDumpSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    this.form.pgDumpPath = input.files[0].name;
    input.value = ''; this.cdr.detectChanges();
  }

  // ── Helpers etiquetas ──────────────────────────────────────────────────────

  get labelDiaSemanaFull(): string {
    return this.diasSemana.find(d => d.value === this.schedFullDiaSemana)?.label ?? 'Dom';
  }

  get descripcionCadena(): string {
    const [h] = this.parseHora(this.schedFullHoraInicio);
    const hora = this.schedFullHoraInicio;
    if (!this.form.diferencialActivo) {
      return `Un FULL cada vez que se ejecute, sin diferenciales.`;
    }
    const cadaDias = this.schedDifCadaDias;
    const plural   = cadaDias === 1 ? 'día' : `${cadaDias} días`;
    switch (this.tipoFrecuenciaFull) {
      case 'semanas':
        return `FULL cada ${this.schedFullCadaSemanas === 1 ? 'semana' : this.schedFullCadaSemanas + ' semanas'} los ${this.labelDiaSemanaFull} a las ${hora}. Diferenciales cada ${plural} entre FULLs.`;
      case 'dias':
        return `FULL cada ${this.schedFullCadaDias} día(s) a las ${hora}. Diferenciales cada ${plural} entre FULLs.`;
      case 'meses':
        return `FULL el día ${this.schedFullDiaMes} de cada ${this.schedFullCadaMeses === 1 ? 'mes' : this.schedFullCadaMeses + ' meses'} a las ${hora}. Diferenciales cada ${plural}.`;
      default:
        return `FULL anual el 1 de enero. Diferenciales cada ${plural}.`;
    }
  }

  // ── Helpers UI ─────────────────────────────────────────────────────────────

  formatearTamano(bytes?: number): string {
    if (!bytes) return '-';
    if (bytes < 1024)               return `${bytes} B`;
    if (bytes < 1024 * 1024)        return `${(bytes/1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes/1048576).toFixed(1)} MB`;
    return `${(bytes/1073741824).toFixed(1)} GB`;
  }

  badgeEstado(estado?: EstadoEjecucion): string {
    const map: Record<string, string> = {
      EXITOSO: 'badge-ok', FALLIDO: 'badge-error',
      EN_PROCESO: 'badge-warning', PENDIENTE: 'badge-info'
    };
    return map[estado ?? ''] ?? 'badge-info';
  }

  setDestinoActivo(tipo: TipoDestino): void { this.destinoActivo = tipo; }
  destinosJob(job: BackupJobDto): any[] { return job.destinos ?? []; }

  // ── Privados ───────────────────────────────────────────────────────────────

  private resetForm(): void {
    this.form                   = this.formVacio();
    this.destinoLocal           = this.destinoVacio('LOCAL');
    this.destinoAzure           = this.destinoVacio('AZURE');
    this.destinoS3              = this.destinoVacio('S3');
    this.destinoDrive           = this.destinoVacio('GOOGLE_DRIVE');
    this.databasesDisponibles   = [];
    this.databasesSeleccionadas = [];
    this.errorDatabases         = '';
    this.mensajeTest            = '';
    this.tipoFrecuenciaFull     = 'semanas';
    this.schedFullHoraInicio    = '02:00';
    this.schedFullCadaSemanas   = 1;
    this.schedFullDiaSemana     = 0;
    this.schedFullCadaDias      = 7;
    this.schedFullCadaMeses     = 1;
    this.schedFullDiaMes        = 1;
    this.schedDifCadaDias       = 1;
    this.ventanaActiva          = false;
    this.recalcularCron();
  }

  private formVacio(): BackupJobRequest {
    return {
      nombre: '', pgDumpPath: '', pgHost: 'localhost', pgPort: 5432,
      pgUsuario: '', pgPassword: '', databases: '', comprimir: true,
      cronFull: '0 0 2 * * 0', cronDiferencial: '',
      diferencialActivo: false, zonaHoraria: 'America/Guayaquil',
      ventanaExcluirInicio: '', ventanaExcluirFin: '',
      maxReintentos: 2, emailExito: '', emailFallo: '',
      activo: true, destinos: []
    };
  }

  private destinoVacio(tipo: TipoDestino): BackupDestinationRequest {
    return {
      idDestination: null, tipo, activo: false,
      rutaLocal: '', azureAccount: '', azureKey: '', azureContainer: '',
      gdriveCuenta: '', gdriveFolderId: '', gdriveFolderNombre: '',
      s3Bucket: '', s3Region: 'us-east-1', s3AccessKey: '', s3SecretKey: '',
      retencionMeses: 6, retencionDias: 0, maxBackups: 0
    };
  }
}
