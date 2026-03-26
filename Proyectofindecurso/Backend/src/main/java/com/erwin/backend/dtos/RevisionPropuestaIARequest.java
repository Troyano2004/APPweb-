package com.erwin.backend.dtos;

/**
 * Request para POST /api/revision-ia/propuesta/{idPropuesta}
 *
 * Todos los campos son opcionales — si no se envían,
 * el service los toma directamente desde la base de datos.
 *
 * ¿Por qué incluirlos opcionalmente?
 * Permite que el frontend los envíe antes de guardar la propuesta
 * (revisión en tiempo real mientras el estudiante escribe).
 */
public class RevisionPropuestaIARequest {

    /**
     * Modo de análisis:
     *   "integral"         → Evalúa todo: coherencia, pertinencia, viabilidad (default)
     *   "coherencia"       → Solo revisa que título, objetivos y metodología estén alineados
     *   "pertinencia"      → Evalúa si el tema es relevante para la carrera del estudiante
     *   "viabilidad"       → Analiza si el proyecto es realizable con los recursos de la carrera
     */
    private String modo;

    /**
     * Instrucción adicional del docente o del propio estudiante.
     * Ejemplo: "Soy estudiante de último año, el proyecto debe ser implementable en 6 meses"
     */
    private String instruccionAdicional;

    // Getters y Setters

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }

    public String getInstruccionAdicional() { return instruccionAdicional; }
    public void setInstruccionAdicional(String instruccionAdicional) {
        this.instruccionAdicional = instruccionAdicional;
    }
}