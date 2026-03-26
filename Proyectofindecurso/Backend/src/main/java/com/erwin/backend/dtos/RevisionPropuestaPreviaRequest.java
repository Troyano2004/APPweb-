
package com.erwin.backend.dtos;

/**
 * Request para POST /api/revision-ia/propuesta/previa
 *
 * Todos los campos del formulario se envían directamente desde el frontend,
 * sin necesidad de haber guardado nada en la base de datos.
 * El backend usa el idEstudiante para obtener la carrera y modalidad.
 */
public class RevisionPropuestaPreviaRequest {

    /** ID del estudiante — para obtener su carrera y modalidad desde BD */
    private Integer idEstudiante;

    // ─── Campos del formulario de propuesta ───────────────────────────────
    private String titulo;
    private String temaInvestigacion;
    private String planteamientoProblema;
    private String objetivosGenerales;
    private String objetivosEspecificos;
    private String marcoTeorico;
    private String metodologia;
    private String resultadosEsperados;
    private String bibliografia;

    // ─── Configuración del análisis ───────────────────────────────────────
    /**
     * Modo de análisis (default: "integral"):
     *   "integral"    → Evalúa todo: coherencia, pertinencia, viabilidad
     *   "coherencia"  → Solo revisa que título, objetivos y metodología estén alineados
     *   "pertinencia" → Evalúa si el tema es relevante para la carrera
     *   "viabilidad"  → Analiza si el proyecto es realizable
     */
    private String modo;

    /** Instrucción adicional libre del estudiante */
    private String instruccionAdicional;

    // ─── Getters y Setters ────────────────────────────────────────────────

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTemaInvestigacion() { return temaInvestigacion; }
    public void setTemaInvestigacion(String temaInvestigacion) { this.temaInvestigacion = temaInvestigacion; }

    public String getPlanteamientoProblema() { return planteamientoProblema; }
    public void setPlanteamientoProblema(String planteamientoProblema) { this.planteamientoProblema = planteamientoProblema; }

    public String getObjetivosGenerales() { return objetivosGenerales; }
    public void setObjetivosGenerales(String objetivosGenerales) { this.objetivosGenerales = objetivosGenerales; }

    public String getObjetivosEspecificos() { return objetivosEspecificos; }
    public void setObjetivosEspecificos(String objetivosEspecificos) { this.objetivosEspecificos = objetivosEspecificos; }

    public String getMarcoTeorico() { return marcoTeorico; }
    public void setMarcoTeorico(String marcoTeorico) { this.marcoTeorico = marcoTeorico; }

    public String getMetodologia() { return metodologia; }
    public void setMetodologia(String metodologia) { this.metodologia = metodologia; }

    public String getResultadosEsperados() { return resultadosEsperados; }
    public void setResultadosEsperados(String resultadosEsperados) { this.resultadosEsperados = resultadosEsperados; }

    public String getBibliografia() { return bibliografia; }
    public void setBibliografia(String bibliografia) { this.bibliografia = bibliografia; }

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }

    public String getInstruccionAdicional() { return instruccionAdicional; }
    public void setInstruccionAdicional(String instruccionAdicional) { this.instruccionAdicional = instruccionAdicional; }
}