package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoDocenteAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComplexivoDocenteAsignacionRepository
        extends JpaRepository<ComplexivoDocenteAsignacion, Integer> {

    Optional<ComplexivoDocenteAsignacion> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    List<ComplexivoDocenteAsignacion> findByPeriodo_IdPeriodoAndActivoTrue(Integer idPeriodo);

    boolean existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante, Integer idPeriodo);

    @Query("""
        SELECT c FROM ComplexivoDocenteAsignacion c
        WHERE c.periodo.idPeriodo = :idPeriodo
          AND c.activo = true
          AND c.carrera.idCarrera = :idCarrera
    """)
    List<ComplexivoDocenteAsignacion> findByCarreraAndPeriodoActivo(
            Integer idCarrera, Integer idPeriodo);
}