package com.erwin.backend.repository;

import com.erwin.backend.entities.DocenteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocenteCarreraRepository extends JpaRepository<DocenteCarrera, Integer> {

    List<DocenteCarrera> findByCarrera_IdCarreraAndActivoTrue(Integer idCarrera);

    boolean existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(
            Integer idDocente,
            Integer idCarrera
    );
}