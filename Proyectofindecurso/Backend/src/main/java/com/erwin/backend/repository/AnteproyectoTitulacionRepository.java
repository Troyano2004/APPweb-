package com.erwin.backend.repository;

import com.erwin.backend.entities.AnteproyectoTitulacion;
import jakarta.persistence.LockModeType;
import org.hibernate.LockMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnteproyectoTitulacionRepository extends JpaRepository<AnteproyectoTitulacion, Integer> {
    Optional<AnteproyectoTitulacion> findByPropuesta_IdPropuesta(Integer IdpPropuesta);



    List<AnteproyectoTitulacion> findByEstudiante_IdEstudianteAndEleccion_Periodo_IdPeriodoAndEstadoIgnoreCase(
            Integer idEstudiante, Integer idPeriodo, String estado);


    List<AnteproyectoTitulacion> findByCarrera_IdCarreraAndEleccion_Periodo_IdPeriodoAndEstadoInIgnoreCase(
            Integer idCarrera,
            Integer idPeriodo,
            List<String> estados
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AnteproyectoTitulacion a where a.idAnteproyecto = :id")
    Optional<AnteproyectoTitulacion> findByIdForUpdate(@Param("id") Integer idAnteproyecto);



}