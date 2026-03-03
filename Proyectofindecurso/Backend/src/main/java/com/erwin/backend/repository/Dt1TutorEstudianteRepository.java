package com.erwin.backend.repository;

import com.erwin.backend.entities.Dt1TutorEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Dt1TutorEstudianteRepository extends JpaRepository<Dt1TutorEstudiante, Integer> {

    // 🔎 Buscar tutor activo de un estudiante en un periodo
    Optional<Dt1TutorEstudiante> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante,
            Integer idPeriodo
    );

    // 🔎 Verificar si el estudiante ya tiene tutor
    boolean existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idEstudiante,
            Integer idPeriodo
    );

    // 🔎 Validar que un docente sea tutor del estudiante en ese periodo
    boolean existsByDocente_IdDocenteAndEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idDocente,
            Integer idEstudiante,
            Integer idPeriodo
    );

    // 🔎 Listar todos los tutorados de un docente en un periodo
    List<Dt1TutorEstudiante> findByDocente_IdDocenteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idDocente,
            Integer idPeriodo
    );

    // 🔎 Listar todos los tutores activos del periodo (útil para filtros)
    List<Dt1TutorEstudiante> findByPeriodo_IdPeriodoAndActivoTrue(
            Integer idPeriodo
    );



    Optional<Dt1TutorEstudiante> findByDocente_IdDocenteAndEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idDocente,
            Integer idEstudiante,
            Integer idPeriodo
    );


}