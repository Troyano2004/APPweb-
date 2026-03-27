package com.erwin.backend.service;

import com.erwin.backend.config.DataSourceConfig;
import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupDestinationRepository;
import com.erwin.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryService {

    private static final String TEMP_DIR        = System.getProperty("java.io.tmpdir") + "/recovery_tmp/";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

    // ID ficticio que identifica el job de emergencia (no existe en BD)
    private static final Long EMERGENCY_JOB_ID = -1L;

    private static final java.util.regex.Pattern SAFE_DB_NAME =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    private final BackupJobRepository         jobRepo;
    private final BackupDestinationRepository destinationRepo;
    private final GoogleDriveOAuthService     oauthService;
    private final BackupEncryptionUtil        encryption;
    private final RestTemplate                restTemplate;

    @Value("${recovery.password}")
    private String recoveryPassword;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    // ── Propiedades del job de emergencia (hardcodeadas en application.properties) ──
    @Value("${recovery.emergency.refresh-token}")
    private String emergencyRefreshToken;

    @Value("${recovery.emergency.folder-id}")
    private String emergencyFolderId;

    @Value("${recovery.emergency.pg-host}")
    private String emergencyPgHost;

    @Value("${recovery.emergency.pg-port}")
    private int emergencyPgPort;

    @Value("${recovery.emergency.pg-usuario}")
    private String emergencyPgUsuario;

    @Value("${recovery.emergency.pg-password-enc}")
    private String emergencyPgPasswordEnc;

    @Value("${recovery.emergency.pg-dump-path}")
    private String emergencyPgDumpPath;

    @Value("${recovery.emergency.databases}")
    private String emergencyDatabases;

    // ── Verificar si la BD está disponible SIN usar JPA/Hikari ────────────────

    private boolean bdDisponible() {
        return DataSourceConfig.baseDeDatosExiste(datasourceUrl, datasourceUsername, datasourcePassword);
    }

    // ── Autenticación ──────────────────────────────────────────────────────────

    public boolean validarPassword(String password) {
        return recoveryPassword != null && recoveryPassword.equals(password);
    }

    // ── Listar jobs disponibles ────────────────────────────────────────────────

    public List<Map<String, Object>> listarJobs() {
        List<Map<String, Object>> lista = new ArrayList<>();

        if (bdDisponible()) {
            // BD disponible — leer jobs normalmente desde la base de datos
            List<Map<String, Object>> jobsBd = jobRepo.findAll().stream()
                    .map(j -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("idJob",     j.getIdJob());
                        m.put("nombre",    j.getNombre());
                        m.put("databases", j.getDatabases());
                        m.put("pgHost",    j.getPgHost());
                        m.put("pgPort",    j.getPgPort());
                        m.put("pgUsuario", j.getPgUsuario());
                        boolean tieneDrive = j.getDestinos().stream()
                                .anyMatch(d -> d.getTipo() != null
                                        && d.getTipo().name().equals("GOOGLE_DRIVE")
                                        && Boolean.TRUE.equals(d.getActivo())
                                        && d.getGdriveRefreshTokenEnc() != null
                                        && !d.getGdriveRefreshTokenEnc().isBlank());
                        m.put("tieneDrive", tieneDrive);
                        m.put("esEmergencia", false);
                        return m;
                    })
                    .collect(Collectors.toList());
            lista.addAll(jobsBd);
        } else {
            // BD no disponible — inyectar el job de emergencia desde application.properties
            log.warn("Recovery: BD no disponible, usando job de emergencia en memoria");
            lista.add(construirJobEmergencia());
        }

        return lista;
    }

    /**
     * Construye el job de emergencia en memoria usando los valores de application.properties.
     * Este job NO existe en la BD — tiene id=-1 como señal especial.
     */
    private Map<String, Object> construirJobEmergencia() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("idJob",        EMERGENCY_JOB_ID);
        job.put("nombre",       "🔥 RECOVERY DE EMERGENCIA (En Memoria)");
        job.put("databases",    emergencyDatabases);
        job.put("pgHost",       emergencyPgHost);
        job.put("pgPort",       emergencyPgPort);
        job.put("pgUsuario",    emergencyPgUsuario);
        job.put("tieneDrive",   true);
        job.put("esEmergencia", true);
        return job;
    }

    // ── Listar backups en Google Drive ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarBackupsEnDrive(Long idJob) {
        String accessToken;
        String folderId;

        if (EMERGENCY_JOB_ID.equals(idJob)) {
            // Job de emergencia: usar token y folder de application.properties directamente
            log.info("Recovery: listando backups con credenciales de emergencia");
            accessToken = oauthService.obtenerAccessToken(emergencyRefreshToken);
            folderId    = emergencyFolderId;
        } else {
            if (!bdDisponible()) {
                throw new RuntimeException("BD no disponible. Usa el job de emergencia (id=-1).");
            }
            BackupJob job = jobRepo.findById(idJob)
                    .orElseThrow(() -> new RuntimeException("Job no encontrado: " + idJob));
            BackupDestination destino = resolverDestinoDrive(job);
            accessToken = oauthService.obtenerAccessToken(destino.getGdriveRefreshTokenEnc());
            folderId    = destino.getGdriveFolderId();
        }

        String query = "mimeType != 'application/vnd.google-apps.folder'"
                + " and trashed = false"
                + " and (name contains 'backup_full_' or name contains 'backup_dif_')";

        if (folderId != null && !folderId.isBlank()) {
            query += " and '" + folderId + "' in parents";
        }

        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(DRIVE_FILES_URL)
                .queryParam("q", query)
                .queryParam("fields", "files(id,name,size,createdTime,modifiedTime)")
                .queryParam("orderBy", "createdTime desc")
                .queryParam("pageSize", "50")
                .encode().build().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        if (response.getBody() == null) return List.of();

        List<Map<String, Object>> files =
                (List<Map<String, Object>>) response.getBody().get("files");
        if (files == null) return List.of();

        return files.stream().map(f -> {
            Map<String, Object> item = new LinkedHashMap<>();
            String nombre = String.valueOf(f.getOrDefault("name", ""));
            item.put("fileId",        f.getOrDefault("id", ""));
            item.put("nombre",        nombre);
            item.put("tamano",        f.getOrDefault("size", "0"));
            item.put("fechaCreacion", f.getOrDefault("createdTime", ""));
            item.put("tipo",          nombre.contains("backup_full_") ? "FULL" : "DIFERENCIAL");
            return item;
        }).collect(Collectors.toList());
    }


    // ── Listar backups en carpeta local ───────────────────────────────────────

    @Value("${recovery.local.backup-path:C:\\backups}")
    private String localBackupPath;

    public List<Map<String, Object>> listarBackupsLocales(Long idJob) throws Exception {
        Path carpeta = Paths.get(localBackupPath);

        if (!Files.exists(carpeta) || !Files.isDirectory(carpeta)) {
            throw new RuntimeException(
                    "Carpeta de backups no encontrada: " + localBackupPath +
                            ". Configura recovery.local.backup-path en application.properties");
        }

        return Files.list(carpeta)
                .filter(p -> p.getFileName().toString().endsWith(".zip"))
                .filter(p -> {
                    String n = p.getFileName().toString();
                    return n.contains("backup_full_") || n.contains("backup_dif_");
                })
                .sorted((a, b) -> {
                    try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                    catch (Exception e) { return 0; }
                })
                .map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String nombre = p.getFileName().toString();
                    try {
                        item.put("fileId",        p.toAbsolutePath().toString()); // ruta completa como ID
                        item.put("nombre",        nombre);
                        item.put("tamano",        String.valueOf(p.toFile().length()));
                        item.put("fechaCreacion", Files.getLastModifiedTime(p).toInstant().toString());
                        item.put("tipo",          nombre.contains("backup_full_") ? "FULL" : "DIFERENCIAL");
                        item.put("fuente",        "LOCAL");
                    } catch (Exception e) {
                        log.warn("Error leyendo metadata de {}: {}", nombre, e.getMessage());
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ── Ejecutar restauración ──────────────────────────────────────────────────

    public Map<String, Object> ejecutarRestore(Long idJob, String fileId,
                                               String nombreArchivo, String modoRestore,
                                               String nombreBdNueva) throws Exception {
        Files.createDirectories(Paths.get(TEMP_DIR));

        // Resolver credenciales según si es job de emergencia o normal
        final String pgHost, pgUsuario, pgPassword, pgDumpPath, databases;
        final int    pgPort;
        final String refreshToken;
        final String folderId;

        if (EMERGENCY_JOB_ID.equals(idJob)) {
            log.warn("Recovery EMERGENCIA: iniciando restauración archivo={} modo={}", nombreArchivo, modoRestore);
            pgHost     = emergencyPgHost;
            pgPort     = emergencyPgPort;
            pgUsuario  = emergencyPgUsuario;
            pgPassword = encryption.decrypt(emergencyPgPasswordEnc);
            pgDumpPath = emergencyPgDumpPath;
            databases  = emergencyDatabases;
            refreshToken = emergencyRefreshToken;
            folderId     = emergencyFolderId;
        } else {
            if (!bdDisponible()) {
                throw new RuntimeException("BD no disponible. Usa el job de emergencia.");
            }
            BackupJob job = jobRepo.findById(idJob)
                    .orElseThrow(() -> new RuntimeException("Job no encontrado: " + idJob));
            BackupDestination destino = resolverDestinoDrive(job);
            pgHost     = job.getPgHost();
            pgPort     = job.getPgPort();
            pgUsuario  = job.getPgUsuario();
            pgPassword = encryption.decrypt(job.getPgPasswordEnc());
            pgDumpPath = job.getPgDumpPath();
            databases  = job.getDatabases();
            refreshToken = destino.getGdriveRefreshTokenEnc();
            folderId     = destino.getGdriveFolderId();
            log.info("Recovery: iniciando restauración job={} archivo={} modo={}", idJob, nombreArchivo, modoRestore);
        }

        long inicio = System.currentTimeMillis();

        // 1. Obtener archivo — desde Drive o desde carpeta local
        // Si fileId es una ruta absoluta que existe en disco → LOCAL, si no → DRIVE
        Path archivoLocal;
        boolean esLocal = Paths.get(fileId).toFile().exists();

        if (esLocal) {
            archivoLocal = Paths.get(fileId);
            log.info("Usando archivo local: {}", archivoLocal);
        } else {
            String accessToken = oauthService.obtenerAccessToken(refreshToken);
            archivoLocal = descargarDeDrive(fileId, nombreArchivo, accessToken);
            log.info("Archivo descargado de Drive: {}", archivoLocal);
        }

        // 2. Descomprimir si es ZIP
        Path archivoRestore = archivoLocal;
        if (nombreArchivo.endsWith(".zip")) {
            archivoRestore = descomprimirZip(archivoLocal);
            // Solo borrar el ZIP descargado si vino de Drive (el local lo conservamos)
            if (!esLocal) Files.deleteIfExists(archivoLocal);
            log.info("Archivo descomprimido: {}", archivoRestore);
        }

        // 3. Determinar BD destino
        String dbDestino;
        if ("NUEVA_BD".equals(modoRestore) && nombreBdNueva != null && !nombreBdNueva.isBlank()) {
            dbDestino = nombreBdNueva.trim();
        } else {
            dbDestino = extraerDbDelNombre(nombreArchivo, databases);
        }

        if (!SAFE_DB_NAME.matcher(dbDestino).matches()) {
            throw new RuntimeException("Nombre de base de datos inválido: '" + dbDestino + "'");
        }

        // 4. Crear BD si no existe o si es modo nueva BD
        crearBaseDeDatosIfNotExists(pgHost, pgPort, pgUsuario, pgPassword, dbDestino);

        // 5. Ejecutar pg_restore
        String pgRestoreCmd = resolverPgRestoreCmd(pgDumpPath);
        String logRestore   = ejecutarPgRestore(pgRestoreCmd, pgHost, pgPort,
                pgUsuario, pgPassword, dbDestino, archivoRestore);

        // Solo borrar el dump temporal si NO era el archivo local original
        if (!esLocal || nombreArchivo.endsWith(".zip")) Files.deleteIfExists(archivoRestore);

        long duracion = (System.currentTimeMillis() - inicio) / 1000;
        log.info("Recovery completado en {}s — BD: {}", duracion, dbDestino);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("exitoso",          true);
        resultado.put("mensaje",          "Restauración completada en " + duracion + "s");
        resultado.put("bdRestaurada",     dbDestino);
        resultado.put("duracionSegundos", duracion);
        resultado.put("log",              logRestore);
        return resultado;
    }

    // ── Descarga desde Google Drive (recibe accessToken directo) ──────────────

    private Path descargarDeDrive(String fileId, String nombreArchivo,
                                  String accessToken) throws Exception {
        String downloadUrl = DRIVE_FILES_URL + "/" + fileId + "?alt=media";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl, HttpMethod.GET,
                new HttpEntity<>(headers), byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error descargando archivo de Drive: " + fileId);
        }

        Path destPath = Paths.get(TEMP_DIR, nombreArchivo);
        Files.write(destPath, response.getBody());
        return destPath;
    }

    // ── Descomprimir ZIP ───────────────────────────────────────────────────────

    private Path descomprimirZip(Path zipPath) throws Exception {
        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            java.util.zip.ZipEntry entry = zis.getNextEntry();
            if (entry == null) throw new RuntimeException("ZIP vacío o inválido");
            Path dumpPath = Paths.get(TEMP_DIR, entry.getName());
            Files.copy(zis, dumpPath, StandardCopyOption.REPLACE_EXISTING);
            return dumpPath;
        }
    }

    // ── pg_restore ─────────────────────────────────────────────────────────────

    private String ejecutarPgRestore(String pgRestoreCmd, String host, int port,
                                     String usuario, String password,
                                     String dbName, Path archivoBackup) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(
                pgRestoreCmd,
                "-h", host,
                "-p", String.valueOf(port),
                "-U", usuario,
                "-d", dbName,
                "--clean",      // limpia objetos existentes antes de restaurar
                "--if-exists",  // no falla si un objeto no existe al limpiar
                "-v",           // verbose para el log
                archivoBackup.toString()
                // NOTA: NO usamos --no-owner ni --no-privileges para que los
                // permisos y dueños se restauren exactamente como estaban en el backup
        ));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        Process process  = pb.start();
        String  output   = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_restore superó 30 minutos");
        }

        if (process.exitValue() >= 2) {
            throw new RuntimeException(
                    "pg_restore falló (exit=" + process.exitValue() + "): " + output);
        }

        return output;
    }

    // ── Crear BD si no existe ──────────────────────────────────────────────────

    private void crearBaseDeDatosIfNotExists(String host, int port, String usuario,
                                             String password, String dbName) {
        if (!SAFE_DB_NAME.matcher(dbName).matches()) {
            throw new RuntimeException("Nombre de BD inválido: " + dbName);
        }
        String url = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        try (Connection conn = DriverManager.getConnection(url, usuario, password);
             PreparedStatement check = conn.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?");
             Statement st = conn.createStatement()) {

            check.setString(1, dbName);
            if (!check.executeQuery().next()) {
                st.execute("CREATE DATABASE \"" + dbName + "\"");
                log.info("BD creada: {}", dbName);
            } else {
                log.info("BD ya existe, restaurando encima: {}", dbName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creando BD '" + dbName + "': " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extraerDbDelNombre(String nombreArchivo, String databasesDelJob) {
        try {
            String sin = nombreArchivo.replace(".zip", "").replace(".dump", "");
            sin = sin.replaceFirst("^backup_(full|dif)_", "");
            sin = sin.replaceAll("_\\d{8}_\\d{6}$", "");
            return sin;
        } catch (Exception e) {
            if (databasesDelJob != null && !databasesDelJob.isBlank())
                return databasesDelJob.split(",")[0].trim();
            return "restored_db";
        }
    }

    private String resolverPgRestoreCmd(String pgDumpPath) {
        if (pgDumpPath == null || pgDumpPath.isBlank()) return "pg_restore";
        return pgDumpPath.trim()
                .replace("pg_dump.exe", "pg_restore.exe")
                .replace("pg_dump",     "pg_restore");
    }

    private BackupDestination resolverDestinoDrive(BackupJob job) {
        return job.getDestinos().stream()
                .filter(d -> d.getTipo() != null
                        && d.getTipo().name().equals("GOOGLE_DRIVE")
                        && Boolean.TRUE.equals(d.getActivo())
                        && d.getGdriveRefreshTokenEnc() != null
                        && !d.getGdriveRefreshTokenEnc().isBlank())
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Este job no tiene Google Drive configurado y conectado."));
    }

    // ── Verificar conexión PG ──────────────────────────────────────────────────

    public boolean verificarConexionPg(Long idJob) {
        String host, usuario, password;
        int    port;

        if (EMERGENCY_JOB_ID.equals(idJob)) {
            host     = emergencyPgHost;
            port     = emergencyPgPort;
            usuario  = emergencyPgUsuario;
            password = encryption.decrypt(emergencyPgPasswordEnc);
        } else {
            if (!bdDisponible()) return false;
            BackupJob job = jobRepo.findById(idJob).orElse(null);
            if (job == null) return false;
            host     = job.getPgHost();
            port     = job.getPgPort();
            usuario  = job.getPgUsuario();
            password = encryption.decrypt(job.getPgPasswordEnc());
        }

        String url = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        try (Connection conn = DriverManager.getConnection(url, usuario, password)) {
            return conn.isValid(3);
        } catch (Exception e) {
            log.debug("Ping PG falló job={}: {}", idJob, e.getMessage());
            return false;
        }
    }
}