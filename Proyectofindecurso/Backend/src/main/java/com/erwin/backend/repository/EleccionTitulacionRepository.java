package com.erwin.backend.repository;

import com.erwin.backend.entities.EleccionTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleccionTitulacionRepository extends JpaRepository<EleccionTitulacion, Integer> {
    List<EleccionTitulacion> findByEstudiante_IdEstudiante(Integer IdEstudiante);

    Optional<EleccionTitulacion> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
            Integer IdEstudiante, Integer IdPeriodo);

    // NUEVO: buscar la elección activa del estudiante para saber su modalidad
    Optional<EleccionTitulacion> findByEstudiante_IdEstudianteAndEstado(
            Integer idEstudiante, String estado);
}