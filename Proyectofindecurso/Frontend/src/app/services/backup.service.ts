import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ─── Enums ────────────────────────────────────────────────────────────────────
export type TipoDestino     = 'LOCAL' | 'AZURE' | 'GOOGLE_DRIVE' | 'S3';
export type EstadoEjecucion = 'PENDIENTE' | 'EN_PROCESO' | 'EXITOSO' | 'FALLIDO';
export type TipoBackup      = 'FULL' | 'DIFERENCIAL';

// ─── Interfaces ───────────────────────────────────────────────────────────────
export interface BackupDestinationDto {
  idDestination:       number | null;
  tipo:                TipoDestino;
  activo:              boolean;

  // LOCAL
  rutaLocal?:          string;

  // AZURE
  azureAccount?:       string;
  azureContainer?:     string;
  azureConfigurado?:   boolean;

  // GOOGLE DRIVE
  gdriveCuenta?:       string;
  gdriveFolderId?:     string;
  gdriveFolderNombre?: string;
  gdriveConectado?:    boolean;

  // S3
  s3Bucket?:           string;
  s3Region?:           string;
  s3AccessKey?:        string;
  s3Configurado?:      boolean;

  // Retención
  retencionMeses?:     number;
  retencionDias?:      number;
  maxBackups?:         number;
}

export interface BackupDestinationRequest extends BackupDestinationDto {
  azureKey?:           string;
  gdriveRefreshToken?: string;
  s3SecretKey?:        string;
}

export interface BackupJobDto {
  idJob:                number;
  nombre:               string;
  pgDumpPath?:          string;
  pgHost:               string;
  pgPort:               number;
  pgUsuario:            string;
  databases:            string;
  comprimir?:           boolean;
  cronFull:             string;
  cronDiferencial?:     string;
  diferencialActivo:    boolean;
  zonaHoraria:          string;
  ventanaExcluirInicio?: string;
  ventanaExcluirFin?:   string;
  maxReintentos:        number;
  emailExito?:          string;
  emailFallo?:          string;
  activo:               boolean;
  proximaEjecucion?:    string;
  ultimaEjecucion?:     string;
  creadoEn?:            string;
  ultimoEstado?:        EstadoEjecucion;
  destinos:             BackupDestinationDto[];
}

export interface BackupJobRequest {
  nombre:               string;
  pgDumpPath?:          string;
  pgHost:               string;
  pgPort:               number;
  pgUsuario:            string;
  pgPassword?:          string;
  databases:            string;
  comprimir?:           boolean;
  cronFull:             string;
  cronDiferencial?:     string;
  diferencialActivo:    boolean;
  zonaHoraria:          string;
  ventanaExcluirInicio?: string;
  ventanaExcluirFin?:   string;
  maxReintentos:        number;
  emailExito?:          string;
  emailFallo?:          string;
  activo:               boolean;
  destinos:             BackupDestinationRequest[];
}

export interface RestoreResponse {
  idExecution:       number;
  idJob:             number;
  jobNombre:         string;
  databaseNombre:    string;
  archivoNombre:     string | null;
  archivoRuta:       string | null;
  tamanoBytes:       number | null;
  estado:            string;
  iniciadoEn:        string;
  archivoDisponible: boolean;
  destinoTipo:       string | null;
}

export interface RestoreRequest {
  idExecution:    number;
  idJob:          number;
  modo:           'REEMPLAZAR' | 'NUEVA_BD';
  nombreBdNueva?: string;
}

export interface RestoreResultado {
  exitoso:          boolean;
  mensaje:          string;
  log:              string | null;
  bdRestaurada:     string | null;
  duracionSegundos: number;
}

export interface BackupExecutionResponse {
  idExecution:        number;
  idJob:              number;
  jobNombre:          string;
  estado:             EstadoEjecucion;
  tipoBackup:         TipoBackup;
  databaseNombre:     string;
  archivoNombre?:     string;
  tamanoBytes?:       number;
  destinoTipo?:       string;
  errorMensaje?:      string;
  intentoNumero:      number;
  iniciadoEn?:        string;
  finalizadoEn?:      string;
  duracionSegundos?:  number;
  manual:             boolean;
  // ── Diferencial ──────────────────────────────────
  idBackupPadre?:     number | null;
  tablasIncluidas?:   string | null;
}

export interface TestResultado {
  exitoso: boolean;
  mensaje: string;
}

// ─── Timeline DTOs ────────────────────────────────────────────────────────────
export interface BackupNodoDto {
  idExecution:         number;
  tipo:                TipoBackup;
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

// ─── Service ──────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class BackupService {

  private readonly API_URL = 'http://localhost:8080/api/backup';

  constructor(private readonly http: HttpClient) {}

  // ── Jobs ───────────────────────────────────────────────────────────────────

  listarJobs(): Observable<BackupJobDto[]> {
    return this.http.get<BackupJobDto[]>(`${this.API_URL}/jobs`);
  }

  obtenerJob(id: number): Observable<BackupJobDto> {
    return this.http.get<BackupJobDto>(`${this.API_URL}/jobs/${id}`);
  }

  crearJob(req: BackupJobRequest): Observable<BackupJobDto> {
    return this.http.post<BackupJobDto>(`${this.API_URL}/jobs`, req);
  }

  actualizarJob(id: number, req: BackupJobRequest): Observable<BackupJobDto> {
    return this.http.put<BackupJobDto>(`${this.API_URL}/jobs/${id}`, req);
  }

  eliminarJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/jobs/${id}`);
  }

  toggleEstado(id: number, activo: boolean): Observable<BackupJobDto> {
    return this.http.patch<BackupJobDto>(`${this.API_URL}/jobs/${id}/estado`, { activo });
  }

  ejecutarAhora(id: number): Observable<BackupExecutionResponse> {
    return this.http.post<BackupExecutionResponse>(`${this.API_URL}/jobs/${id}/run`, {});
  }

  // ── Diferencial ────────────────────────────────────────────────────────────

  ejecutarDiferencialManual(idJob: number): Observable<BackupExecutionResponse> {
    return this.http.post<BackupExecutionResponse>(
      `${this.API_URL}/jobs/${idJob}/ejecutar-diferencial`, {});
  }

  // ── Historial ──────────────────────────────────────────────────────────────

  historialJob(id: number): Observable<BackupExecutionResponse[]> {
    return this.http.get<BackupExecutionResponse[]>(`${this.API_URL}/jobs/${id}/historial`);
  }

  historialGeneral(): Observable<BackupExecutionResponse[]> {
    return this.http.get<BackupExecutionResponse[]>(`${this.API_URL}/historial`);
  }

  // ── Tests ──────────────────────────────────────────────────────────────────

  probarPostgres(host: string, port: number, usuario: string, password: string): Observable<TestResultado> {
    return this.http.post<TestResultado>(`${this.API_URL}/test/postgres`,
      { host, port, usuario, password });
  }

  listarDatabases(host: string, port: number, usuario: string, password: string):
    Observable<{ exitoso: boolean; databases: string[]; mensaje?: string }> {
    return this.http.post<{ exitoso: boolean; databases: string[]; mensaje?: string }>(
      `${this.API_URL}/test/databases`, { host, port, usuario, password });
  }

  probarDestino(payload: any): Observable<TestResultado> {
    return this.http.post<TestResultado>(`${this.API_URL}/test/destino`, payload);
  }

  probarEmail(email: string): Observable<TestResultado> {
    return this.http.post<TestResultado>(`${this.API_URL}/test/email`, { email });
  }

  // ── Estadísticas ──────────────────────────────────────────────────────────

  estadisticas(): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/stats`);
  }

  verificarIntegridad(idExecution: number, idJob: number): Observable<any> {
    return this.http.post<any>(
      `${this.API_URL}/integridad/${idExecution}?idJob=${idJob}`, {});
  }

  aplicarRetencion(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/retencion/aplicar`, {});
  }

  // ── Restauración ──────────────────────────────────────────────────────────

  historialRestore(jobId: number): Observable<RestoreResponse[]> {
    return this.http.get<RestoreResponse[]>(
      `${this.API_URL}/restaurar/historial/${jobId}`);
  }

  ejecutarRestore(req: RestoreRequest): Observable<RestoreResultado> {
    return this.http.post<RestoreResultado>(
      `${this.API_URL}/restaurar/ejecutar`, req);
  }

  // ── Timeline git-log ──────────────────────────────────────────────────────

  obtenerTimeline(idJob: number): Observable<TimelineResponseDto> {
    return this.http.get<TimelineResponseDto>(
      `${this.API_URL}/jobs/${idJob}/timeline`);
  }

  // ── Zonas horarias ────────────────────────────────────────────────────────

  zonasHorarias(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API_URL}/zonas-horarias`);
  }

  // ── Google Drive OAuth ────────────────────────────────────────────────────

  iniciarOAuthDrive(destinationId: number): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.API_URL}/oauth/google/init/${destinationId}`);
  }

  listarCarpetasDrive(destinationId: number):
    Observable<{ exitoso: boolean; carpetas: { id: string; name: string }[]; mensaje?: string }> {
    return this.http.get<any>(
      `${this.API_URL}/oauth/google/folders/${destinationId}`);
  }

  desconectarDrive(destinationId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.API_URL}/oauth/google/disconnect/${destinationId}`);
  }

  probarDrive(destinationId: number): Observable<TestResultado> {
    return this.http.post<TestResultado>(
      `${this.API_URL}/oauth/google/test/${destinationId}`, {});
  }
}
