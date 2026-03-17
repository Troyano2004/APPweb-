package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {

    List<BackupJob> findByActivoTrue();

    Optional<BackupJob> findByNombre(String nombre);

    // Carga el job con sus destinos en una sola query (evita LazyInitializationException)
    @Query("SELECT j FROM BackupJob j LEFT JOIN FETCH j.destinos WHERE j.idJob = :id")
    Optional<BackupJob> findByIdWithDestinos(@Param("id") Long id);
}