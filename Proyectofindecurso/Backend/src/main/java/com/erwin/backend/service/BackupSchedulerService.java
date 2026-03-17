package com.erwin.backend.service;

import com.erwin.backend.entities.BackupExecution.TipoBackup;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupJobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupSchedulerService {

    private final TaskScheduler       taskScheduler;
    private final BackupJobRepository jobRepo;
    private final BackupService       backupService;

    private final Map<Long, ScheduledFuture<?>> tareasFullMap = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> tareasDifMap  = new ConcurrentHashMap<>();

    @PostConstruct
    public void inicializarTareas() {
        log.info("Inicializando schedulers de backup...");
        jobRepo.findByActivoTrue().forEach(this::programarJob);
    }

    public void programarJob(BackupJob job) {
        cancelarJob(job.getIdJob());
        if (!Boolean.TRUE.equals(job.getActivo())) return;

        if (job.getCronFull() != null && !job.getCronFull().isBlank()) {
            try {
                CronTrigger        cron  = new CronTrigger(job.getCronFull(), resolverZona(job.getZonaHoraria()));
                ScheduledFuture<?> tarea = taskScheduler.schedule(
                        () -> ejecutarSiNoEnVentana(job.getIdJob(), TipoBackup.FULL), cron);
                tareasFullMap.put(job.getIdJob(), tarea);
                log.info("Full backup programado: job={} cron={}", job.getIdJob(), job.getCronFull());
            } catch (Exception e) {
                log.error("Error programando full backup job={}", job.getIdJob(), e);
            }
        }

        if (Boolean.TRUE.equals(job.getDiferencialActivo())
                && job.getCronDiferencial() != null
                && !job.getCronDiferencial().isBlank()) {
            try {
                CronTrigger        cron  = new CronTrigger(job.getCronDiferencial(), resolverZona(job.getZonaHoraria()));
                ScheduledFuture<?> tarea = taskScheduler.schedule(
                        () -> ejecutarSiNoEnVentana(job.getIdJob(), TipoBackup.DIFERENCIAL), cron);
                tareasDifMap.put(job.getIdJob(), tarea);
                log.info("Diferencial programado: job={} cron={}", job.getIdJob(), job.getCronDiferencial());
            } catch (Exception e) {
                log.error("Error programando diferencial job={}", job.getIdJob(), e);
            }
        }
    }

    public void cancelarJob(Long jobId) {
        cancelarTarea(tareasFullMap.remove(jobId));
        cancelarTarea(tareasDifMap.remove(jobId));
    }

    private void ejecutarSiNoEnVentana(Long jobId, TipoBackup tipo) {
        try {
            BackupJob job = jobRepo.findById(jobId).orElse(null);
            if (job == null || !Boolean.TRUE.equals(job.getActivo())) return;
            if (estaEnVentanaExclusion(job)) {
                log.info("Backup omitido por ventana de exclusión: job={}", jobId);
                return;
            }
            backupService.ejecutarConReintento(job, tipo, false);
        } catch (Exception e) {
            log.error("Error en ejecución scheduled job={}", jobId, e);
        }
    }

    private boolean estaEnVentanaExclusion(BackupJob job) {
        if (job.getVentanaExcluirInicio() == null || job.getVentanaExcluirFin() == null) return false;
        try {
            ZoneId        zoneId = ZoneId.of(job.getZonaHoraria() != null ? job.getZonaHoraria() : "UTC");
            ZonedDateTime now    = ZonedDateTime.now(zoneId);
            int           hora   = now.getHour() * 60 + now.getMinute();
            String[]      ini    = job.getVentanaExcluirInicio().split(":");
            String[]      fin    = job.getVentanaExcluirFin().split(":");
            int inicio = Integer.parseInt(ini[0]) * 60 + Integer.parseInt(ini[1]);
            int finMin = Integer.parseInt(fin[0]) * 60 + Integer.parseInt(fin[1]);
            return hora >= inicio && hora <= finMin;
        } catch (Exception e) {
            return false;
        }
    }

    private TimeZone resolverZona(String zona) {
        if (zona == null || zona.isBlank()) return TimeZone.getTimeZone("America/Guayaquil");
        return TimeZone.getTimeZone(zona);
    }

    private void cancelarTarea(ScheduledFuture<?> tarea) {
        if (tarea != null && !tarea.isCancelled()) tarea.cancel(false);
    }
}