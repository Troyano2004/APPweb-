package com.erwin.backend.repository;

import com.erwin.backend.entities.AnteproyectoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnteproyectoTitulacionRepository extends JpaRepository<AnteproyectoTitulacion, Integer> {
    Optional<AnteproyectoTitulacion> findByPropuesta_IdPropuesta(Integer IdpPropuesta);

    List<AnteproyectoTitulacion> findByCarrera_IdCarreraAndEleccion_Periodo_IdPeriodoAndEstadoIgnoreCase(
            Integer idCarrera,
            Integer idPeriodo,
            String estado
    );

    List<AnteproyectoTitulacion> findByEstudiante_IdEstudianteAndEleccion_Periodo_IdPeriodoAndEstadoIgnoreCase(
            Integer idEstudiante, Integer idPeriodo, String estado);
}
