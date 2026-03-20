package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {

    Page<BackupExecution> findByJobOrderByIniciadoEnDesc(BackupJob job, Pageable pageable);

    List<BackupExecution> findByIniciadoEnAfter(LocalDateTime fecha);

    Optional<BackupExecution> findTopByOrderByIniciadoEnDesc();
}