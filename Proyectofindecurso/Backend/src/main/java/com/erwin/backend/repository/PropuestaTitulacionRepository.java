package com.erwin.backend.repository;

import com.erwin.backend.entities.PropuestaTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropuestaTitulacionRepository extends JpaRepository<PropuestaTitulacion, Integer> {
    List<PropuestaTitulacion> findByEstudiante_IdEstudiante(Integer IdEstudiante);
    List<PropuestaTitulacion> findByEstado(String estado);
    long countByEstado(String estado);
    List<PropuestaTitulacion> findTop5ByEstadoOrderByFechaRevisionDesc(String estado);

    Optional<PropuestaTitulacion> findByEleccion_IdEleccion(Integer IdEleccion);

    @Query(value = "SELECT * FROM sp_obtener_propuestas_estudiante(:idEstudiante)", nativeQuery = true)
    List<PropuestaTitulacion> findByEstudianteStored(@Param("idEstudiante") Integer idEstudiante);

    @Transactional
    @Query(value = "SELECT sp_crear_propuesta_titulacion(:idEleccion, :idEstudiante, :idCarrera, :idTema, :titulo, :temaInvestigacion, :planteamientoProblema, :objetivosGenerales, :objetivosEspecificos, :marcoTeorico, :metodologia, :resultadosEsperados, :bibliografia, :estado, :fechaEnvio)", nativeQuery = true)
    Integer crearPropuestaStored(@Param("idEleccion") Integer idEleccion,
                                 @Param("idEstudiante") Integer idEstudiante,
                                 @Param("idCarrera") Integer idCarrera,
                                 @Param("idTema") Integer idTema,
                                 @Param("titulo") String titulo,
                                 @Param("temaInvestigacion") String temaInvestigacion,
                                 @Param("planteamientoProblema") String planteamientoProblema,
                                 @Param("objetivosGenerales") String objetivosGenerales,
                                 @Param("objetivosEspecificos") String objetivosEspecificos,
                                 @Param("marcoTeorico") String marcoTeorico,
                                 @Param("metodologia") String metodologia,
                                 @Param("resultadosEsperados") String resultadosEsperados,
                                 @Param("bibliografia") String bibliografia,
                                 @Param("estado") String estado,
                                 @Param("fechaEnvio") LocalDate fechaEnvio);

    @Transactional
    @Query(value = "SELECT sp_registrar_decision_propuesta(:idPropuesta, :estado, :observaciones, :fechaRevision)", nativeQuery = true)
    Integer registrarDecisionStored(@Param("idPropuesta") Integer idPropuesta,
                                    @Param("estado") String estado,
                                    @Param("observaciones") String observaciones,
                                    @Param("fechaRevision") LocalDate fechaRevision);
}