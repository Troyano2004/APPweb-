package com.erwin.backend.repository;

import com.erwin.backend.entities.DocenteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocenteCarreraRepository extends JpaRepository<DocenteCarrera, Integer> {

    List<DocenteCarrera> findByCarrera_IdCarreraAndActivoTrue(Integer idCarrera);

    boolean existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(
            Integer idDocente,
            Integer idCarrera
    );

    List<DocenteCarrera> findByCarrera_IdCarrera(Integer idCarrera);


    List<DocenteCarrera> findByDocente_IdDocente(Integer idDocente);
    List<DocenteCarrera> findByActivoTrue();
    boolean existsByDocente_IdDocenteAndActivoTrue(Integer idDocente);
    Optional<DocenteCarrera> findByDocente_IdDocenteAndCarrera_IdCarrera(Integer idDocente, Integer idCarrera);

}