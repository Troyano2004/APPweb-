
package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class ComplexivoDtos {

    // ─── Asignación de docente (Coordinador) ───────────────────────────
    public record AsignarDocenteComplexivoRequest(
            Integer idEstudiante,
            Integer idDocente,
            Integer idUsuarioCoordinador,
            String observacion
    ) {}

    public record ComplexivoDocenteAsignacionResponse(
            Integer idAsignacion,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idDocente,
            String nombreDocente,
            Integer idPeriodo,
            String periodo,
            LocalDateTime fechaAsignacion,
            Boolean activo
    ) {}

    public record EstudianteComplexivoSinDocenteDto(
            Integer idEstudiante,
            String nombre,
            String carrera,
            String modalidad,
            String estadoComplexivo
    ) {}

    public record InfoCoordinadorComplexivoDto(
            Integer idCarrera,
            String carrera,
            Integer idPeriodo,
            String periodo,
            List<DocenteOpcionDto> docentesDisponibles,
            List<EstudianteComplexivoSinDocenteDto> estudiantesSinDocente,
            List<ComplexivoDocenteAsignacionResponse> asignacionesActuales
    ) {}

    public record DocenteOpcionDto(Integer idDocente, String nombre) {}

    public record ComplexivoInformeDto(
            Integer idInforme,
            Integer idComplexivo,
            String titulo,
            String planteamientoProblema,
            String objetivos,
            String marcoTeorico,
            String metodologia,
            String resultados,
            String conclusiones,
            String bibliografia,
            String estado,
            String observaciones,
            Integer idDocente,
            String nombreDocente
    ) {}

    public record ComplexivoInformeUpdateRequest(
            String titulo,
            String planteamientoProblema,
            String objetivos,
            String marcoTeorico,
            String metodologia,
            String resultados,
            String conclusiones,
            String bibliografia
    ) {}

    public record EstadoComplexivoEstudianteDto(
            boolean tieneComplexivo,
            Integer idComplexivo,
            String estadoComplexivo,
            boolean tieneInforme,
            Integer idInforme,
            String estadoInforme,
            boolean tieneDocente,
            String nombreDocente
    ) {}

    // ─── NUEVOS — Docente complexivo ──────────────────────────────
    public record EstudianteDeDocenteDto(
            Integer idComplexivo,
            Integer idEstudiante,
            String  nombreEstudiante,
            String  carrera,
            String  estadoComplexivo,
            boolean tieneInforme,
            String  estadoInforme
    ) {}

    public record ComplexivoAsesoriaDto(
            Integer idAsesoria,
            Integer idComplexivo,
            String  fecha,
            String  observaciones,
            String  nombreDocente
    ) {}

    public record RegistrarAsesoriaRequest(
            String observaciones
    ) {}
    // ─── Propuestas para docente complexivo ──────────────────────
    public record PropuestaComplexivoDto(
            Integer idPropuesta,
            Integer idEstudiante,
            String  nombreEstudiante,
            String  titulo,
            String  planteamientoProblema,
            String  objetivosGenerales,
            String  objetivosEspecificos,
            String  metodologia,
            String  resultadosEsperados,
            String  bibliografia,
            String  estado,
            String  observacionesComision,
            String  fechaEnvio
    ) {}

    public record DecisionPropuestaComplexivoRequest(
            String estado,        // APROBADA o RECHAZADA
            String observaciones
    ) {}
}