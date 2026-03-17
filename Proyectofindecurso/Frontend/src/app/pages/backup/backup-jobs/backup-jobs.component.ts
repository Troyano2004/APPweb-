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

  // Scheduler
  tipoFrecuencia: 'horas' | 'dias' | 'semanas' | 'meses' | 'anual' = 'horas';
  schedFullHoras       = 12;
  schedFullMinutos     = 0;
  schedFullHoraInicio  = '21:00';
  schedFullDiasSemana  = 1;
  schedFullSemanas     = 1;
  schedFullMeses       = 1;
  schedDiaMes          = 1;
  schedDifHoras        = 6;
  ventanaActiva        = false;
  diasActivos: number[] = [0, 1, 2, 3, 4, 5, 6];

  diasSemana = [
    { label: 'Dom', value: 0 }, { label: 'Lun', value: 1 },
    { label: 'Mar', value: 2 }, { label: 'Mié', value: 3 },
    { label: 'Jue', value: 4 }, { label: 'Vie', value: 5 },
    { label: 'Sáb', value: 6 },
  ];

  proximasEjecuciones: { fecha: string; tipo: string }[] = [];

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
        this.jobs     = jobs;
        this.cargando = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false;
        this.cdr.detectChanges();
      })
    });
  }

  // ── NUEVO ──────────────────────────────────────────────────────────────────

  nuevoJob(): void {
    this.resetForm();
    this.modoEdicion       = false;
    this.jobSeleccionado   = null;
    this.mostrarFormulario = true;
    this.mostrarHistorial  = false;
    this.cdr.detectChanges();
  }

  // ── EDITAR ─────────────────────────────────────────────────────────────────

  editarJob(job: BackupJobDto): void {
    this.cargando         = true;
    this.mostrarFormulario = false;
    this.cdr.detectChanges();

    this.svc.obtenerJob(job.idJob).subscribe({
      next: j => this.zone.run(() => {
        this.cargarEnFormulario(j);
        this.modoEdicion       = true;
        this.jobSeleccionado   = j;
        this.mostrarFormulario = true;
        this.mostrarHistorial  = false;
        this.cargando          = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false;
        this.cdr.detectChanges();
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
      cronFull:             j.cronFull      ?? '0 0 */12 * * *',
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
    this.errorDatabases         = '';
    this.mensajeTest            = '';

    this.destinoLocal = this.destinoVacio('LOCAL');
    this.destinoAzure = this.destinoVacio('AZURE');
    this.destinoS3    = this.destinoVacio('S3');
    this.destinoDrive = this.destinoVacio('GOOGLE_DRIVE');

    for (const d of (j.destinos ?? [])) {
      if (d.tipo === 'LOCAL')         Object.assign(this.destinoLocal,  d);
      if (d.tipo === 'AZURE')         Object.assign(this.destinoAzure,  d);
      if (d.tipo === 'S3')            Object.assign(this.destinoS3,     d);
      if (d.tipo === 'GOOGLE_DRIVE')  Object.assign(this.destinoDrive,  d);
    }

    this.recalcularCron();
  }

  // ── GUARDAR ────────────────────────────────────────────────────────────────

  guardar(): void {
    this.form.databases = this.databasesSeleccionadas.join(',');
    const destinos: BackupDestinationRequest[] = [];
    if (this.destinoLocal.activo)  destinos.push({ ...this.destinoLocal });
    if (this.destinoAzure.activo)  destinos.push({ ...this.destinoAzure });
    if (this.destinoS3.activo)     destinos.push({ ...this.destinoS3 });
    if (this.destinoDrive.activo)  destinos.push({ ...this.destinoDrive });
    this.form.destinos = destinos;

    this.cargando = true;
    this.cdr.detectChanges();

    const op = this.modoEdicion
      ? this.svc.actualizarJob(this.jobSeleccionado!.idJob, this.form)
      : this.svc.crearJob(this.form);

    op.subscribe({
      next: () => this.zone.run(() => {
        this.mostrarFormulario = false;
        this.cargando          = false;
        this.cargarJobs();
      }),
      error: () => this.zone.run(() => {
        this.cargando = false;
        this.cdr.detectChanges();
      })
    });
  }

  cancelar(): void {
    this.mostrarFormulario = false;
    this.mensajeTest       = '';
    this.cdr.detectChanges();
  }

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

  // ── RUN NOW ────────────────────────────────────────────────────────────────

  ejecutarAhora(job: BackupJobDto): void {
    if (!confirm(`¿Ejecutar backup de "${job.nombre}" ahora?`)) return;
    this.cargandoRun = true;
    this.cdr.detectChanges();
    this.svc.ejecutarAhora(job.idJob).subscribe({
      next: () => this.zone.run(() => {
        this.cargandoRun = false;
        this.cargarJobs();
      }),
      error: () => this.zone.run(() => {
        this.cargandoRun = false;
        this.cdr.detectChanges();
      })
    });
  }

  // ── HISTORIAL ──────────────────────────────────────────────────────────────

  verHistorial(job: BackupJobDto): void {
    this.jobSeleccionado  = job;
    this.mostrarHistorial = true;
    this.mostrarFormulario = false;
    this.cdr.detectChanges();
    this.svc.historialJob(job.idJob).subscribe(h =>
      this.zone.run(() => {
        this.historial = h;
        this.cdr.detectChanges();
      })
    );
  }

  cerrarHistorial(): void {
    this.mostrarHistorial = false;
    this.historial        = [];
    this.cdr.detectChanges();
  }

  // ── TESTS ──────────────────────────────────────────────────────────────────

  probarPostgres(): void {
    this.cargandoTest = true;
    this.mensajeTest  = '';
    this.cdr.detectChanges();
    this.svc.probarPostgres(
      this.form.pgHost, this.form.pgPort,
      this.form.pgUsuario, this.form.pgPassword ?? ''
    ).subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest    = r.exitoso;
        this.mensajeTest  = r.mensaje;
        this.cargandoTest = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest    = false;
        this.mensajeTest  = 'Error al conectar';
        this.cargandoTest = false;
        this.cdr.detectChanges();
      })
    });
  }

  cargarDatabases(): void {
    if (!this.form.pgHost || !this.form.pgUsuario || !this.form.pgPassword) {
      this.errorDatabases = 'Completa host, usuario y contraseña primero';
      this.cdr.detectChanges();
      return;
    }
    this.cargandoDatabases = true;
    this.errorDatabases    = '';
    this.cdr.detectChanges();
    this.svc.listarDatabases(
      this.form.pgHost, this.form.pgPort,
      this.form.pgUsuario, this.form.pgPassword ?? ''
    ).subscribe({
      next: r => this.zone.run(() => {
        if (r.exitoso) {
          this.databasesDisponibles   = r.databases;
          this.databasesSeleccionadas = this.databasesSeleccionadas
            .filter(d => r.databases.includes(d));
        } else {
          this.errorDatabases = r.mensaje ?? 'No se pudo cargar';
        }
        this.cargandoDatabases = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.errorDatabases    = 'Error al conectar con PostgreSQL';
        this.cargandoDatabases = false;
        this.cdr.detectChanges();
      })
    });
  }

  toggleDatabase(db: string): void {
    if (this.databasesSeleccionadas.includes(db)) {
      this.databasesSeleccionadas = this.databasesSeleccionadas.filter(d => d !== db);
    } else {
      this.databasesSeleccionadas = [...this.databasesSeleccionadas, db];
    }
  }

  isDatabaseSeleccionada(db: string): boolean {
    return this.databasesSeleccionadas.includes(db);
  }

  probarDestino(tipo: TipoDestino): void {
    this.cargandoTest = true;
    this.mensajeTest  = '';
    this.cdr.detectChanges();

    let payload: any = { tipo };
    if (tipo === 'LOCAL') payload.rutaLocal       = this.destinoLocal.rutaLocal;
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
        this.exitoTest    = r.exitoso;
        this.mensajeTest  = r.mensaje;
        this.cargandoTest = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest    = false;
        this.mensajeTest  = 'Error al probar el destino';
        this.cargandoTest = false;
        this.cdr.detectChanges();
      })
    });
  }

  probarEmail(): void {
    const email = this.form.emailExito || this.form.emailFallo;
    if (!email) {
      this.mensajeTest = 'Ingresa un email primero';
      this.exitoTest   = false;
      this.cdr.detectChanges();
      return;
    }
    this.cargandoTest = true;
    this.cdr.detectChanges();
    this.svc.probarEmail(email).subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest    = r.exitoso;
        this.mensajeTest  = r.mensaje;
        this.cargandoTest = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest    = false;
        this.mensajeTest  = 'Error al enviar email';
        this.cargandoTest = false;
        this.cdr.detectChanges();
      })
    });
  }

  // ── SCHEDULER ──────────────────────────────────────────────────────────────

  isDiaActivo(dia: number): boolean {
    return this.diasActivos.includes(dia);
  }

  toggleDia(dia: number): void {
    if (this.diasActivos.includes(dia)) {
      if (this.diasActivos.length > 1)
        this.diasActivos = this.diasActivos.filter(d => d !== dia);
    } else {
      this.diasActivos = [...this.diasActivos, dia].sort((a, b) => a - b);
    }
    this.recalcularCron();
  }

  recalcularCron(): void {
    const p    = (this.schedFullHoraInicio || '00:00').split(':');
    const hora = parseInt(p[0], 10) || 0;
    const min  = parseInt(p[1], 10) || 0;
    const dias = this.diasActivos.length === 7
      ? '*'
      : this.diasActivos.slice().sort((a, b) => a - b).join(',');

    switch (this.tipoFrecuencia) {
      case 'horas': {
        const h = Math.max(1, this.schedFullHoras || 1);
        // Si el intervalo es exactamente 24h → cron diario a hora fija
        if (h >= 24) {
          this.form.cronFull = `0 ${min} ${hora} * * ${dias}`;
        } else {
          // "cada h horas, empezando en 'hora', con offset de 'min' minutos"
          // Ej: cada 12h desde las 21:30 → 0 30 21/12 * * *  (21:30, 09:30, 21:30...)
          this.form.cronFull = `0 ${min} ${hora}/${h} * * ${dias}`;
        }
        break;
      }
      case 'dias': {
        const d = Math.max(1, this.schedFullDiasSemana || 1);
        this.form.cronFull = d === 1
          ? `0 ${min} ${hora} * * ${dias}`
          : `0 ${min} ${hora} */${d} * *`;
        break;
      }
      case 'semanas': {
        const s = Math.max(1, this.schedFullSemanas || 1);
        this.form.cronFull = `0 ${min} ${hora} */${s * 7} * ${dias}`;
        break;
      }
      case 'meses': {
        const m   = Math.max(1, this.schedFullMeses || 1);
        const dia = Math.min(28, Math.max(1, this.schedDiaMes || 1));
        this.form.cronFull = m === 1
          ? `0 ${min} ${hora} ${dia} * *`
          : `0 ${min} ${hora} ${dia} */${m} *`;
        break;
      }
      case 'anual': {
        const dia = Math.min(28, Math.max(1, this.schedDiaMes || 1));
        this.form.cronFull = `0 ${min} ${hora} ${dia} 1 *`;
        break;
      }
    }

    if (this.form.diferencialActivo) {
      const dif = Math.max(1, this.schedDifHoras || 6);
      this.form.cronDiferencial = `0 0 */${dif} * * *`;
    }

    this.calcularProximasEjecuciones();
    this.cdr.detectChanges();
  }

  private calcularProximasEjecuciones(): void {
    const result: { fecha: string; tipo: string }[] = [];
    const ahora  = new Date();
    const p      = (this.schedFullHoraInicio || '00:00').split(':');
    const horaI  = parseInt(p[0], 10) || 0;
    const minI   = parseInt(p[1], 10) || 0;

    const fmt = (d: Date) =>
      `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()} ` +
      `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;

    const fechas: Date[] = [];

    if (this.tipoFrecuencia === 'horas') {
      const h  = Math.max(1, this.schedFullHoras || 1);
      const ms = h * 3_600_000;
      // Empezar exactamente en la hora:minuto configurada
      let c = new Date(ahora);
      c.setHours(horaI, minI, 0, 0);
      // Avanzar hasta la próxima ocurrencia futura
      while (c <= ahora) c = new Date(c.getTime() + ms);
      for (let i = 0; fechas.length < 10 && i < 500; i++) {
        if (this.diasActivos.includes(c.getDay()) && !this.enVentana(c)) fechas.push(new Date(c));
        c = new Date(c.getTime() + ms);
      }
    } else if (this.tipoFrecuencia === 'dias') {
      const d = Math.max(1, this.schedFullDiasSemana);
      let c = new Date(ahora); c.setHours(horaI, minI, 0, 0);
      if (c <= ahora) c.setDate(c.getDate() + d);
      for (let i = 0; fechas.length < 10 && i < 200; i++) {
        if (!this.enVentana(c)) fechas.push(new Date(c));
        c.setDate(c.getDate() + d);
      }
    } else if (this.tipoFrecuencia === 'semanas') {
      const s = Math.max(1, this.schedFullSemanas);
      let c = new Date(ahora); c.setHours(horaI, minI, 0, 0);
      for (let i = 0; i < 14; i++) {
        if (this.diasActivos.includes(c.getDay()) && c > ahora) break;
        c.setDate(c.getDate() + 1);
      }
      for (let i = 0; fechas.length < 10 && i < 100; i++) {
        if (this.diasActivos.includes(c.getDay())) fechas.push(new Date(c));
        c.setDate(c.getDate() + s * 7);
      }
    } else if (this.tipoFrecuencia === 'meses') {
      const m   = Math.max(1, this.schedFullMeses);
      const dia = Math.min(28, Math.max(1, this.schedDiaMes));
      let c = new Date(ahora.getFullYear(), ahora.getMonth(), dia, horaI, minI, 0);
      if (c <= ahora) c.setMonth(c.getMonth() + m);
      for (let i = 0; fechas.length < 10; i++) {
        fechas.push(new Date(c)); c.setMonth(c.getMonth() + m);
      }
    } else if (this.tipoFrecuencia === 'anual') {
      const dia = Math.min(28, Math.max(1, this.schedDiaMes));
      let c = new Date(ahora.getFullYear(), 0, dia, horaI, minI, 0);
      if (c <= ahora) c.setFullYear(c.getFullYear() + 1);
      for (let i = 0; fechas.length < 6; i++) {
        fechas.push(new Date(c)); c.setFullYear(c.getFullYear() + 1);
      }
    }

    for (let i = 0; i < fechas.length && result.length < 10; i++) {
      result.push({ fecha: fmt(fechas[i]), tipo: 'Full' });
      if (this.form.diferencialActivo && fechas[i + 1] && result.length < 10) {
        const difTime = new Date(fechas[i].getTime() + Math.max(1, this.schedDifHoras) * 3_600_000);
        if (difTime < fechas[i + 1]) result.push({ fecha: fmt(difTime), tipo: 'Dif' });
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

  // ── GOOGLE DRIVE OAuth ─────────────────────────────────────────────────────

  conectarGoogleDrive(): void {
    // Buscar si ya hay un destino Drive guardado con ID
    const driveDestino = (this.jobSeleccionado?.destinos ?? [])
      .find(d => d.tipo === 'GOOGLE_DRIVE');

    if (driveDestino?.idDestination) {
      // Ya tiene ID — abrir OAuth directamente
      this.abrirVentanaOAuth(driveDestino.idDestination);
    } else if (this.jobSeleccionado) {
      // Tiene job pero el destino Drive no está guardado aún — guardar primero
      this.cargandoOAuth = true;
      this.cdr.detectChanges();
      this.guardarYConectar();
    } else {
      // No hay job todavía — pedir que guarde primero
      this.mensajeTest = 'Guarda el job primero haciendo clic en "Crear Job", luego edítalo y conecta Google Drive.';
      this.exitoTest   = false;
      this.cdr.detectChanges();
    }
  }

  private guardarYConectar(): void {
    // Guardar el job actual con el destino Drive incluido
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
        this.jobSeleccionado = jobGuardado;
        this.modoEdicion     = true;
        // Recargar el job para tener los IDs de los destinos
        this.svc.obtenerJob(jobGuardado.idJob).subscribe({
          next: jobCompleto => this.zone.run(() => {
            this.jobSeleccionado = jobCompleto;
            // Actualizar el destinoDrive con los datos del servidor
            const driveGuardado = (jobCompleto.destinos ?? [])
              .find(d => d.tipo === 'GOOGLE_DRIVE');
            if (driveGuardado) Object.assign(this.destinoDrive, driveGuardado);

            const id = driveGuardado?.idDestination;
            if (id) {
              this.abrirVentanaOAuth(id);
            } else {
              this.cargandoOAuth = false;
              this.mensajeTest   = 'No se pudo obtener el ID del destino Google Drive';
              this.exitoTest     = false;
              this.cdr.detectChanges();
            }
          }),
          error: () => this.zone.run(() => {
            this.cargandoOAuth = false;
            this.cdr.detectChanges();
          })
        });
      }),
      error: () => this.zone.run(() => {
        this.cargandoOAuth = false;
        this.mensajeTest   = 'Error guardando el job';
        this.exitoTest     = false;
        this.cdr.detectChanges();
      })
    });
  }

  private abrirVentanaOAuth(destinationId: number): void {
    this.svc.iniciarOAuthDrive(destinationId).subscribe({
      next: r => this.zone.run(() => {
        const popup = window.open(r.url, 'google-oauth',
          'width=500,height=600,scrollbars=yes,resizable=yes');

        const handler = (event: MessageEvent) => {
          if (event.data?.type === 'GDRIVE_CONNECTED') {
            window.removeEventListener('message', handler);
            this.zone.run(() => {
              this.destinoDrive.gdriveConectado = true;
              this.destinoDrive.gdriveCuenta    = event.data.email;
              this.cargandoOAuth                = false;
              this.mensajeTest                  = `Cuenta conectada: ${event.data.email}`;
              this.exitoTest                    = true;
              this.cdr.detectChanges();
            });
          } else if (event.data?.type === 'GDRIVE_ERROR') {
            window.removeEventListener('message', handler);
            this.zone.run(() => {
              this.cargandoOAuth = false;
              this.mensajeTest   = `Error: ${event.data.mensaje}`;
              this.exitoTest     = false;
              this.cdr.detectChanges();
            });
          }
        };
        window.addEventListener('message', handler);

        const timer = setInterval(() => {
          if (popup?.closed) {
            clearInterval(timer);
            window.removeEventListener('message', handler);
            this.zone.run(() => {
              this.cargandoOAuth = false;
              this.cdr.detectChanges();
            });
          }
        }, 1000);

        this.cargandoOAuth = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargandoOAuth = false;
        this.mensajeTest   = 'Error iniciando OAuth con Google';
        this.exitoTest     = false;
        this.cdr.detectChanges();
      })
    });
  }

  cargarCarpetasDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? [])
      .find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;

    this.cargandoCarpetas       = true;
    this.mostrarSelectorCarpetas = true;
    this.cdr.detectChanges();

    this.svc.listarCarpetasDrive(driveDestino.idDestination).subscribe({
      next: r => this.zone.run(() => {
        this.carpetasDrive   = r.exitoso ? r.carpetas : [];
        this.cargandoCarpetas = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.cargandoCarpetas = false;
        this.cdr.detectChanges();
      })
    });
  }

  seleccionarCarpeta(carpeta: { id: string; name: string }): void {
    this.destinoDrive.gdriveFolderNombre = carpeta.name;
    this.destinoDrive.gdriveFolderId     = carpeta.id;
    this.mostrarSelectorCarpetas         = false;
    this.cdr.detectChanges();
  }

  desconectarGoogleDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? [])
      .find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;

    this.svc.desconectarDrive(driveDestino.idDestination).subscribe(() =>
      this.zone.run(() => {
        this.destinoDrive.gdriveConectado = false;
        this.destinoDrive.gdriveCuenta    = '';
        this.mensajeTest                  = 'Cuenta de Google Drive desconectada';
        this.exitoTest                    = true;
        this.cdr.detectChanges();
      })
    );
  }

  probarGoogleDrive(): void {
    const driveDestino = (this.jobSeleccionado?.destinos ?? [])
      .find(d => d.tipo === 'GOOGLE_DRIVE');
    if (!driveDestino?.idDestination) return;

    this.cargandoTest = true;
    this.cdr.detectChanges();
    this.svc.probarDrive(driveDestino.idDestination).subscribe({
      next: r => this.zone.run(() => {
        this.exitoTest    = r.exitoso;
        this.mensajeTest  = r.mensaje;
        this.cargandoTest = false;
        this.cdr.detectChanges();
      }),
      error: () => this.zone.run(() => {
        this.exitoTest    = false;
        this.mensajeTest  = 'Error probando Google Drive';
        this.cargandoTest = false;
        this.cdr.detectChanges();
      })
    });
  }

  onCarpetaSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const ruta = (input.files[0] as any).webkitRelativePath as string || input.files[0].name;
    this.destinoLocal.rutaLocal = ruta.split('/')[0];
    input.value = '';
    this.cdr.detectChanges();
  }

  onPgDumpSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    // El browser no expone la ruta completa por seguridad,
    // pero sí el nombre del archivo. El usuario puede editar manualmente.
    const file = input.files[0];
    this.form.pgDumpPath = file.name;
    input.value = '';
    this.cdr.detectChanges();
  }

  // ── HELPERS ────────────────────────────────────────────────────────────────

  formatearTamano(bytes?: number): string {
    if (!bytes) return '-';
    if (bytes < 1024)                return `${bytes} B`;
    if (bytes < 1024 * 1024)         return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024)  return `${(bytes / 1048576).toFixed(1)} MB`;
    return `${(bytes / 1073741824).toFixed(1)} GB`;
  }

  badgeEstado(estado?: EstadoEjecucion): string {
    const map: Record<string, string> = {
      EXITOSO: 'badge-ok', FALLIDO: 'badge-error',
      EN_PROCESO: 'badge-warning', PENDIENTE: 'badge-info'
    };
    return map[estado ?? ''] ?? 'badge-info';
  }

  setDestinoActivo(tipo: TipoDestino): void {
    this.destinoActivo = tipo;
  }

  destinosJob(job: BackupJobDto): any[] {
    return job.destinos ?? [];
  }

  // ── PRIVADOS ───────────────────────────────────────────────────────────────

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
    this.tipoFrecuencia         = 'horas';
    this.schedFullHoras         = 12;
    this.schedFullMinutos       = 0;
    this.schedFullHoraInicio    = '21:00';
    this.schedFullDiasSemana    = 1;
    this.schedFullSemanas       = 1;
    this.schedFullMeses         = 1;
    this.schedDiaMes            = 1;
    this.schedDifHoras          = 6;
    this.diasActivos            = [0, 1, 2, 3, 4, 5, 6];
    this.ventanaActiva          = false;
    this.recalcularCron();
  }

  private formVacio(): BackupJobRequest {
    return {
      nombre: '', pgDumpPath: '', pgHost: 'localhost', pgPort: 5432,
      pgUsuario: '', pgPassword: '', databases: '', comprimir: true,
      cronFull: '0 0 */12 * * *', cronDiferencial: '',
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
