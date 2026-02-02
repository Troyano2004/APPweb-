package com.erwin.backend.repository;

import com.erwin.backend.entities.Proyectotitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProyectoTitulacionRepository extends JpaRepository<Proyectotitulacion, Integer> {
    Optional<Proyectotitulacion> findByPropuesta_IdPropuesta(Integer idPropuesta);
    List<Proyectotitulacion> findByDirector_IdDocente(Integer IdDocente);
}
