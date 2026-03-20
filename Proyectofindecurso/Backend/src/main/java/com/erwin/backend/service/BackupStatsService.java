package com.erwin.backend.service;

import com.erwin.backend.dtos.BackupStatsDto;
import com.erwin.backend.dtos.IntegrityResultDto;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupExecutionRepository;
import com.erwin.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupStatsService {

    private final BackupJobRepository       jobRepo;
    private final BackupExecutionRepository execRepo;

    // ── Estadísticas generales ─────────────────────────────────────────────────

    public BackupStatsDto obtenerEstadisticas() {
        List<BackupJob> todosJobs   = jobRepo.findAll();
        long jobsActivos            = todosJobs.stream().filter(j -> Boolean.TRUE.equals(j.getActivo())).count();

        // Ejecuciones del último mes
        LocalDateTime haceMes       = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<BackupExecution> mesEjec = execRepo.findByIniciadoEnAfter(haceMes);

        long exitosos = mesEjec.stream()
                .filter(e -> e.getEstado() != null && e.getEstado().name().equals("EXITOSO")).count();
        long fallidos = mesEjec.stream()
                .filter(e -> e.getEstado() != null && e.getEstado().name().equals("FALLIDO")).count();
        double tasa   = mesEjec.isEmpty() ? 0 : Math.round((exitosos * 100.0 / mesEjec.size()) * 10) / 10.0;

        // Tamaño acumulado
        long tamanoTotal = mesEjec.stream()
                .filter(e -> e.getTamanoBytes() != null)
                .mapToLong(BackupExecution::getTamanoBytes).sum();

        // Último backup
        BackupExecution ultimo = execRepo
                .findTopByOrderByIniciadoEnDesc()
                .orElse(null);

        // Próxima ejecución
        BackupJob proxJob = todosJobs.stream()
                .filter(j -> Boolean.TRUE.equals(j.getActivo()) && j.getProximaEjecucion() != null)
                .min((a, b) -> a.getProximaEjecucion().compareTo(b.getProximaEjecucion()))
                .orElse(null);

        return BackupStatsDto.builder()
                .jobsActivos(jobsActivos)
                .jobsTotal(todosJobs.size())
                .totalEjecucionesMes(mesEjec.size())
                .exitososMes(exitosos)
                .fallidosMes(fallidos)
                .tasaExitoMes(tasa)
                .tamanoAcumuladoBytes(tamanoTotal)
                .tamanoUltimoBackup(ultimo != null && ultimo.getTamanoBytes() != null ? ultimo.getTamanoBytes() : 0L)
                .ultimoBackupFecha(ultimo != null ? ultimo.getIniciadoEn() : null)
                .ultimoBackupEstado(ultimo != null && ultimo.getEstado() != null ? ultimo.getEstado().name() : null)
                .ultimoBackupJob(ultimo != null && ultimo.getJob() != null ? ultimo.getJob().getNombre() : null)
                .proximaEjecucion(proxJob != null ? proxJob.getProximaEjecucion() : null)
                .proximaEjecucionJob(proxJob != null ? proxJob.getNombre() : null)
                .build();
    }

    // ── Verificación de integridad ─────────────────────────────────────────────

    public IntegrityResultDto verificarIntegridad(Long idExecution, BackupJob job) {
        BackupExecution exec = execRepo.findById(idExecution)
                .orElseThrow(() -> new RuntimeException("Ejecución no encontrada: " + idExecution));

        String ruta = exec.getArchivoRuta();
        if (ruta == null || ruta.isBlank()) {
            return new IntegrityResultDto(false, "No hay ruta de archivo registrada", 0, 0, null);
        }

        Path archivoPath = Paths.get(ruta);
        if (!Files.exists(archivoPath)) {
            return new IntegrityResultDto(false, "Archivo no encontrado en: " + ruta, 0, 0, null);
        }

        try {
            long tamano = Files.size(archivoPath);

            // Si es ZIP, extraer el .dump a un temporal
            Path dumpPath = archivoPath;
            Path tempDump = null;
            if (ruta.toLowerCase().endsWith(".zip")) {
                tempDump = extraerZipTemp(archivoPath);
                if (tempDump == null) {
                    return new IntegrityResultDto(false, "El archivo ZIP está corrupto o vacío", 0, tamano, null);
                }
                dumpPath = tempDump;
            }

            // Ejecutar pg_restore --list para verificar sin restaurar
            int objetos = verificarConPgRestore(dumpPath, job);

            // Limpiar temporal
            if (tempDump != null) Files.deleteIfExists(tempDump);

            if (objetos >= 0) {
                return new IntegrityResultDto(true,
                        "Archivo válido — " + objetos + " objetos encontrados", objetos, tamano, null);
            } else {
                return new IntegrityResultDto(false, "El archivo dump está corrupto", 0, tamano, null);
            }

        } catch (Exception e) {
            log.error("Error verificando integridad de ejecución {}", idExecution, e);
            return new IntegrityResultDto(false, "Error durante verificación: " + e.getMessage(), 0, 0, e.getMessage());
        }
    }

    private Path extraerZipTemp(Path zipPath) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir") + "/backup_verify/";
        Files.createDirectories(Paths.get(tempDir));

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            var entry = zis.getNextEntry();
            if (entry == null) return null;
            Path out = Paths.get(tempDir, "verify_" + System.currentTimeMillis() + ".dump");
            Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
            return out;
        }
    }

    private int verificarConPgRestore(Path dumpPath, BackupJob job) throws Exception {
        String pgDumpDir = "";
        if (job.getPgDumpPath() != null && !job.getPgDumpPath().isBlank()) {
            Path parent = Paths.get(job.getPgDumpPath()).getParent();
            if (parent != null) pgDumpDir = parent.toString() + "/";
        }
        String pgRestoreCmd = pgDumpDir.isBlank() ? "pg_restore" : pgDumpDir + "pg_restore";

        ProcessBuilder pb = new ProcessBuilder(pgRestoreCmd, "--list", dumpPath.toString());
        pb.redirectErrorStream(true);

        Process process  = pb.start();
        String  output   = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(2, TimeUnit.MINUTES);

        if (!finished) { process.destroyForcibly(); return -1; }
        if (process.exitValue() != 0) return -1;

        // Contar líneas que representan objetos (no comentarios)
        long objetos = output.lines()
                .filter(l -> !l.isBlank() && !l.startsWith(";"))
                .count();
        return (int) objetos;
    }

    // ── Política de retención automática ──────────────────────────────────────
    // Corre cada noche a las 2:00 AM

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void aplicarPoliticaRetencion() {
        log.info("Iniciando política de retención de backups...");
        List<BackupJob> jobs = jobRepo.findAll();

        for (BackupJob job : jobs) {
            job.getDestinos().forEach(destino -> {
                if (!Boolean.TRUE.equals(destino.getActivo())) return;
                if (!"LOCAL".equals(destino.getTipo().name())) return;

                List<BackupExecution> ejecuciones = execRepo
                        .findByJobOrderByIniciadoEnDesc(job, PageRequest.of(0, 10000))
                        .getContent();

                aplicarRetenciónLocal(job, destino.getRutaLocal(),
                        destino.getRetencionMeses(),
                        destino.getRetencionDias(),
                        destino.getMaxBackups(),
                        ejecuciones);
            });
        }
        log.info("Política de retención completada.");
    }

    private void aplicarRetenciónLocal(BackupJob job, String rutaLocal,
                                       Integer meses, Integer dias, Integer maxBackups,
                                       List<BackupExecution> ejecuciones) {
        LocalDateTime ahora   = LocalDateTime.now();
        LocalDateTime limiteM = (meses != null && meses > 0)
                ? ahora.minusMonths(meses) : null;
        LocalDateTime limiteD = (dias != null && dias > 0)
                ? ahora.minusDays(dias) : null;

        // Determinar el límite de fecha más estricto
        LocalDateTime limite = null;
        if (limiteM != null && limiteD != null)
            limite = limiteM.isAfter(limiteD) ? limiteM : limiteD;
        else if (limiteM != null) limite = limiteM;
        else if (limiteD != null) limite = limiteD;

        int eliminados = 0;

        for (BackupExecution exec : ejecuciones) {
            if (exec.getArchivoRuta() == null) continue;
            boolean porFecha = limite != null
                    && exec.getIniciadoEn() != null
                    && exec.getIniciadoEn().isBefore(limite);

            boolean porMaximo = maxBackups != null && maxBackups > 0
                    && ejecuciones.indexOf(exec) >= maxBackups;

            if (porFecha || porMaximo) {
                eliminarArchivoLocal(exec);
                eliminados++;
            }
        }

        if (eliminados > 0) {
            log.info("Retención job='{}': {} archivos eliminados", job.getNombre(), eliminados);
        }
    }

    private void eliminarArchivoLocal(BackupExecution exec) {
        try {
            Path p = Paths.get(exec.getArchivoRuta());
            if (Files.exists(p)) {
                Files.delete(p);
                exec.setArchivoRuta("[PURGADO]");
                execRepo.save(exec);
                log.info("Archivo purgado: {}", p);
            }
        } catch (Exception e) {
            log.warn("No se pudo eliminar archivo: {} — {}", exec.getArchivoRuta(), e.getMessage());
        }
    }

    // ── Helper formateo tamaño ─────────────────────────────────────────────────

    public static String formatBytes(long bytes) {
        if (bytes < 1024)            return bytes + " B";
        if (bytes < 1048576)         return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824)      return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }
}