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
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String            TEMP_DIR = System.getProperty("java.io.tmpdir") + "/backups_tmp/";

    private final BackupJobRepository          jobRepo;
    private final BackupExecutionRepository    execRepo;
    private final BackupEncryptionUtil         encryption;
    private final BackupStorageService         storageService;
    private final BackupNotificationService    notificationService;
    private final BackupSseNotificationService sseService;

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

    // ── Ejecución manual ───────────────────────────────────────

    @Transactional
    public BackupExecution ejecutarJob(Long jobId, boolean manual) {
        BackupJob job = obtenerJob(jobId);
        return ejecutarConReintento(job, TipoBackup.FULL, manual);
    }

    /**
     * Ejecuta un diferencial manual para el job dado.
     * Busca el último FULL exitoso y exporta solo tablas modificadas.
     */
    @Transactional
    public BackupExecution ejecutarDiferencialManual(Long jobId) {
        BackupJob job = obtenerJob(jobId);
        return ejecutarConReintento(job, TipoBackup.DIFERENCIAL, true);
    }

    // ── Ejecución con reintento ────────────────────────────────

    @Transactional
    public BackupExecution ejecutarConReintento(BackupJob job, TipoBackup tipo, boolean manual) {
        BackupJob jobFresh = jobRepo.findById(job.getIdJob())
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + job.getIdJob()));
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
                if (tipo == TipoBackup.DIFERENCIAL) {
                    archivoBackup = runPgDumpDiferencial(job, dbName.trim(), exec);
                } else {
                    archivoBackup = runPgDump(job, dbName.trim());
                }

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
                log.info("Backup {} exitoso: job={} db={}", tipo, job.getIdJob(), dbName);

            } catch (Exception e) {
                exec.setEstado(EstadoEjecucion.FALLIDO);
                exec.setErrorMensaje(e.getMessage());
                log.error("Backup {} fallido: job={} db={}", tipo, job.getIdJob(), dbName, e);
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

    // ── FULL backup ────────────────────────────────────────────

    private Path runPgDump(BackupJob job, String dbName) throws Exception {
        Files.createDirectories(Paths.get(TEMP_DIR));

        boolean comprimir  = Boolean.TRUE.equals(job.getComprimir());
        String  ext        = comprimir ? ".zip" : ".dump";
        String  fileName   = String.format("backup_full_%s_%s%s", dbName, LocalDateTime.now().format(FMT), ext);
        Path    dumpPath   = Paths.get(TEMP_DIR, fileName.replace(".zip", ".dump"));
        Path    outputPath = Paths.get(TEMP_DIR, fileName);
        String  password   = encryption.decrypt(job.getPgPasswordEnc());

        String pgDumpCmd = (job.getPgDumpPath() != null && !job.getPgDumpPath().isBlank())
                ? job.getPgDumpPath().trim() : "pg_dump";

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpCmd, "-h", job.getPgHost(), "-p", String.valueOf(job.getPgPort()),
                "-U", job.getPgUsuario(), "-d", dbName, "-F", "c", "-f", dumpPath.toString()
        );
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        return ejecutarPgDump(pb, dumpPath, outputPath, comprimir);
    }

    // ── DIFERENCIAL backup ──────────────────────────────────────

    /**
     * Backup diferencial lógico:
     * 1. Busca el último FULL exitoso de este job para esta BD
     * 2. Consulta pg_stat_user_tables para detectar tablas con actividad DML
     *    desde la fecha del último FULL
     * 3. Exporta solo esas tablas con pg_dump --table=...
     * 4. Guarda idBackupPadre y tablasIncluidas en la ejecución
     */
    private Path runPgDumpDiferencial(BackupJob job, String dbName, BackupExecution exec) throws Exception {
        Files.createDirectories(Paths.get(TEMP_DIR));

        // 1. Buscar último FULL exitoso
        BackupExecution ultimoFull = execRepo
                .findTopByJob_IdJobAndDatabaseNombreAndTipoBackupAndEstadoOrderByIniciadoEnDesc(
                        job.getIdJob(), dbName, TipoBackup.FULL, EstadoEjecucion.EXITOSO)
                .orElseThrow(() -> new RuntimeException(
                        "No existe backup FULL exitoso para '" + dbName +
                                "'. Ejecuta primero un backup FULL antes del diferencial."));

        exec.setIdBackupPadre(ultimoFull.getIdExecution());
        LocalDateTime fechaFull = ultimoFull.getIniciadoEn();
        log.info("Diferencial basado en FULL id={} fecha={}", ultimoFull.getIdExecution(), fechaFull);

        // 2. Detectar tablas modificadas desde el último FULL
        String password = encryption.decrypt(job.getPgPasswordEnc());
        List<String> tablasModificadas = detectarTablasModificadas(
                job.getPgHost(), job.getPgPort(), job.getPgUsuario(), password, dbName, fechaFull);

        if (tablasModificadas.isEmpty()) {
            log.info("No hay tablas modificadas desde el FULL id={} — generando diferencial vacío marcado", ultimoFull.getIdExecution());
            exec.setTablasIncluidas("(sin cambios)");
            exec.setLogDetalle("No se detectaron tablas modificadas desde el último FULL. " +
                    "El diferencial está vacío pero se registra como exitoso.");

            // Crear archivo vacío con metadatos para mantener la cadena
            Path vacioPath = Paths.get(TEMP_DIR,
                    String.format("backup_dif_%s_%s_empty.dump", dbName, LocalDateTime.now().format(FMT)));
            Files.writeString(vacioPath, "-- DIFERENCIAL SIN CAMBIOS: " + fechaFull);
            return vacioPath;
        }

        exec.setTablasIncluidas(String.join(",", tablasModificadas));
        log.info("Tablas modificadas para diferencial: {}", tablasModificadas);

        // 3. Ejecutar pg_dump solo con las tablas detectadas
        boolean comprimir  = Boolean.TRUE.equals(job.getComprimir());
        String  ext        = comprimir ? ".zip" : ".dump";
        String  fileName   = String.format("backup_dif_%s_%s%s", dbName, LocalDateTime.now().format(FMT), ext);
        Path    dumpPath   = Paths.get(TEMP_DIR, fileName.replace(".zip", ".dump"));
        Path    outputPath = Paths.get(TEMP_DIR, fileName);

        String pgDumpCmd = (job.getPgDumpPath() != null && !job.getPgDumpPath().isBlank())
                ? job.getPgDumpPath().trim() : "pg_dump";

        // Construir comando con --table por cada tabla modificada
        List<String> cmd = new ArrayList<>();
        cmd.add(pgDumpCmd);
        cmd.addAll(List.of("-h", job.getPgHost(), "-p", String.valueOf(job.getPgPort()),
                "-U", job.getPgUsuario(), "-d", dbName, "-F", "c", "-f", dumpPath.toString()));

        for (String tabla : tablasModificadas) {
            cmd.add("--table=" + tabla);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        return ejecutarPgDump(pb, dumpPath, outputPath, comprimir);
    }

    /**
     * Consulta pg_stat_user_tables para detectar tablas con actividad DML
     * (inserts, updates, deletes) desde la fecha del último FULL.
     *
     * Estrategia:
     * - n_tup_ins + n_tup_upd + n_tup_del > 0 indica actividad reciente
     * - last_autovacuum / last_autoanalyze como indicadores secundarios
     * - Si last_autovacuum > fechaFull → la tabla tuvo suficiente actividad
     *   para que PostgreSQL la procesara automáticamente
     */
    private List<String> detectarTablasModificadas(String host, int port, String usuario,
                                                   String password, String dbName,
                                                   LocalDateTime fechaFull) {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        List<String> tablas = new ArrayList<>();

        String sql = """
                SELECT schemaname, relname,
                       n_tup_ins, n_tup_upd, n_tup_del,
                       last_autovacuum, last_autoanalyze,
                       last_analyze
                FROM pg_stat_user_tables
                WHERE schemaname = 'public'
                  AND (
                      -- Tabla con actividad DML registrada
                      (n_tup_ins + n_tup_upd + n_tup_del) > 0
                      OR
                      -- Tabla procesada por autovacuum después del FULL
                      last_autovacuum > ?
                      OR
                      -- Tabla analizada después del FULL (indica cambios)
                      last_autoanalyze > ?
                  )
                ORDER BY (n_tup_ins + n_tup_upd + n_tup_del) DESC
                """;

        try (Connection conn = DriverManager.getConnection(url, usuario, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, fechaFull);
            ps.setObject(2, fechaFull);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String nombre = rs.getString("relname");
                long   total  = rs.getLong("n_tup_ins") + rs.getLong("n_tup_upd") + rs.getLong("n_tup_del");

                // Incluir tabla si tiene actividad o si fue procesada post-FULL
                tablas.add(schema + "." + nombre);
                log.debug("Tabla modificada: {}.{} ops={}", schema, nombre, total);
            }

        } catch (Exception e) {
            log.error("Error consultando pg_stat_user_tables: {}", e.getMessage());
            throw new RuntimeException("No se pudo detectar tablas modificadas: " + e.getMessage());
        }

        return tablas;
    }

    // ── Helper compartido para ejecutar pg_dump y comprimir ────

    private Path ejecutarPgDump(ProcessBuilder pb, Path dumpPath, Path outputPath, boolean comprimir) throws Exception {
        Process process  = pb.start();
        String  output   = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);

        if (!finished) { process.destroyForcibly(); throw new RuntimeException("pg_dump superó 10 minutos"); }
        if (process.exitValue() != 0) throw new RuntimeException("pg_dump falló: " + output);
        if (!Files.exists(dumpPath) || Files.size(dumpPath) == 0) throw new RuntimeException("pg_dump generó archivo vacío");

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

    // ── Prueba de conexión y listado de DBs ────────────────────

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

    public List<String> listarDatabases(String host, int port, String usuario, String password) {
        String url = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        List<String> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, usuario, password);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT datname FROM pg_database " +
                             "WHERE datistemplate = false " +
                             "AND datname NOT IN ('postgres','template0','template1') " +
                             "ORDER BY datname")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(rs.getString("datname"));
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

    // ── Mapper request → entidad ───────────────────────────────

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

        if (req.getDestinos() != null) {
            Map<Long, BackupDestination> existentes = new HashMap<>();
            for (BackupDestination ex : job.getDestinos()) {
                if (ex.getIdDestination() != null) existentes.put(ex.getIdDestination(), ex);
            }
            job.getDestinos().clear();

            for (BackupDestinationRequest dr : req.getDestinos()) {
                BackupDestination d = new BackupDestination();
                d.setJob(job);
                d.setTipo(dr.getTipo());
                d.setActivo(dr.getActivo() != null ? dr.getActivo() : Boolean.TRUE);

                BackupDestination existente = dr.getIdDestination() != null
                        ? existentes.get(dr.getIdDestination()) : null;

                d.setRutaLocal(dr.getRutaLocal());
                d.setAzureAccount(dr.getAzureAccount());
                d.setAzureContainer(dr.getAzureContainer());
                if (dr.getAzureKey() != null && !dr.getAzureKey().isBlank()) {
                    d.setAzureKeyEnc(encryption.encrypt(dr.getAzureKey()));
                } else if (existente != null) {
                    d.setAzureKeyEnc(existente.getAzureKeyEnc());
                }

                d.setGdriveCuenta(dr.getGdriveCuenta() != null ? dr.getGdriveCuenta()
                        : (existente != null ? existente.getGdriveCuenta() : null));
                d.setGdriveFolderNombre(dr.getGdriveFolderNombre());

                String nombreNuevo     = dr.getGdriveFolderNombre();
                String nombreExistente = existente != null ? existente.getGdriveFolderNombre() : null;
                boolean nombreCambiado = nombreNuevo != null && !nombreNuevo.isBlank()
                        && !nombreNuevo.equals(nombreExistente);

                if (nombreCambiado) {
                    d.setGdriveFolderId(null);
                    log.info("Nombre carpeta Drive cambió '{}' -> '{}', limpiando folderId", nombreExistente, nombreNuevo);
                } else {
                    d.setGdriveFolderId(dr.getGdriveFolderId() != null ? dr.getGdriveFolderId()
                            : (existente != null ? existente.getGdriveFolderId() : null));
                }

                if (existente != null && existente.getGdriveRefreshTokenEnc() != null) {
                    d.setGdriveRefreshTokenEnc(existente.getGdriveRefreshTokenEnc());
                }

                d.setS3Bucket(dr.getS3Bucket());
                d.setS3Region(dr.getS3Region());
                d.setS3AccessKey(dr.getS3AccessKey());
                if (dr.getS3SecretKey() != null && !dr.getS3SecretKey().isBlank()) {
                    d.setS3SecretKeyEnc(encryption.encrypt(dr.getS3SecretKey()));
                } else if (existente != null) {
                    d.setS3SecretKeyEnc(existente.getS3SecretKeyEnc());
                }

                d.setRetencionMeses(dr.getRetencionMeses() != null ? dr.getRetencionMeses() : 0);
                d.setRetencionDias(dr.getRetencionDias()   != null ? dr.getRetencionDias()   : 0);
                d.setMaxBackups(dr.getMaxBackups()         != null ? dr.getMaxBackups()       : 0);

                job.getDestinos().add(d);
            }
        }
    }
}