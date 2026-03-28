package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class Dt2Dtos {

    // =========================================================
    // MÓDULO 1 — Configuración inicial de Titulación II
    // =========================================================

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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarDocenteDt2Request {
        private Integer idProyecto;
        private Integer idDocenteDt2;
        private Integer idRealizadoPor;
        private String periodo;
        private String observacion;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarDirectorRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private Integer idRealizadoPor;
        private String motivo;
        private String periodo;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MiembroTribunalRequest {
        private Integer idDocente;
        private String cargo;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AsignarTribunalDt2Request {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private String periodo;
        private List<MiembroTribunalRequest> miembros;
    }

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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarAsesoriaRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private LocalDateTime fecha;
        private String observaciones;
        private String evidenciaUrl;
        private BigDecimal porcentajeAvance;
        private Integer numeroCorte;
        private BigDecimal calificacion;
    }

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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CerrarCorteRequest {
        private Integer idProyecto;
        private Integer idDirector;
        private Integer numeroCorte;
        private BigDecimal notaCorte;
        private String observaciones;
    }

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
        private String advertencia;
    }

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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AntiplacioIntentoDto {
        private Integer idIntento;
        private LocalDateTime fechaIntento;
        private BigDecimal porcentajeCoincidencia;
        private String urlInforme;
        private Boolean favorable;
        private String observaciones;
        private String resultado;
    }

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
    // MÓDULO 3B — Revisión IA del contenido del documento
    // =========================================================

    /**
     * Todos los campos de texto del DocumentoTitulacion para
     * enviarlos al frontend y analizarlos con WasItAIGenerated.
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DocumentoTitulacionTextoDto {
        private Integer idDocumento;
        private Integer idProyecto;
        private String titulo;
        private String nombreEstudiante;
        private String carreraEstudiante;
        private String estadoDocumento;
        private String estadoRevisionIa;
        private String feedbackIa;
        private String resumen;
        private String abstractText;
        private String introduccion;
        private String planteamientoProblema;
        private String justificacion;
        private String objetivoGeneral;
        private String objetivosEspecificos;
        private String marcoTeorico;
        private String metodologia;
        private String resultados;
        private String discusion;
        private String conclusiones;
        private String recomendaciones;
        private String bibliografia;
        private String anexos;
    }

    /**
     * El frontend manda este request tras analizar con WasItAIGenerated.
     * El backend lo persiste en feedbackIa + estadoRevisionIa.
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GuardarRevisionIaRequest {
        private Integer idProyecto;
        private Integer porcentajePromedioIa;
        private String nivelRiesgo;
        private String feedbackIa;
        private String estadoRevisionIa;
    }

    /**
     * Registra antiplagio automático desde el análisis IA (sin PDF).
     * El porcentaje viene de WasItAIGenerated calculado en el frontend.
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarAntiplacioIaRequest {
        private Integer idProyecto;
        private Integer idDocenteDt2;
        private BigDecimal porcentajeCoincidencia;
        private String nivelRiesgo;
        private String observaciones;
    }

    /**
     * Request al proxy WasItAIGenerated — el backend hace la llamada HTTP
     * para evitar CORS en el navegador.
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WasItAiRequest {
        private String content;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WasItAiAnalysis {
        private String likelihood;
        private String reasoning;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WasItAiSentence {
        private String text;
        private Boolean isAI;
        private Double confidence;
        private Map<String, Double> scores;
    }

    /**
     * Respuesta del proxy WasItAIGenerated (espejo del JSON externo).
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WasItAiResponse {
        private Boolean isAI;
        private Double confidence;
        private List<String> patterns;
        private WasItAiAnalysis analysis;
        private List<WasItAiSentence> sentences;
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProgramarPredefensaRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarPredefensaDocenteRequest {
        private Integer idProyecto;
        private Integer idDocenteDt2;
        private BigDecimal nota;
        private String observaciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarPredefensaTribunalRequest {
        private Integer idProyecto;
        private Integer idDocente;
        private BigDecimal nota;
        private String observaciones;
        private Boolean solicitudCorrecciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PredefensaDto {
        private Integer idSustentacion;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String estado;
        private BigDecimal notaDocenteDt2;
        private BigDecimal promedioTribunal;
        private BigDecimal notaFinalPonderada;
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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DocumentosPreviosDto {
        private Boolean ejemplarImpreso;
        private Boolean copiaDigitalBiblioteca;
        private Boolean copiasDigitalesTribunal;
        private Boolean informeCompilatioFirmado;
        private Boolean completo;
        private String observaciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProgramarSustentacionRequest {
        private Integer idProyecto;
        private Integer idRealizadoPor;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CalificarSustentacionRequest {
        private Integer idProyecto;
        private Integer idDocente;
        private BigDecimal calidadTrabajo;
        private BigDecimal originalidad;
        private BigDecimal dominioTema;
        private BigDecimal preguntas;
        private String observaciones;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ResultadoSustentacionDto {
        private Integer idProyecto;
        private String titulo;
        private String estudiante;
        private LocalDate fechaSustentacion;
        private String resultado;
        private BigDecimal promedioTribunal;
        private BigDecimal notaGradoFinal;
        private BigDecimal promedioRecordAcademico;
        private List<EvaluacionMiembroDto> evaluaciones;
        private Boolean habilitadoSegundaOportunidad;
        private LocalDate fechaLimiteSegundaOportunidad;
    }

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