package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoPrevioSustentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoPrevioSustentacionRepository extends JpaRepository<DocumentoPrevioSustentacion, Integer> {

    Optional<DocumentoPrevioSustentacion> findByProyecto_IdProyecto(Integer idProyecto);

    boolean existsByProyecto_IdProyectoAndCompletoTrue(Integer idProyecto);
}
