package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoTitulacion;
import com.erwin.backend.enums.EstadoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoTitulacionRepository extends JpaRepository<DocumentoTitulacion, Integer> {
    Optional<DocumentoTitulacion> findByEstudiante_IdEstudiante(Integer idEstudiante);
    List<DocumentoTitulacion> findByDirector_IdDocenteAndEstado(Integer idDocente, EstadoDocumento estado);
    List<DocumentoTitulacion> findByDirector_IdDocente(Integer idDocente);
    long countByEstado(EstadoDocumento estado);
    Optional<DocumentoTitulacion> findByProyecto_IdProyecto(Integer idProyecto);
    List<DocumentoTitulacion> findByDirectorIsNull();
}
