package com.erwin.backend.repository;

import com.erwin.backend.entities.TribunalProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TribunalProyectoRepository extends JpaRepository<TribunalProyecto, Integer> {
    List<TribunalProyecto> findByProyecto_IdProyecto(Integer idProyecto);
    long countByProyecto_IdProyecto(Integer idProyecto);
    void deleteByProyecto_IdProyecto(Integer idProyecto);
    boolean existsByProyecto_IdProyectoAndDocente_IdDocente(Integer idProyecto, Integer idDocente);

    // ✅ NUEVO: para obtener todos los proyectos donde un docente es tribunal
    List<TribunalProyecto> findByDocente_IdDocente(Integer idDocente);
}