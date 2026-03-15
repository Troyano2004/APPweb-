package com.erwin.backend.repository;

import com.erwin.backend.entities.ZoomConfigDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoomConfigDocenteRepository extends JpaRepository<ZoomConfigDocente, Integer> {
    Optional<ZoomConfigDocente> findByDocente_IdDocente(Integer idDocente);
}

