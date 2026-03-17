package com.erwin.backend.repository;

import com.erwin.backend.entities.Dt1Revision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Dt1RevisionRepository extends JpaRepository<Dt1Revision, Integer> {
    Optional<Dt1Revision> findTopByAnteproyecto_IdAnteproyectoOrderByFechaRevisionDesc(Integer idAnteproyecto);

}
