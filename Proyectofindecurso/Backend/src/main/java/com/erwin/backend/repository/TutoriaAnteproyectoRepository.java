package com.erwin.backend.repository;

import com.erwin.backend.entities.TutoriaAnteproyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutoriaAnteproyectoRepository extends JpaRepository<TutoriaAnteproyecto, Integer> {

    List<TutoriaAnteproyecto> findByAnteproyecto_IdAnteproyectoOrderByFechaAsc(Integer idAnteproyecto);
    Optional<TutoriaAnteproyecto> findTopByAnteproyecto_IdAnteproyectoAndDocente_IdDocenteOrderByIdTutoriaDesc(
            Integer idAnteproyecto,
            Integer idDocente
    );
    List<TutoriaAnteproyecto> findByAnteproyecto_IdAnteproyectoAndDocente_IdDocenteOrderByFechaAsc(
            Integer idAnteproyecto,
            Integer idDocente
    );
    List<TutoriaAnteproyecto> findByDocente_IdDocenteOrderByFechaDesc(Integer idDocente);

    Optional<TutoriaAnteproyecto> findByIdTutoria(Integer idTutoria);
    List<TutoriaAnteproyecto> findByAnteproyecto_Estudiante_IdEstudianteAndEstado(
            Integer idEstudiante, String estado
    );

}