
package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class ComplexivoDtos {

    // ─── Asignación DT1 (Coordinador — Titulación I) ──────────────
    public record AsignarDt1ComplexivoRequest(
            Integer idEstudiante,
            Integer idDocente,
            Integer idUsuarioCoordinador,
            String observacion
    ) {}

    // ─── Asignación DT2 (Coordinador — Titulación II) ─────────────
    public record AsignarDt2ComplexivoRequest(
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

    // ─── Info coordinador DT1 ──────────────────────────────────────
    public record InfoCoordinadorDt1Dto(
            Integer idCarrera,
            String carrera,
            Integer idPeriodo,
            String periodo,
            List<DocenteOpcionDto> docentesDisponibles,
            List<EstudianteComplexivoSinDocenteDto> estudiantesSinDocente,
            List<ComplexivoDocenteAsignacionResponse> asignacionesActuales
    ) {}

    // ─── Info coordinador DT2 ──────────────────────────────────────
    public record InfoCoordinadorDt2Dto(
            Integer idCarrera,
            String carrera,
            Integer idPeriodo,
            String periodo,
            List<DocenteOpcionDto> docentesDisponibles,
            List<EstudianteComplexivoSinDocenteDto> estudiantesSinDocente,
            List<ComplexivoDocenteAsignacionResponse> asignacionesActuales
    ) {}

    // Mantener por compatibilidad con código existente
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

    // ─── Informe práctico ──────────────────────────────────────────
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

    // ─── Estado del complexivo del estudiante ──────────────────────
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

    // ─── DT2 — estudiantes e informes ──────────────────────────────
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

    // ─── Propuestas (DT1 las aprueba) ─────────────────────────────
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
            String estado,
            String observaciones
    ) {}

    // ─── Request genérico (compatibilidad) ────────────────────────
    public record AsignarDocenteComplexivoRequest(
            Integer idEstudiante,
            Integer idDocente,
            Integer idUsuarioCoordinador,
            String observacion
    ) {}
}