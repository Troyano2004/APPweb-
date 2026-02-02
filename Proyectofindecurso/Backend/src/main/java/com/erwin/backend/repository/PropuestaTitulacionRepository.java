package com.erwin.backend.repository;

import com.erwin.backend.entities.Elecciontitulacion;
import com.erwin.backend.entities.Propuestatitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropuestaTitulacionRepository extends JpaRepository<Propuestatitulacion, Integer> {
    List<Propuestatitulacion> findByEstudiante_IdEstudiante(Integer IdEstudiante);
    List<Propuestatitulacion> findByEstado(String estado);

    Optional<Propuestatitulacion> findByEleccion_IdEleccion(Integer IdEleccion);
}
