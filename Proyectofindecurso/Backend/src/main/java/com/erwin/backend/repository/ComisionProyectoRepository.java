package com.erwin.backend.repository;

import com.erwin.backend.entities.ComisionProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComisionProyectoRepository extends JpaRepository<ComisionProyecto, Integer> {
    Optional<ComisionProyecto> findByProyecto_IdProyecto(Integer idProyecto);
}
