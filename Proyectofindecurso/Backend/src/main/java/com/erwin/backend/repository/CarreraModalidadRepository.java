package com.erwin.backend.repository;

import com.erwin.backend.entities.Carreramodalidad;
import com.erwin.backend.entities.Carreramodalidadid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarreraModalidadRepository extends JpaRepository<Carreramodalidad, Carreramodalidadid> {
    boolean existsById_IdCarreraAndId_IdModalidadAndActivoTrue(Integer idCarrera, Integer idIdModalidad);

    List<Carreramodalidad> findById_IdCarreraAndActivoTrue(Integer idCarrera);
}
