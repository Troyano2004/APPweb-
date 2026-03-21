package com.erwin.backend.service;

import com.erwin.backend.dtos.BackupTimelineDtos.*;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import com.erwin.backend.entities.BackupExecution.TipoBackup;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.repository.BackupExecutionRepository;
import com.erwin.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupTimelineService {

    private final BackupJobRepository       jobRepo;
    private final BackupExecutionRepository execRepo;

    @Transactional(readOnly = true)
    public TimelineResponseDto obtenerTimeline(Long idJob) {
        BackupJob job = jobRepo.findById(idJob)
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + idJob));

        // Traer TODAS las ejecuciones exitosas del job (FULL y DIFERENCIAL)
        List<BackupExecution> todas = execRepo
                .findByJob_IdJobAndEstado(idJob, EstadoEjecucion.EXITOSO);

        // Separar FULLs — más recientes primero
        List<BackupExecution> fulls = todas.stream()
                .filter(e -> e.getTipoBackup() == TipoBackup.FULL)
                .sorted((a, b) -> b.getIniciadoEn().compareTo(a.getIniciadoEn()))
                .toList();

        List<BackupCadenaDto> cadenas = new ArrayList<>();
        int  totalDiff  = 0;
        long tamanoTotal = 0;

        for (BackupExecution full : fulls) {
            // Buscar diferenciales hijos de este FULL
            List<BackupExecution> hijos = execRepo
                    .findByIdBackupPadreOrderByIniciadoEnAsc(full.getIdExecution());

            List<BackupNodoDto> nodosHijos = hijos.stream()
                    .map(this::toNodo)
                    .toList();

            long tamanoFull  = full.getTamanoBytes() != null ? full.getTamanoBytes() : 0L;
            long tamanoDiffs = hijos.stream()
                    .mapToLong(h -> h.getTamanoBytes() != null ? h.getTamanoBytes() : 0L)
                    .sum();

            BackupNodoDto nodoFull = toNodo(full);
            nodoFull.setNumeroDiferenciales(nodosHijos.size());

            cadenas.add(new BackupCadenaDto(
                    nodoFull, nodosHijos,
                    nodosHijos.size(),
                    tamanoFull + tamanoDiffs
            ));

            totalDiff   += nodosHijos.size();
            tamanoTotal += tamanoFull + tamanoDiffs;
        }

        return new TimelineResponseDto(
                idJob, job.getNombre(), cadenas,
                fulls.size(), totalDiff, tamanoTotal
        );
    }

    private BackupNodoDto toNodo(BackupExecution exec) {
        BackupNodoDto n = new BackupNodoDto();
        n.setIdExecution(exec.getIdExecution());
        n.setTipo(exec.getTipoBackup() != null ? exec.getTipoBackup().name() : "FULL");
        n.setEstado(exec.getEstado() != null ? exec.getEstado().name() : "");
        n.setDatabaseNombre(exec.getDatabaseNombre());
        n.setIniciadoEn(exec.getIniciadoEn());
        n.setFinalizadoEn(exec.getFinalizadoEn());
        n.setDuracionSegundos(exec.getDuracionSegundos());
        n.setTamanoBytes(exec.getTamanoBytes());
        n.setArchivoNombre(exec.getArchivoNombre());
        n.setDestinoTipo(exec.getDestinoTipo());
        n.setTablasIncluidas(exec.getTablasIncluidas());
        n.setIdBackupPadre(exec.getIdBackupPadre());
        n.setManual(exec.getManual());
        n.setErrorMensaje(exec.getErrorMensaje());
        n.setNumeroDiferenciales(0);

        // Verificar disponibilidad del archivo
        boolean disponible = false;
        if (exec.getArchivoRuta() != null && !exec.getArchivoRuta().isBlank()) {
            try { disponible = Files.exists(Paths.get(exec.getArchivoRuta())); }
            catch (Exception ignored) {}
        }
        // Para destinos remotos (Drive, S3, Azure) marcar disponible si fue exitoso
        if (!disponible && exec.getDestinoTipo() != null
                && !exec.getDestinoTipo().equalsIgnoreCase("LOCAL")) {
            disponible = exec.getEstado() == EstadoEjecucion.EXITOSO;
        }
        n.setArchivoDisponible(disponible);

        return n;
    }
}