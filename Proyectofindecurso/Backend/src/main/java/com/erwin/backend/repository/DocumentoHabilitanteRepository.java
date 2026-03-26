
package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoHabilitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoHabilitanteRepository
        extends JpaRepository<DocumentoHabilitante, Integer> {

    // ── TIC (por proyecto) ─────────────────────────────────────
    List<DocumentoHabilitante> findByProyecto_IdProyecto(Integer idProyecto);

    Optional<DocumentoHabilitante> findByProyecto_IdProyectoAndTipoDocumento(
            Integer idProyecto, String tipoDocumento);

    List<DocumentoHabilitante> findByProyecto_IdProyectoAndEstado(
            Integer idProyecto, String estado);

    // ── COMPLEXIVO (por complexivo_titulacion) ─────────────────
    List<DocumentoHabilitante> findByComplexivo_IdComplexivo(Integer idComplexivo);

    Optional<DocumentoHabilitante> findByComplexivo_IdComplexivoAndTipoDocumento(
            Integer idComplexivo, String tipoDocumento);

    // ── Por estudiante ─────────────────────────────────────────
    List<DocumentoHabilitante> findByEstudiante_IdEstudiante(Integer idEstudiante);

    List<DocumentoHabilitante> findByValidadoPor_IdDocenteAndEstado(
            Integer idDocente, String estado);

    // ── Verificar aprobados TIC ────────────────────────────────
    @Query("""
        SELECT COUNT(d) = 0
        FROM DocumentoHabilitante d
        WHERE d.proyecto.idProyecto = :idProyecto
          AND d.tipoDocumento IN (
              'INFORME_DIRECTOR','CERTIFICADO_ANTIPLAGIO','TRABAJO_FINAL_PDF',
              'CERTIFICADO_PENSUM','CERTIFICADO_DEUDAS','CERTIFICADO_IDIOMA',
              'CERTIFICADO_PRACTICAS'
          )
          AND d.estado != 'APROBADO'
    """)
    boolean todosAprobados(@Param("idProyecto") Integer idProyecto);

    // ── Pendientes director TIC ────────────────────────────────
    @Query("""
        SELECT d FROM DocumentoHabilitante d
        WHERE d.estado = 'ENVIADO'
          AND d.proyecto IS NOT NULL
          AND d.proyecto.director.idDocente = :idDocente
    """)
    List<DocumentoHabilitante> findPendientesPorDirector(
            @Param("idDocente") Integer idDocente);

    // ── Pendientes DT2 Complexivo filtrado por docente ─────────
    @Query("""
        SELECT d FROM DocumentoHabilitante d
        JOIN ComplexivoDt2Asignacion a
            ON a.estudiante.idEstudiante = d.estudiante.idEstudiante
            AND a.activo = true
        WHERE d.estado = 'ENVIADO'
          AND d.complexivo IS NOT NULL
          AND a.docente.idDocente = :idDocente
    """)
    List<DocumentoHabilitante> findPendientesComplexivoPorDocente(
            @Param("idDocente") Integer idDocente);
}