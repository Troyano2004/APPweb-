
package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoHabilitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoHabilitanteRepository extends JpaRepository<DocumentoHabilitante, Integer> {

    /** Todos los habilitantes de un proyecto */
    List<DocumentoHabilitante> findByProyecto_IdProyecto(Integer idProyecto);

    /** Todos los habilitantes de un estudiante */
    List<DocumentoHabilitante> findByEstudiante_IdEstudiante(Integer idEstudiante);

    /** Un tipo específico dentro de un proyecto (unicidad por constraint) */
    Optional<DocumentoHabilitante> findByProyecto_IdProyectoAndTipoDocumento(
            Integer idProyecto, String tipoDocumento);

    /** Verificar si todos los habilitantes obligatorios están APROBADOS */
    @Query("""
        SELECT COUNT(d) = 0
        FROM DocumentoHabilitante d
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
     * ✅ FIX PRINCIPAL: Documentos ENVIADO donde el docente es Director del proyecto.
     *
     * ANTES (BUGGY): buscaba por validadoPor.idDocente, pero ese campo es NULL
     * hasta que alguien valide — por eso el docente nunca veía nada.
     *
     * AHORA: busca documentos en estado ENVIADO cuyos proyectos tienen
     * al docente como director (proyecto.director.idDocente = idDocente).
     */
    @Query("""
        SELECT d FROM DocumentoHabilitante d
        WHERE d.estado = 'ENVIADO'
          AND d.proyecto.director.idDocente = :idDocente
    """)
    List<DocumentoHabilitante> findPendientesPorDirector(@Param("idDocente") Integer idDocente);

    /**
     * Habilitantes ya validados por un docente (historial).
     * Este sí usa validadoPor porque ya fueron procesados.
     */
    List<DocumentoHabilitante> findByValidadoPor_IdDocenteAndEstado(
            Integer idDocente, String estado);

    /** Habilitantes de un proyecto filtrados por estado */
    List<DocumentoHabilitante> findByProyecto_IdProyectoAndEstado(
            Integer idProyecto, String estado);
}