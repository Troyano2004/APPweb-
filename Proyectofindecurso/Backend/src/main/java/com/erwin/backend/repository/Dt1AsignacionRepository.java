package com.erwin.backend.repository;

import com.erwin.backend.entities.Dt1Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Dt1AsignacionRepository extends JpaRepository<Dt1Asignacion, Integer> {

    List<Dt1Asignacion> findByDocente_IdDocenteAndActivoTrue(Integer idDocente);

    boolean existsByDocente_IdDocenteAndCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(
            Integer idDocente,
            Integer idCarrera,
            Integer idPeriodo
    );
    List<Dt1Asignacion> findByCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(Integer idCarrera, Integer idPeriodo);

}