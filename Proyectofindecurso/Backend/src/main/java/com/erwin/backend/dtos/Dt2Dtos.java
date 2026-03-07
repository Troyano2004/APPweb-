package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTOs para los 5 módulos del sistema de Titulación II.
 */
public class Dt2Dtos {

    // =========================================================
    // MÓDULO 1 — Configuración inicial de Titulación II
    // =========================================================

    /** Lista de proyectos con anteproyecto APROBADO pendientes de configurar DT2. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoPendienteConfiguracionDto {
        private Integer idProyecto;
        private String titulo;
        private String estudiante;
        private String carrera;
        private String periodo;
        private String estadoProyecto;
        private Boolean tieneDocenteDt2;
        private Boolean tieneDirector;
        private Boolean tieneTribunal;
        private Boolean configuracionCompleta;
    }

    /** Request para asignar el docente DT2. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarDocenteDt2Request {
        private Integer idProyecto;
        private Integer idDocenteDt2;
        private Integer idRealizadoPor;
        private String periodo;
        private String observacion;
    }

    /** Request para asignar/cambiar el Director del TIC. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarDirectorRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private Integer idRealizadoPor;
        private String motivo;
        private String periodo;
    }

    /** Request para registrar miembro del tribunal. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MiembroTribunalRequest {
        private Integer idDocente;
        private String cargo; // PRESIDENTE | VOCAL | SUPLENTE
    }

    /** Request para asignar todo el tribunal de una vez. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarTribunalDt2Request {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private String periodo;
        private List<MiembroTribunalRequest> miembros;
    }

    /** Respuesta de configuración del proyecto. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ConfiguracionProyectoDto {
        private Integer idProyecto;
        private String titulo;
        private String estadoProyecto;
        private String docenteDt2;
        private Integer idDocenteDt2;
        private String director;
        private Integer idDirector;
        private List<MiembroTribunalDto> tribunal;
        private Boolean configuracionCompleta;
        private List<BitacoraDto> bitacora;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MiembroTribunalDto {
        private Integer idTribunal;
        private Integer idDocente;
        private String nombre;
        private String cargo;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BitacoraDto {
        private String tipo;
        private String nombreAsignado;
        private String cargo;
        private String realizadoPor;
        private LocalDateTime fecha;
        private String observacion;
    }

    // =========================================================
    // MÓDULO 2 — Seguimiento de avances
    // =========================================================

    /** Request para registrar una asesoría. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarAsesoriaRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private LocalDateTime fecha;
        private String observaciones;
        private String evidenciaUrl;
        private BigDecimal porcentajeAvance;
        private Integer numeroCorte; // 1 o 2
        private BigDecimal calificacion; // opcional
    }

    /** DTO de una asesoría registrada. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsesoriaDto {
        private Integer idAsesoria;
        private LocalDateTime fecha;
        private String observaciones;
        private String evidenciaUrl;
        private BigDecimal porcentajeAvance;
        private Integer numeroCorte;
        private BigDecimal calificacion;
    }

    /** Request para cerrar un corte y generar el acta. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CerrarCorteRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private Integer numeroCorte;
        private BigDecimal notaCorte;
        private String observaciones;
    }

    /** DTO del acta de corte. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ActaCorteDto {
        private Integer idActaCorte;
        private Integer numeroCorte;
        private LocalDateTime fechaGeneracion;
        private Integer totalAsesorias;
        private Boolean asesoriasSuficientes;
        private BigDecimal notaCorte;
        private String observaciones;
        private String urlActaPdf;
        private String advertencia; // si no se alcanzaron las 5 asesorías mínimas
    }

    /** Resumen de seguimiento para una vista. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SeguimientoDto {
        private Integer idProyecto;
        private String titulo;
        private String estudiante;
        private String estadoProyecto;
        private Integer totalAsesorias;
        private Integer asesoriaCorte1;
        private Integer asesoriaCorte2;
        private BigDecimal ultimoAvance;
        private List<ActaCorteDto> actas;
    }

    // =========================================================
    // MÓDULO 3 — Certificación antiplagio
    // =========================================================

    /** DTO de un intento de antiplagio. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AntiplacioIntentoDto {
        private Integer idIntento;
        private LocalDateTime fechaIntento;
        private BigDecimal porcentajeCoincidencia;
        private String urlInforme;
        private Boolean favorable;
        private String observaciones;
        private String resultado; // "APROBADO (<10%)" o "RECHAZADO (>=10%)"
    }

    /** Respuesta del estado del certificado antiplagio. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CertificadoAntiplacioDto {
        private Integer idProyecto;
        private Boolean certificadoFavorable;
        private BigDecimal ultimoPorcentaje;
        private String urlUltimoInforme;
        private LocalDateTime fechaUltimoIntento;
        private Integer totalIntentos;
        private List<AntiplacioIntentoDto> historial;
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    /** Request para programar la predefensa. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProgramarPredefensaRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
    }

    /** Request para que el docente DT2 registre su calificación (60%). */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarPredefensaDocenteRequest {
        private Integer idProyecto;
        private Integer idDocenteDt2;
        private BigDecimal nota; // sobre 10
        private String observaciones;
    }

    /** Request para que un miembro del tribunal califique la predefensa (su parte = 40% promedio). */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarPredefensaTribunalRequest {
        private Integer idProyecto;
        private Integer idDocente; // miembro del tribunal
        private BigDecimal nota; // sobre 10
        private String observaciones;
        private Boolean solicitudCorrecciones; // true si el tribunal pide correcciones
    }

    /** Respuesta del estado de la predefensa. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PredefensaDto {
        private Integer idSustentacion;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String estado; // PENDIENTE | EN_PROCESO | APROBADA | CON_OBSERVACIONES
        private BigDecimal notaDocenteDt2;        // 60%
        private BigDecimal promedioTribunal;      // 40%
        private BigDecimal notaFinalPonderada;    // 60% * notaDT2 + 40% * promedio tribunal
        private List<EvaluacionMiembroDto> evaluacionesTribunal;
        private Boolean solicitudCorrecciones;
        private String observacionesTribunal;
        private Integer miembrosQueCalificaron;
        private Integer totalMiembrosTribunal;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EvaluacionMiembroDto {
        private Integer idDocente;
        private String nombreDocente;
        private String cargo;
        private BigDecimal nota;
        private String observaciones;
        private Boolean solicitudCorrecciones;
    }

    // =========================================================
    // MÓDULO 5 — Sustentación final
    // =========================================================

    /** Request para registrar/actualizar el checklist de documentos previos. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DocumentosPreviosRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private Boolean ejemplarImpreso;
        private Boolean copiaDigitalBiblioteca;
        private Boolean copiasDigitalesTribunal;
        private Boolean informeCompilatioFirmado;
        private String observaciones;
    }

    /** DTO del checklist de documentos previos. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DocumentosPreviosDto {
        private Boolean ejemplarImpreso;
        private Boolean copiaDigitalBiblioteca;
        private Boolean copiasDigitalesTribunal;
        private Boolean informeCompilatioFirmado;
        private Boolean completo;
        private String observaciones;
    }

    /** Request para programar la sustentación final. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProgramarSustentacionRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
    }

    /** Request para que un miembro del tribunal califique la sustentación con los 4 criterios. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarSustentacionRequest {
        private Integer idProyecto;
        private Integer idDocente; // miembro del tribunal
        /**
         * Calidad del trabajo y metodología: 20% → máximo 2 puntos.
         * Se registra sobre 10 y el sistema aplica el peso.
         */
        private BigDecimal calidadTrabajo;
        /**
         * Originalidad e independencia: 20% → máximo 2 puntos.
         */
        private BigDecimal originalidad;
        /**
         * Dominio del tema en la exposición: 30% → máximo 3 puntos.
         */
        private BigDecimal dominioTema;
        /**
         * Pertinencia de respuestas: 30% → máximo 3 puntos.
         */
        private BigDecimal preguntas;
        private String observaciones;
    }

    /** Respuesta del resultado de la sustentación final. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ResultadoSustentacionDto {
        private Integer idProyecto;
        private String titulo;
        private String estudiante;
        private LocalDate fechaSustentacion;
        private String resultado; // APROBADO | REPROBADO | SEGUNDA_OPORTUNIDAD
        private BigDecimal promedioTribunal;     // promedio de los miembros
        private BigDecimal notaGradoFinal;       // 80% promedioRecord + 20% notaSustentacion
        private BigDecimal promedioRecordAcademico;
        private List<EvaluacionMiembroDto> evaluaciones;
        private Boolean habilitadoSegundaOportunidad;
        private LocalDate fechaLimiteSegundaOportunidad;
    }

    /** Request para habilitar la segunda oportunidad. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SegundaOportunidadRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private LocalDate fechaSustentacion;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
    }

    // =========================================================
    // RESPUESTA GENÉRICA
    // =========================================================

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MensajeDto {
        private String mensaje;
        private String estado;
        private Boolean exito;
    }
}
