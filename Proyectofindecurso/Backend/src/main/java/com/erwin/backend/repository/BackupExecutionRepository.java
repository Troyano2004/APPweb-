package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {

    Page<BackupExecution> findByJobOrderByIniciadoEnDesc(BackupJob job, Pageable pageable);

    @Query("SELECT e FROM BackupExecution e WHERE e.job.idJob = :jobId " +
            "ORDER BY e.iniciadoEn DESC")
    List<BackupExecution> findUltimasByJob(@Param("jobId") Long jobId, Pageable pageable);

    @Query("SELECT e FROM BackupExecution e " +
            "WHERE e.job.idJob = :jobId " +
            "AND e.iniciadoEn BETWEEN :desde AND :hasta " +
            "ORDER BY e.iniciadoEn DESC")
    List<BackupExecution> findByJobAndRangoFecha(
            @Param("jobId") Long jobId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT e FROM BackupExecution e " +
            "WHERE e.job.idJob = :jobId " +
            "AND e.estado = 'EXITOSO' " +
            "AND e.iniciadoEn < :fechaLimite " +
            "ORDER BY e.iniciadoEn ASC")
    List<BackupExecution> findAntiguasPorLimpiar(
            @Param("jobId")       Long jobId,
            @Param("fechaLimite") LocalDateTime fechaLimite);
}