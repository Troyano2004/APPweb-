package com.erwin.backend.repository;

import com.erwin.backend.entities.EvaluacionSustentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluacionSustentacionRepository extends JpaRepository<EvaluacionSustentacion, Integer> {

    List<EvaluacionSustentacion> findBySustentacion_IdSustentacion(Integer idSustentacion);

    List<EvaluacionSustentacion> findBySustentacion_IdSustentacionAndTipo(Integer idSustentacion, String tipo);

    Optional<EvaluacionSustentacion> findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
            Integer idSustentacion, Integer idDocente, String tipo);

    long countBySustentacion_IdSustentacionAndTipo(Integer idSustentacion, String tipo);
}
