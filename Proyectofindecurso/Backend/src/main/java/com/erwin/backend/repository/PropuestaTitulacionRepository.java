package com.erwin.backend.repository;

import com.erwin.backend.entities.PropuestaTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropuestaTitulacionRepository extends JpaRepository<PropuestaTitulacion, Integer> {
    List<PropuestaTitulacion> findByEstudiante_IdEstudiante(Integer IdEstudiante);
    List<PropuestaTitulacion> findByEstado(String estado);

    Optional<PropuestaTitulacion> findByEleccion_IdEleccion(Integer IdEleccion);
}
