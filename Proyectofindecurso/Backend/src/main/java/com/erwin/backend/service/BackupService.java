package com.erwin.backend.service;

import com.erwin.backend.dtos.BackupJobRequest;
import com.erwin.backend.dtos.BackupDestinationRequest;
import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import com.erwin.backend.entities.BackupExecution.TipoBackup;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupExecutionRepository;
import com.erwin.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String            TEMP_DIR = System.getProperty("java.io.tmpdir") + "/backups_tmp/";

    private final BackupJobRepository       jobRepo;
    private final BackupExecutionRepository execRepo;
    private final BackupEncryptionUtil        encryption;
    private final BackupStorageService        storageService;
    private final BackupNotificationService   notificationService;      // email
    private final BackupSseNotificationService sseService;              // SSE tiempo real

    // ── CRUD ───────────────────────────────────────────────────

    @Transactional
    public BackupJob crearJob(BackupJobRequest req) {
        BackupJob job = new BackupJob();
        mapRequestToJob(req, job);
        return jobRepo.save(job);
    }

    @Transactional
    public BackupJob actualizarJob(Long id, BackupJobRequest req) {
        BackupJob job = jobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + id));
        mapRequestToJob(req, job);
        return jobRepo.save(job);
    }

    @Transactional
    public void eliminarJob(Long id) {
        jobRepo.deleteById(id);
    }

    public List<BackupJob> listarJobs() {
        return jobRepo.findAll();
    }

    public BackupJob obtenerJob(Long id) {
        return jobRepo.findByIdWithDestinos(id)
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + id));
    }

    // ── Ejecución ──────────────────────────────────────────────

    @Transactional
    public BackupExecution ejecutarJob(Long jobId, boolean manual) {
        BackupJob job = obtenerJob(jobId);
        return ejecutarConReintento(job, TipoBackup.FULL, manual);
    }

    @Transactional
    public BackupExecution ejecutarConReintento(BackupJob job, TipoBackup tipo, boolean manual) {
        // Recargar el job dentro de la transacción para que Hibernate
        // pueda hacer lazy loading de destinos sin LazyInitializationException
        BackupJob jobFresh = jobRepo.findById(job.getIdJob())
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + job.getIdJob()));

        // Forzar carga de destinos dentro de la sesión activa
        jobFresh.getDestinos().size();

        int maxIntentos = jobFresh.getMaxReintentos() != null ? jobFresh.getMaxReintentos() : 1;
        BackupExecution lastExec = null;

        for (int intento = 1; intento <= maxIntentos + 1; intento++) {
            lastExec = ejecutarBackup(jobFresh, tipo, manual, intento);
            if (lastExec.getEstado() == EstadoEjecucion.EXITOSO) break;
            if (intento <= maxIntentos) {
                log.warn("Reintentando backup job={} intento={}", jobFresh.getIdJob(), intento + 1);
                try { Thread.sleep(5000L * intento); } catch (InterruptedException ignored) {}
            }
        }

        jobFresh.setUltimaEjecucion(LocalDateTime.now());
        jobRepo.save(jobFresh);
        notificationService.notificar(jobFresh, lastExec);

        // Emitir notificación SSE en tiempo real
        if (lastExec != null) {
            sseService.notificarBackupCompletado(lastExec, lastExec.getEstado());
        }

        return lastExec;
    }

    private BackupExecution ejecutarBackup(BackupJob job, TipoBackup tipo, boolean manual, int intento) {
        String[] databases = job.getDatabases().split(",");
        BackupExecution exec = null;

        for (String dbName : databases) {
            exec = new BackupExecution();
            exec.setJob(job);
            exec.setTipoBackup(tipo);
            exec.setDatabaseNombre(dbName.trim());
            exec.setManual(manual);
            exec.setIntentoNumero(intento);
            exec.setEstado(EstadoEjecucion.EN_PROCESO);
            exec = execRepo.save(exec);

            Path archivoBackup = null;
            try {
                archivoBackup = runPgDump(job, dbName.trim());
                exec.setArchivoNombre(archivoBackup.getFileName().toString());
                exec.setArchivoRuta(archivoBackup.toString());
                exec.setTamanoBytes(Files.size(archivoBackup));

                for (BackupDestination destino : job.getDestinos()) {
                    if (Boolean.TRUE.equals(destino.getActivo())) {
                        storageService.subir(archivoBackup, destino, exec);
                        exec.setDestinoTipo(destino.getTipo().name());
                    }
                }
                exec.setEstado(EstadoEjecucion.EXITOSO);
                log.info("Backup exitoso: job={} db={}", job.getIdJob(), dbName);

            } catch (Exception e) {
                exec.setEstado(EstadoEjecucion.FALLIDO);
                exec.setErrorMensaje(e.getMessage());
                log.error("Backup fallido: job={} db={}", job.getIdJob(), dbName, e);
            } finally {
                exec.setFinalizadoEn(LocalDateTime.now());
                if (exec.getIniciadoEn() != null) {
                    exec.setDuracionSegundos(
                            java.time.Duration.between(exec.getIniciadoEn(), exec.getFinalizadoEn()).getSeconds());
                }
                execRepo.save(exec);
                if (archivoBackup != null) {
                    try { Files.deleteIfExists(archivoBackup); } catch (IOException ignored) {}
                }
            }
        }
        return exec;
    }

    private Path runPgDump(BackupJob job, String dbName) throws Exception {
        Files.createDirectories(Paths.get(TEMP_DIR));

        boolean comprimir = Boolean.TRUE.equals(job.getComprimir());
        String  ext       = comprimir ? ".zip" : ".dump";
        String  fileName  = String.format("backup_%s_%s%s", dbName, LocalDateTime.now().format(FMT), ext);
        Path    dumpPath  = Paths.get(TEMP_DIR, fileName.replace(".zip", ".dump"));
        Path    outputPath = Paths.get(TEMP_DIR, fileName);
        String  password  = encryption.decrypt(job.getPgPasswordEnc());

        String pgDumpCmd = (job.getPgDumpPath() != null && !job.getPgDumpPath().isBlank())
                ? job.getPgDumpPath().trim()
                : "pg_dump";

        // Siempre genera .dump primero
        ProcessBuilder pb = new ProcessBuilder(
                pgDumpCmd,
                "-h", job.getPgHost(),
                "-p", String.valueOf(job.getPgPort()),
                "-U", job.getPgUsuario(),
                "-d", dbName,
                "-F", "c",
                "-f", dumpPath.toString()
        );

        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        Process process  = pb.start();
        String  output   = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);

        if (!finished) { process.destroyForcibly(); throw new RuntimeException("pg_dump superó 10 minutos"); }
        if (process.exitValue() != 0) throw new RuntimeException("pg_dump falló: " + output);
        if (!Files.exists(dumpPath) || Files.size(dumpPath) == 0) throw new RuntimeException("pg_dump generó archivo vacío");

        // Si comprimir está activado, empaquetar en ZIP
        if (comprimir) {
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(outputPath.toFile()))) {
                zos.putNextEntry(new java.util.zip.ZipEntry(dumpPath.getFileName().toString()));
                Files.copy(dumpPath, zos);
                zos.closeEntry();
            }
            Files.deleteIfExists(dumpPath);
            return outputPath;
        }

        return dumpPath;
    }

    // Usa JDBC directamente — funciona en Windows y Linux sin necesitar pg_isready en el PATH
    public boolean probarConexionPg(String host, int port, String usuario, String password) {
        String url = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            log.info("Prueba conexión PG exitosa: {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.warn("Prueba conexión PG fallida: {}:{} — {}", host, port, e.getMessage());
            return false;
        }
    }

    // Lista todas las bases de datos del servidor (excluye las de sistema)
    public List<String> listarDatabases(String host, int port, String usuario, String password) {
        String url = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        List<String> result = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, usuario, password);
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT datname FROM pg_database " +
                             "WHERE datistemplate = false " +
                             "AND datname NOT IN ('postgres','template0','template1') " +
                             "ORDER BY datname")) {
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("datname"));
            }
        } catch (Exception e) {
            log.error("Error listando databases: {}", e.getMessage());
            throw new RuntimeException("No se pudo listar las bases de datos: " + e.getMessage());
        }
        return result;
    }

    public List<BackupExecution> historialJob(Long jobId) {
        BackupJob job = obtenerJob(jobId);
        return execRepo.findByJobOrderByIniciadoEnDesc(job, PageRequest.of(0, 100)).getContent();
    }

    private void mapRequestToJob(BackupJobRequest req, BackupJob job) {
        job.setNombre(req.getNombre());
        job.setPgDumpPath(req.getPgDumpPath());
        job.setPgHost(req.getPgHost());
        job.setPgPort(req.getPgPort() != null ? req.getPgPort() : 5432);
        job.setPgUsuario(req.getPgUsuario());
        if (req.getPgPassword() != null && !req.getPgPassword().isBlank()) {
            job.setPgPasswordEnc(encryption.encrypt(req.getPgPassword()));
        }
        job.setDatabases(req.getDatabases());
        job.setComprimir(req.getComprimir() != null ? req.getComprimir() : Boolean.TRUE);
        job.setCronFull(req.getCronFull());
        job.setCronDiferencial(req.getCronDiferencial());
        job.setDiferencialActivo(req.getDiferencialActivo());
        job.setZonaHoraria(req.getZonaHoraria());
        job.setVentanaExcluirInicio(req.getVentanaExcluirInicio());
        job.setVentanaExcluirFin(req.getVentanaExcluirFin());
        job.setMaxReintentos(req.getMaxReintentos());
        job.setEmailExito(req.getEmailExito());
        job.setEmailFallo(req.getEmailFallo());
        job.setActivo(req.getActivo() != null ? req.getActivo() : Boolean.TRUE);

        // Mapear destinos
        if (req.getDestinos() != null) {
            // Guardar los destinos existentes ANTES del clear para preservar tokens cifrados
            Map<Long, BackupDestination> existentes = new java.util.HashMap<>();
            for (BackupDestination ex : job.getDestinos()) {
                if (ex.getIdDestination() != null) {
                    existentes.put(ex.getIdDestination(), ex);
                }
            }

            job.getDestinos().clear();

            for (BackupDestinationRequest dr : req.getDestinos()) {
                BackupDestination d = new BackupDestination();
                d.setJob(job);
                d.setTipo(dr.getTipo());
                d.setActivo(dr.getActivo() != null ? dr.getActivo() : Boolean.TRUE);

                // Destino existente para recuperar campos cifrados
                BackupDestination existente = dr.getIdDestination() != null
                        ? existentes.get(dr.getIdDestination())
                        : null;

                // LOCAL
                d.setRutaLocal(dr.getRutaLocal());

                // AZURE
                d.setAzureAccount(dr.getAzureAccount());
                d.setAzureContainer(dr.getAzureContainer());
                if (dr.getAzureKey() != null && !dr.getAzureKey().isBlank()) {
                    d.setAzureKeyEnc(encryption.encrypt(dr.getAzureKey()));
                } else if (existente != null) {
                    d.setAzureKeyEnc(existente.getAzureKeyEnc());
                }

                // GOOGLE DRIVE — preservar refresh token cifrado siempre
                d.setGdriveCuenta(dr.getGdriveCuenta() != null ? dr.getGdriveCuenta()
                        : (existente != null ? existente.getGdriveCuenta() : null));
                d.setGdriveFolderNombre(dr.getGdriveFolderNombre());

                // Si el nombre de carpeta cambió, limpiar el folderId para forzar nueva búsqueda
                String nombreNuevo    = dr.getGdriveFolderNombre();
                String nombreExistente = existente != null ? existente.getGdriveFolderNombre() : null;
                boolean nombreCambiado = nombreNuevo != null && !nombreNuevo.isBlank()
                        && !nombreNuevo.equals(nombreExistente);

                if (nombreCambiado) {
                    d.setGdriveFolderId(null); // forzar búsqueda/creación con nuevo nombre
                    log.info("Nombre carpeta Drive cambió '{}' -> '{}', limpiando folderId",
                            nombreExistente, nombreNuevo);
                } else {
                    d.setGdriveFolderId(dr.getGdriveFolderId() != null ? dr.getGdriveFolderId()
                            : (existente != null ? existente.getGdriveFolderId() : null));
                }

                // Nunca sobreescribir el refresh token desde el request — solo se guarda via OAuth
                if (existente != null && existente.getGdriveRefreshTokenEnc() != null) {
                    d.setGdriveRefreshTokenEnc(existente.getGdriveRefreshTokenEnc());
                }

                // S3
                d.setS3Bucket(dr.getS3Bucket());
                d.setS3Region(dr.getS3Region());
                d.setS3AccessKey(dr.getS3AccessKey());
                if (dr.getS3SecretKey() != null && !dr.getS3SecretKey().isBlank()) {
                    d.setS3SecretKeyEnc(encryption.encrypt(dr.getS3SecretKey()));
                } else if (existente != null) {
                    d.setS3SecretKeyEnc(existente.getS3SecretKeyEnc());
                }

                // Retención
                d.setRetencionMeses(dr.getRetencionMeses() != null ? dr.getRetencionMeses() : 0);
                d.setRetencionDias(dr.getRetencionDias()   != null ? dr.getRetencionDias()   : 0);
                d.setMaxBackups(dr.getMaxBackups()         != null ? dr.getMaxBackups()       : 0);

                job.getDestinos().add(d);
            }
        }
    }
}