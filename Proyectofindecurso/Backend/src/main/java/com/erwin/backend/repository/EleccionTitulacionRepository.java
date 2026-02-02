package com.erwin.backend.repository;

import com.erwin.backend.entities.Elecciontitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleccionTitulacionRepository extends JpaRepository<Elecciontitulacion, Integer> {
    List<Elecciontitulacion> findByEstudiante_IdEstudiante(Integer IdEstudiante);

    Optional<Elecciontitulacion> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(Integer IdEstudiante, Integer IdPeriodo);
}
