package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import com.erwin.backend.entities.BackupExecution.TipoBackup;
import com.erwin.backend.entities.BackupJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {

    // ── Historial paginado ─────────────────────────────────────────────────────
    Page<BackupExecution> findByJobOrderByIniciadoEnDesc(BackupJob job, Pageable pageable);

    // ── Stats ──────────────────────────────────────────────────────────────────
    List<BackupExecution> findByIniciadoEnAfter(LocalDateTime desde);

    Optional<BackupExecution> findTopByOrderByIniciadoEnDesc();

    // ── Diferencial: último FULL exitoso de un job/BD ──────────────────────────
    Optional<BackupExecution> findTopByJob_IdJobAndDatabaseNombreAndTipoBackupAndEstadoOrderByIniciadoEnDesc(
            Long idJob, String databaseNombre, TipoBackup tipoBackup, EstadoEjecucion estado);

    // ── Timeline: todas las ejecuciones exitosas de un job ────────────────────
    @Query("SELECT e FROM BackupExecution e WHERE e.job.idJob = :idJob AND e.estado = :estado ORDER BY e.iniciadoEn DESC")
    List<BackupExecution> findByJob_IdJobAndEstado(
            @Param("idJob") Long idJob,
            @Param("estado") EstadoEjecucion estado);

    // ── Diferenciales hijos de un FULL ────────────────────────────────────────
    List<BackupExecution> findByIdBackupPadreOrderByIniciadoEnAsc(Long idBackupPadre);
}