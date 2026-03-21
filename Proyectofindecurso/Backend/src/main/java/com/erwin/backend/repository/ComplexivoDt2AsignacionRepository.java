package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoDt2Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComplexivoDt2AsignacionRepository
        extends JpaRepository<ComplexivoDt2Asignacion, Integer> {

    Optional<ComplexivoDt2Asignacion>
    findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    boolean existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    List<ComplexivoDt2Asignacion> findByPeriodo_IdPeriodoAndActivoTrue(
            Integer idPeriodo);

    @Query("""
        SELECT c FROM ComplexivoDt2Asignacion c
        WHERE c.periodo.idPeriodo = :idPeriodo
          AND c.activo = true
          AND c.carrera.idCarrera = :idCarrera
    """)
    List<ComplexivoDt2Asignacion> findByCarreraAndPeriodoActivo(
            Integer idCarrera, Integer idPeriodo);
}