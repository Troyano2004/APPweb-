package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoDt1Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComplexivoDt1AsignacionRepository
        extends JpaRepository<ComplexivoDt1Asignacion, Integer> {

    Optional<ComplexivoDt1Asignacion>
    findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    boolean existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    List<ComplexivoDt1Asignacion> findByPeriodo_IdPeriodoAndActivoTrue(
            Integer idPeriodo);

    @Query("""
        SELECT c FROM ComplexivoDt1Asignacion c
        WHERE c.periodo.idPeriodo = :idPeriodo
          AND c.activo = true
          AND c.carrera.idCarrera = :idCarrera
    """)
    List<ComplexivoDt1Asignacion> findByCarreraAndPeriodoActivo(
            Integer idCarrera, Integer idPeriodo);
}