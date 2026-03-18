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

    // ─── Lista de estudiantes complexivo sin docente ──────────────────
    public record EstudianteComplexivoSinDocenteDto(
            Integer idEstudiante,
            String nombre,
            String carrera,
            String modalidad,
            String estadoComplexivo
    ) {}

    // ─── Info académica coordinador complexivo ────────────────────────
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

    // ─── Informe práctico (Estudiante — Titulación II Complexivo) ─────
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
            // datos del docente asignado
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

    // ─── Estado del complexivo del estudiante ────────────────────────
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


}