package com.erwin.backend.dtos;

import java.time.LocalDateTime;

/**
 * Respuesta del endpoint POST /api/revision-ia/propuesta/{idPropuesta}
 *
 * Devuelve el contexto de la propuesta + el análisis IA.
 */
public class RevisionPropuestaIAResponse {

    // ─── Contexto de la propuesta ─────────────────────────────────────────
    private Integer idPropuesta;
    private String tituloPropuesta;
    private String nombreEstudiante;

    /** Carrera en la que está inscrito el estudiante (ej: "Ingeniería en Sistemas") */
    private String nombreCarrera;

    /** Facultad a la que pertenece la carrera (ej: "Facultad de Ciencias de la Ingeniería") */
    private String nombreFacultad;

    /** Modalidad de titulación elegida (ej: "TRABAJO_INTEGRACION_CURRICULAR") */
    private String modalidadTitulacion;

    /** Estado actual de la propuesta (ENVIADA / EN_REVISION / APROBADA / RECHAZADA) */
    private String estadoPropuesta;

    // ─── Resultado IA ─────────────────────────────────────────────────────
    /**
     * JSON string con la estructura:
     * {
     *   "estado_evaluacion": "APROBABLE|REQUIERE_AJUSTES|RECHAZABLE",
     *   "puntaje_estimado": 0-100,
     *   "pertinencia_carrera": "ALTA|MEDIA|BAJA",
     *   "analisis_titulo": "texto con análisis del título",
     *   "analisis_objetivos": "texto con análisis de objetivos",
     *   "analisis_metodologia": "texto con análisis de metodología",
     *   "fortalezas": ["fortaleza 1", "fortaleza 2"],
     *   "debilidades": ["debilidad 1", "debilidad 2"],
     *   "sugerencias_mejora": ["sugerencia 1", "sugerencia 2"],
     *   "mensaje_estudiante": "retroalimentación directa y motivadora"
     * }
     */
    private String feedbackIa;

    private LocalDateTime fechaAnalisisIa;

    // ─── Constructores ────────────────────────────────────────────────────

    public RevisionPropuestaIAResponse() {}

    public RevisionPropuestaIAResponse(Integer idPropuesta,
                                       String tituloPropuesta,
                                       String nombreEstudiante,
                                       String nombreCarrera,
                                       String nombreFacultad,
                                       String modalidadTitulacion,
                                       String estadoPropuesta,
                                       String feedbackIa,
                                       LocalDateTime fechaAnalisisIa) {
        this.idPropuesta        = idPropuesta;
        this.tituloPropuesta    = tituloPropuesta;
        this.nombreEstudiante   = nombreEstudiante;
        this.nombreCarrera      = nombreCarrera;
        this.nombreFacultad     = nombreFacultad;
        this.modalidadTitulacion = modalidadTitulacion;
        this.estadoPropuesta    = estadoPropuesta;
        this.feedbackIa         = feedbackIa;
        this.fechaAnalisisIa    = fechaAnalisisIa;
    }

    // ─── Getters y Setters ────────────────────────────────────────────────

    public Integer getIdPropuesta() { return idPropuesta; }
    public void setIdPropuesta(Integer idPropuesta) { this.idPropuesta = idPropuesta; }

    public String getTituloPropuesta() { return tituloPropuesta; }
    public void setTituloPropuesta(String tituloPropuesta) { this.tituloPropuesta = tituloPropuesta; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public String getNombreCarrera() { return nombreCarrera; }
    public void setNombreCarrera(String nombreCarrera) { this.nombreCarrera = nombreCarrera; }

    public String getNombreFacultad() { return nombreFacultad; }
    public void setNombreFacultad(String nombreFacultad) { this.nombreFacultad = nombreFacultad; }

    public String getModalidadTitulacion() { return modalidadTitulacion; }
    public void setModalidadTitulacion(String modalidadTitulacion) {
        this.modalidadTitulacion = modalidadTitulacion;
    }

    public String getEstadoPropuesta() { return estadoPropuesta; }
    public void setEstadoPropuesta(String estadoPropuesta) { this.estadoPropuesta = estadoPropuesta; }

    public String getFeedbackIa() { return feedbackIa; }
    public void setFeedbackIa(String feedbackIa) { this.feedbackIa = feedbackIa; }

    public LocalDateTime getFechaAnalisisIa() { return fechaAnalisisIa; }
    public void setFechaAnalisisIa(LocalDateTime fechaAnalisisIa) {
        this.fechaAnalisisIa = fechaAnalisisIa;
    }
}