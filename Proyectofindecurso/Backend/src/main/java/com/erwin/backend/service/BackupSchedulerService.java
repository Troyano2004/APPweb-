
package com.erwin.backend.service;

import com.erwin.backend.config.DbContextHolder;
import com.erwin.backend.dtos.BackupDto;
import com.erwin.backend.entities.BackupConfiguracion;
import com.erwin.backend.entities.BackupHistorial;
import com.erwin.backend.repository.BackupConfiguracionRepository;
import com.erwin.backend.repository.BackupHistorialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Scheduler del módulo de respaldos.
 *
 * ── ¿Por qué este servicio NO puede usar DbContextHolder? ────────────────────
 * El scheduler corre en un hilo propio del sistema, no dentro de un request HTTP.
 * Eso significa que DbSessionFilter nunca se ejecutó para ese hilo,
 * por lo tanto DbContextHolder.getUser() devuelve null.
 *
 * Solución: el scheduler necesita sus propias credenciales de BD para pg_dump.
 * Estas se configuran en application.properties con las credenciales del
 * usuario admin de la BD (app_admin o el que tenga permisos de dump).
 *
 * ── Separación de responsabilidades ─────────────────────────────────────────
 * - BackupService:          usado cuando el ADMIN presiona el botón manual.
 *                           Lee credenciales del DbContextHolder (JWT del admin).
 * - BackupSchedulerService: usado cuando corre automáticamente por el scheduler.
 *                           Lee credenciales de application.properties.
 */
@Service
@EnableScheduling
public class BackupSchedulerService {

    private final BackupConfiguracionRepository configRepo;
    private final BackupHistorialRepository     historialRepo;
    private final GoogleDriveService            driveService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${backup.pgdump.path:pg_dump}")
    private String pgDumpPath;

    // Estas credenciales son SOLO para el scheduler automático.
    // Deben ser las del usuario app_admin (o cualquier usuario con permisos de dump).
    // Configurarlas en application.properties.
    @Value("${backup.scheduler.db-user:app_admin}")
    private String schedulerDbUser;

    @Value("${backup.scheduler.db-pass}")
    private String schedulerDbPass;

    // Evitar ejecutar más de una vez en el mismo minuto
    private String ultimoMinutoEjecutado = "";

    public BackupSchedulerService(BackupConfiguracionRepository configRepo,
                                  BackupHistorialRepository historialRepo,
                                  GoogleDriveService driveService) {
        this.configRepo    = configRepo;
        this.historialRepo = historialRepo;
        this.driveService  = driveService;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SCHEDULER — corre cada minuto
    // ──────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 * * * * *")
    public void verificarYEjecutar() {
        try {
            BackupConfiguracion cfg = configRepo.findAll().stream().findFirst().orElse(null);

            if (cfg == null)                                    return;
            if (!Boolean.TRUE.equals(cfg.getProgramadoActivo())) return;
            if (cfg.getHoraProgramada() == null)               return;

            String horaAhora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String horaCfg   = cfg.getHoraProgramada().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (horaAhora.equals(horaCfg) && !ultimoMinutoEjecutado.equals(horaAhora)) {
                ultimoMinutoEjecutado = horaAhora;

                System.out.println("⏰ BACKUP AUTOMÁTICO iniciado — hora: " + horaAhora
                        + " | usuario BD: " + schedulerDbUser);

                BackupDto.EjecucionResultDto resultado =
                        ejecutarRespaldoScheduler(cfg);

                if (resultado.isExitoso()) {
                    System.out.println("✅ BACKUP AUTOMÁTICO completado: " + resultado.getMensaje());
                } else {
                    System.err.println("❌ BACKUP AUTOMÁTICO falló: " + resultado.getMensaje());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error en el scheduler de backup: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lógica interna del respaldo automático
    // (igual que BackupService.ejecutarRespaldo() pero usa schedulerDbUser)
    // ──────────────────────────────────────────────────────────────────────────

    private BackupDto.EjecucionResultDto ejecutarRespaldoScheduler(BackupConfiguracion cfg) {

        BackupHistorial historial = new BackupHistorial();
        historial.setTipoRespaldo(cfg.getTipoRespaldo());

        try {
            Path dirDestino = Path.of(cfg.getRutaLocal());
            Files.createDirectories(dirDestino);

            String timestamp  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dbName     = extraerNombreBd(datasourceUrl);
            String tipo       = cfg.getTipoRespaldo().toLowerCase();
            String nombreSql  = "backup_" + dbName + "_" + tipo + "_" + timestamp + ".sql";
            Path   archivoSql = dirDestino.resolve(nombreSql);

            // Usar schedulerDbUser/Pass (de application.properties)
            ejecutarPgDump(dbName, archivoSql, cfg.getTipoRespaldo(),
                    schedulerDbUser, schedulerDbPass);

            Path archivoFinal;
            if (Boolean.TRUE.equals(cfg.getComprimir())) {
                archivoFinal = comprimirEnZip(archivoSql, dirDestino, timestamp, dbName, tipo);
                borrarArchivoFisico(archivoSql);
            } else {
                archivoFinal = archivoSql;
            }

            historial.setNombreArchivo(archivoFinal.getFileName().toString());
            historial.setRutaCompleta(archivoFinal.toAbsolutePath().toString());
            historial.setTamanioBytes(Files.size(archivoFinal));
            historial.setEstado("EXITOSO");

            if (Boolean.TRUE.equals(cfg.getGuardarEnDrive()) && cfg.getDriveFolderId() != null) {
                try {
                    String driveId = driveService.subirArchivo(archivoFinal, cfg.getDriveFolderId());
                    historial.setEnDrive(true);
                    historial.setDriveFileId(driveId);
                } catch (Exception driveEx) {
                    historial.setMensajeError(
                            "Backup local OK. Error al subir a Drive: " + driveEx.getMessage()
                    );
                }
            }

            BackupHistorial guardado = historialRepo.save(historial);
            rotarBackupsViejos(cfg);

            return new BackupDto.EjecucionResultDto(
                    true, "Respaldo automático completado", toHistorialDto(guardado)
            );

        } catch (Exception e) {
            historial.setEstado("FALLIDO");
            historial.setMensajeError(e.getMessage());
            historial.setNombreArchivo(
                    "AUTO_ERROR_" + LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );
            historialRepo.save(historial);
            return new BackupDto.EjecucionResultDto(
                    false, "Error en respaldo automático: " + e.getMessage(), toHistorialDto(historial)
            );
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — pg_dump
    // ──────────────────────────────────────────────────────────────────────────

    private void ejecutarPgDump(String dbName, Path destino, String tipoRespaldo,
                                String dbUser, String dbPass)
            throws IOException, InterruptedException {

        String host = "localhost";
        String port = "5432";
        String sinJdbc = datasourceUrl.replace("jdbc:postgresql://", "");
        String[] partes = sinJdbc.split("/");
        if (partes.length > 0) {
            String[] hostPort = partes[0].split(":");
            host = hostPort[0];
            if (hostPort.length > 1) port = hostPort[1];
        }

        ProcessBuilder pb;
        switch (tipoRespaldo.toUpperCase()) {
            case "DIFERENCIAL":
                pb = new ProcessBuilder(pgDumpPath,
                        "-h", host, "-p", port, "-U", dbUser,
                        "--data-only", "--format=plain",
                        "-f", destino.toAbsolutePath().toString(), dbName);
                break;
            case "INCREMENTAL":
                pb = new ProcessBuilder(pgDumpPath,
                        "-h", host, "-p", port, "-U", dbUser,
                        "--format=plain", "--section=pre-data", "--section=data",
                        "-f", destino.toAbsolutePath().toString(), dbName);
                break;
            default:
                pb = new ProcessBuilder(pgDumpPath,
                        "-h", host, "-p", port, "-U", dbUser,
                        "--format=plain", "--clean", "--if-exists",
                        "-f", destino.toAbsolutePath().toString(), dbName);
        }

        pb.environment().put("PGPASSWORD", dbPass);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();
        StringBuilder salida = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = br.readLine()) != null) salida.append(linea).append("\n");
        }

        int exitCode = proceso.waitFor();
        if (exitCode != 0) {
            throw new IOException("pg_dump terminó con código " + exitCode + ":\n" + salida);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — ZIP / Rotación / Utilidades
    // ──────────────────────────────────────────────────────────────────────────

    private Path comprimirEnZip(Path archivoSql, Path directorio,
                                String timestamp, String dbName, String tipo)
            throws IOException {
        String nombreZip = "backup_" + dbName + "_" + tipo + "_" + timestamp + ".zip";
        Path   zipPath   = directorio.resolve(nombreZip);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
             FileInputStream  fis = new FileInputStream(archivoSql.toFile())) {
            zos.putNextEntry(new ZipEntry(archivoSql.getFileName().toString()));
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
            zos.closeEntry();
        }
        return zipPath;
    }

    private void rotarBackupsViejos(BackupConfiguracion cfg) {
        int max = cfg.getCantidadMaxima() != null ? cfg.getCantidadMaxima() : 10;
        List<BackupHistorial> exitosos = historialRepo.findExitososMasAntiguosPrimero();
        while (exitosos.size() > max) {
            BackupHistorial viejo = exitosos.remove(0);
            if (viejo.getRutaCompleta() != null) borrarArchivoFisico(Path.of(viejo.getRutaCompleta()));
            if (Boolean.TRUE.equals(viejo.getEnDrive()) && viejo.getDriveFileId() != null)
                driveService.eliminarArchivo(viejo.getDriveFileId());
            historialRepo.delete(viejo);
        }
    }

    private String extraerNombreBd(String jdbcUrl) {
        try {
            String sin = jdbcUrl.replace("jdbc:postgresql://", "");
            return sin.split("/")[1].split("\\?")[0];
        } catch (Exception e) { return "database"; }
    }

    private void borrarArchivoFisico(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { System.err.println("⚠️ No se pudo borrar: " + path); }
    }

    private BackupDto.EjecucionResultDto toResultDto(boolean ok, String msg, BackupHistorial h) {
        return new BackupDto.EjecucionResultDto(ok, msg, h != null ? toHistorialDto(h) : null);
    }

    private BackupDto.HistorialDto toHistorialDto(BackupHistorial e) {
        BackupDto.HistorialDto d = new BackupDto.HistorialDto();
        d.setId(e.getId());
        d.setNombreArchivo(e.getNombreArchivo());
        d.setRutaCompleta(e.getRutaCompleta());
        d.setTipoRespaldo(e.getTipoRespaldo());
        d.setTamanioBytes(e.getTamanioBytes());
        d.setEnDrive(e.getEnDrive());
        d.setDriveFileId(e.getDriveFileId());
        d.setEstado(e.getEstado());
        d.setMensajeError(e.getMensajeError());
        d.setFechaCreacion(e.getFechaCreacion());
        return d;
    }
}