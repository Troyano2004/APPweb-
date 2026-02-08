package com.erwin.backend.repository;

import com.erwin.backend.entities.ObservacionAdministrativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacionAdministrativaRepository extends JpaRepository<ObservacionAdministrativa, Integer> {
    List<ObservacionAdministrativa> findByProyecto_IdProyectoOrderByCreadoEnDesc(Integer idProyecto);
}
