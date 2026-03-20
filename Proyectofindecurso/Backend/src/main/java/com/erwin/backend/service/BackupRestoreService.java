package com.erwin.backend.service;

import com.erwin.backend.dtos.RestoreRequest;
import com.erwin.backend.dtos.RestoreResponse;
import com.erwin.backend.dtos.RestoreResultado;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupExecutionRepository;
import com.erwin.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupRestoreService {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/restore_tmp/";

    private final BackupExecutionRepository execRepo;
    private final BackupJobRepository       jobRepo;
    private final BackupEncryptionUtil      encryption;

    // ── Historial con disponibilidad del archivo ───────────────────────────────

    public List<RestoreResponse> obtenerHistorialConDisponibilidad(Long jobId) {
        BackupJob job = jobRepo.findByIdWithDestinos(jobId)
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));

        // Obtener ruta del destino local configurado (para buscar archivos ahí)
        String rutaLocalDestino = job.getDestinos().stream()
                .filter(d -> d.getActivo() && "LOCAL".equals(d.getTipo().name())
                        && d.getRutaLocal() != null && !d.getRutaLocal().isBlank())
                .map(d -> d.getRutaLocal())
                .findFirst()
                .orElse(null);

        List<BackupExecution> ejecuciones = execRepo
                .findByJobOrderByIniciadoEnDesc(job,
                        org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent();

        List<RestoreResponse> result = new ArrayList<>();
        for (BackupExecution e : ejecuciones) {
            RestoreResponse r = new RestoreResponse();
            r.setIdExecution(e.getIdExecution());
            r.setIdJob(job.getIdJob());
            r.setJobNombre(job.getNombre());
            r.setDatabaseNombre(e.getDatabaseNombre());
            r.setArchivoNombre(e.getArchivoNombre());
            r.setTamanoBytes(e.getTamanoBytes());
            r.setEstado(e.getEstado().name());
            r.setIniciadoEn(e.getIniciadoEn());
            r.setDestinoTipo(e.getDestinoTipo());

            // Intentar encontrar el archivo físico
            Path archivoEncontrado = resolverArchivo(e.getArchivoRuta(), e.getArchivoNombre(), rutaLocalDestino);

            if (archivoEncontrado != null) {
                r.setArchivoRuta(archivoEncontrado.toString());
                r.setArchivoDisponible(true);
                // Actualizar la ruta en BD si era incorrecta
                if (!archivoEncontrado.toString().equals(e.getArchivoRuta())) {
                    e.setArchivoRuta(archivoEncontrado.toString());
                    execRepo.save(e);
                }
            } else {
                r.setArchivoRuta(e.getArchivoRuta());
                r.setArchivoDisponible(false);
            }

            result.add(r);
        }
        return result;
    }

    /**
     * Intenta encontrar el archivo de backup en múltiples ubicaciones:
     * 1. Ruta exacta guardada en la BD
     * 2. Carpeta del destino local + nombre del archivo
     * 3. Carpeta del destino local + nombre derivado de la fecha de ejecución
     */
    private Path resolverArchivo(String rutaGuardada, String nombreArchivo, String carpetaLocal) {
        // 1. Ruta exacta en BD
        if (rutaGuardada != null && !rutaGuardada.isBlank()) {
            try {
                Path p = Paths.get(rutaGuardada);
                if (Files.exists(p) && Files.size(p) > 0) return p;
            } catch (Exception ignored) {}
        }

        // 2. Buscar por nombre en la carpeta local configurada
        if (nombreArchivo != null && !nombreArchivo.isBlank() && carpetaLocal != null) {
            try {
                Path p = Paths.get(carpetaLocal, nombreArchivo);
                if (Files.exists(p) && Files.size(p) > 0) return p;
            } catch (Exception ignored) {}
        }

        // 3. Buscar solo el nombre del archivo (sin directorio) en la carpeta local
        if (rutaGuardada != null && !rutaGuardada.isBlank() && carpetaLocal != null) {
            try {
                String soloNombre = Paths.get(rutaGuardada).getFileName().toString();
                Path p = Paths.get(carpetaLocal, soloNombre);
                if (Files.exists(p) && Files.size(p) > 0) return p;
            } catch (Exception ignored) {}
        }

        return null;
    }

    // ── Restaurar ──────────────────────────────────────────────────────────────

    public RestoreResultado restaurar(RestoreRequest req) {
        LocalDateTime inicio = LocalDateTime.now();

        // Obtener la ejecución y el job
        BackupExecution exec = execRepo.findById(req.getIdExecution())
                .orElseThrow(() -> new RuntimeException("Ejecución no encontrada: " + req.getIdExecution()));

        BackupJob job = jobRepo.findByIdWithDestinos(req.getIdJob())
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + req.getIdJob()));

        String archivoRuta = exec.getArchivoRuta();
        if (archivoRuta == null || archivoRuta.isBlank()) {
            return new RestoreResultado(false, "La ejecución no tiene archivo de backup registrado",
                    null, null, 0L);
        }

        Path archivoPath = Paths.get(archivoRuta);
        if (!Files.exists(archivoPath)) {
            return new RestoreResultado(false,
                    "El archivo de backup no existe en la ruta: " + archivoRuta,
                    null, null, 0L);
        }

        String password = encryption.decrypt(job.getPgPasswordEnc());

        try {
            // Si el archivo es ZIP, descomprimir primero
            Path dumpPath = archivoPath;
            boolean esTemporal = false;
            if (archivoRuta.toLowerCase().endsWith(".zip")) {
                dumpPath    = descomprimirZip(archivoPath);
                esTemporal  = true;
            }

            String bdDestino;
            String log;

            if ("NUEVA_BD".equals(req.getModo())) {
                // Crear nueva BD y restaurar en ella
                bdDestino = req.getNombreBdNueva();
                crearBaseDeDatos(job, password, bdDestino);
                log = ejecutarPgRestore(job, password, dumpPath, bdDestino, false);
            } else {
                // Reemplazar BD actual — terminar conexiones y restaurar
                bdDestino = exec.getDatabaseNombre();
                terminarConexiones(job, password, bdDestino);
                log = ejecutarPgRestore(job, password, dumpPath, bdDestino, true);
            }

            // Limpiar temporal
            if (esTemporal) {
                Files.deleteIfExists(dumpPath);
            }

            long duracion = ChronoUnit.SECONDS.between(inicio, LocalDateTime.now());
            return new RestoreResultado(true,
                    "Restauración completada exitosamente en la base de datos: " + bdDestino,
                    log, bdDestino, duracion);

        } catch (Exception e) {
            log.error("Error durante restauración", e);
            long duracion = ChronoUnit.SECONDS.between(inicio, LocalDateTime.now());
            return new RestoreResultado(false,
                    "Error durante la restauración: " + e.getMessage(),
                    e.getMessage(), null, duracion);
        }
    }

    // ── Crear nueva base de datos ──────────────────────────────────────────────

    private void crearBaseDeDatos(BackupJob job, String password, String nombreBd) throws Exception {
        String url = String.format("jdbc:postgresql://%s:%d/postgres",
                job.getPgHost(), job.getPgPort());
        try (Connection conn = DriverManager.getConnection(url, job.getPgUsuario(), password);
             Statement stmt = conn.createStatement()) {
            // Verificar si ya existe
            var rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + nombreBd + "'");
            if (rs.next()) {
                throw new RuntimeException("La base de datos '" + nombreBd + "' ya existe. Elige otro nombre.");
            }
            stmt.execute("CREATE DATABASE \"" + nombreBd + "\"");
            log.info("Base de datos '{}' creada para restauración", nombreBd);
        }
    }

    // ── Terminar conexiones activas ────────────────────────────────────────────

    private void terminarConexiones(BackupJob job, String password, String nombreBd) throws Exception {
        String url = String.format("jdbc:postgresql://%s:%d/postgres",
                job.getPgHost(), job.getPgPort());
        try (Connection conn = DriverManager.getConnection(url, job.getPgUsuario(), password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "SELECT pg_terminate_backend(pid) " +
                            "FROM pg_stat_activity " +
                            "WHERE datname = '" + nombreBd + "' AND pid <> pg_backend_pid()");
            log.info("Conexiones terminadas en '{}'", nombreBd);
        }
    }

    // ── Ejecutar pg_restore ────────────────────────────────────────────────────

    private String ejecutarPgRestore(BackupJob job, String password,
                                     Path dumpPath, String bdDestino,
                                     boolean limpiarAntes) throws Exception {
        String pgDumpDir = "";
        if (job.getPgDumpPath() != null && !job.getPgDumpPath().isBlank()) {
            pgDumpDir = Paths.get(job.getPgDumpPath()).getParent() != null
                    ? Paths.get(job.getPgDumpPath()).getParent().toString() + "/"
                    : "";
        }

        String pgRestoreCmd = pgDumpDir.isBlank() ? "pg_restore" : pgDumpDir + "pg_restore";

        List<String> cmd = new ArrayList<>();
        cmd.add(pgRestoreCmd);
        cmd.add("-h"); cmd.add(job.getPgHost());
        cmd.add("-p"); cmd.add(String.valueOf(job.getPgPort()));
        cmd.add("-U"); cmd.add(job.getPgUsuario());
        cmd.add("-d"); cmd.add(bdDestino);
        cmd.add("--no-owner");
        cmd.add("--no-privileges");
        if (limpiarAntes) {
            cmd.add("--clean");        // DROP objetos antes de recrear
            cmd.add("--if-exists");    // no fallar si no existen
        }
        cmd.add("-v");                 // verbose para el log
        cmd.add(dumpPath.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        Process process  = pb.start();
        String  output   = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_restore superó 30 minutos, cancelado");
        }

        int exitCode = process.exitValue();
        // pg_restore puede devolver exit code 1 con warnings no fatales
        if (exitCode > 1) {
            throw new RuntimeException("pg_restore falló (exit " + exitCode + "): " + output);
        }

        log.info("pg_restore completado en '{}' (exit={})", bdDestino, exitCode);
        return output;
    }

    // ── Descomprimir ZIP ───────────────────────────────────────────────────────

    private Path descomprimirZip(Path zipPath) throws IOException {
        Files.createDirectories(Paths.get(TEMP_DIR));

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) throw new IOException("ZIP vacío: " + zipPath);

            Path outPath = Paths.get(TEMP_DIR, entry.getName());
            Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("ZIP descomprimido: {} -> {}", zipPath.getFileName(), outPath.getFileName());
            return outPath;
        }
    }

    // ── Verificar disponibilidad de un archivo ─────────────────────────────────

    public boolean archivoDisponible(String ruta) {
        if (ruta == null || ruta.isBlank()) return false;
        try {
            Path p = Paths.get(ruta);
            return Files.exists(p) && Files.size(p) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}