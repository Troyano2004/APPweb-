package com.erwin.backend.repository;

import com.erwin.backend.entities.Dt2Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Dt2AsignacionRepository extends JpaRepository<Dt2Asignacion, Integer> {

    Optional<Dt2Asignacion> findByProyecto_IdProyectoAndActivoTrue(Integer idProyecto);

    boolean existsByProyecto_IdProyecto(Integer idProyecto);

    // ✅ NUEVO: proyectos asignados al Docente DT2
    List<Dt2Asignacion> findByDocenteDt2_IdDocenteAndActivoTrue(Integer idDocente);
}




































