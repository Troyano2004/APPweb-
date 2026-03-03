package com.erwin.backend.repository;

import com.erwin.backend.entities.PropuestaTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropuestaRepository extends JpaRepository<PropuestaTitulacion, Integer> {
    Optional<PropuestaTitulacion> findTopByEstudiante_IdEstudianteAndEstadoOrderByIdPropuestaDesc(Integer IdEstudiante, String estado);

    Optional<PropuestaTitulacion> findFirstByEstudiante_IdEstudianteOrderByIdPropuestaDesc(Integer IdEstudiante);

    // ✅ para validar tema ocupado
    boolean existsByTema_IdTemaAndEstadoIn(Integer idTema, List<String> estados);
    Optional<PropuestaTitulacion> findFirstByEstudiante_IdEstudianteAndEstadoOrderByIdPropuesta(Integer IdEstudiante, String estado);
    Optional<PropuestaTitulacion> findFirstByEstudiante_IdEstudianteAndEstadoInOrderByIdPropuesta(Integer IdEstudiante, List<String> estados);
    Optional<PropuestaTitulacion> findFirstByEstudiante_IdEstudianteAndEstadoInOrderByIdPropuestaAsc(
            Integer idEstudiante,
            Collection<String> estados
    );

}
