package com.erwin.backend.repository;

import com.erwin.backend.entities.CalificacionSustentacionFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalificacionSustentacionFinalRepository
        extends JpaRepository<CalificacionSustentacionFinal, Integer> {

    /** Todas las calificaciones de un proyecto */
    List<CalificacionSustentacionFinal> findByProyecto_IdProyecto(Integer idProyecto);

    /** Calificación de un docente específico en un proyecto */
    Optional<CalificacionSustentacionFinal> findByProyecto_IdProyectoAndDocente_IdDocente(
            Integer idProyecto, Integer idDocente);

    /** Cuántos miembros del tribunal ya calificaron */
    long countByProyecto_IdProyecto(Integer idProyecto);

    /** Verifica si un docente ya calificó este proyecto */
    boolean existsByProyecto_IdProyectoAndDocente_IdDocente(Integer idProyecto, Integer idDocente);
}