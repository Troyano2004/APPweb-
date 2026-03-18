package com.erwin.backend.repository;

import com.erwin.backend.entities.EleccionTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleccionTitulacionRepository extends JpaRepository<EleccionTitulacion, Integer> {

    List<EleccionTitulacion> findByEstudiante_IdEstudiante(Integer idEstudiante);

    Optional<EleccionTitulacion> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
            Integer idEstudiante, Integer idPeriodo);

    List<EleccionTitulacion> findByCarrera_IdCarreraAndModalidad_NombreAndPeriodo_IdPeriodoAndEstado(
            Integer idCarrera, String nombre, Integer idPeriodo, String estado);
}