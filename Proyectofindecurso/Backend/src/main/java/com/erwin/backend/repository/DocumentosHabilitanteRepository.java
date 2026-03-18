
package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentosHabilitantes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentosHabilitanteRepository extends JpaRepository<DocumentosHabilitantes, Integer> {

    /** Todos los habilitantes de un proyecto */
    List<DocumentosHabilitantes> findByProyecto_IdProyecto(Integer idProyecto);

    /** Todos los habilitantes de un estudiante */
    List<DocumentosHabilitantes> findByEstudiante_IdEstudiante(Integer idEstudiante);

    /** Un tipo específico dentro de un proyecto (unicidad por constraint) */
    Optional<DocumentosHabilitantes> findByProyecto_IdProyectoAndTipoDocumento(
            Integer idProyecto, String tipoDocumento);

    /** Verificar si todos los habilitantes obligatorios están APROBADOS */
    @Query("""
        SELECT COUNT(d) = 0
        FROM DocumentosHabilitantes d
        WHERE d.proyecto.idProyecto = :idProyecto
          AND d.tipoDocumento IN (
              'INFORME_DIRECTOR',
              'CERTIFICADO_ANTIPLAGIO',
              'TRABAJO_FINAL_PDF',
              'CERTIFICADO_PENSUM',
              'CERTIFICADO_DEUDAS',
              'CERTIFICADO_IDIOMA',
              'CERTIFICADO_PRACTICAS'
          )
          AND d.estado != 'APROBADO'
    """)
    boolean todosAprobados(@Param("idProyecto") Integer idProyecto);

    /**
     * Documentos en estado ENVIADO donde el docente es Director del proyecto.
     */
    @Query("""
        SELECT d FROM DocumentosHabilitantes d
        WHERE d.estado = 'ENVIADO'
          AND d.proyecto.director.idDocente = :idDocente
    """)
    List<DocumentosHabilitantes> findPendientesPorDirector(@Param("idDocente") Integer idDocente);

    /** Habilitantes ya validados por un docente (historial) */
    List<DocumentosHabilitantes> findByValidadoPor_IdDocenteAndEstado(
            Integer idDocente, String estado);

    /** Habilitantes de un proyecto filtrados por estado */
    List<DocumentosHabilitantes> findByProyecto_IdProyectoAndEstado(
            Integer idProyecto, String estado);
}