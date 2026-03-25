package com.erwin.backend.repository;

import com.erwin.backend.entities.SustentacionFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SustentacionFinalRepository extends JpaRepository<SustentacionFinal, Integer> {

    /** Busca la sustentación principal (no segunda oportunidad) de un proyecto */
    Optional<SustentacionFinal> findByProyecto_IdProyectoAndEsSegundaOportunidadFalse(Integer idProyecto);

    /** Busca la segunda oportunidad de un proyecto */
    Optional<SustentacionFinal> findByProyecto_IdProyectoAndEsSegundaOportunidadTrue(Integer idProyecto);

    /** Verifica si ya existe sustentación programada para el proyecto */
    boolean existsByProyecto_IdProyectoAndEsSegundaOportunidadFalse(Integer idProyecto);
}