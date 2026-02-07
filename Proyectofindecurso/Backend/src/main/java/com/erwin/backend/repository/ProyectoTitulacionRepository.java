package com.erwin.backend.repository;

import com.erwin.backend.entities.ProyectoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProyectoTitulacionRepository extends JpaRepository<ProyectoTitulacion, Integer> {
    Optional<ProyectoTitulacion> findByPropuesta_IdPropuesta(Integer idPropuesta);
    List<ProyectoTitulacion> findByDirector_IdDocente(Integer IdDocente);
    long countByEstado(String estado);
}