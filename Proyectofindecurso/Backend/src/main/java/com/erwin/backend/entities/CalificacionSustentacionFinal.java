package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "calificacion_sustentacion_final",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_calificacion_tribunal",
                columnNames = { "id_proyecto", "id_docente" }
        )
)
public class CalificacionSustentacionFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calificacion")
    private Integer idCalificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    /**
     * Calidad del trabajo — 40%
     * (metodología, coherencia, desarrollo del proyecto)
     */
    @Column(name = "calidad_trabajo", nullable = false, precision = 4, scale = 2)
    private BigDecimal calidadTrabajo;

    /**
     * Exposición — 30%
     * (claridad, dominio al presentar, organización de la defensa)
     */
    @Column(name = "exposicion", nullable = false, precision = 4, scale = 2)
    private BigDecimal exposicion;

    /**
     * Respuestas a preguntas — 30%
     * (precisión, argumentación, seguridad y dominio del tema)
     */
    @Column(name = "preguntas", nullable = false, precision = 4, scale = 2)
    private BigDecimal preguntas;

    /**
     * Nota final calculada:
     * calidadTrabajo * 0.40 + exposicion * 0.30 + preguntas * 0.30
     */
    @Column(name = "nota_final", nullable = false, precision = 4, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_calificacion", nullable = false, updatable = false)
    private LocalDateTime fechaCalificacion;

    @PrePersist
    public void prePersist() {
        this.fechaCalificacion = LocalDateTime.now();
    }

    // ── Getters y Setters ──────────────────────────────────

    public Integer getIdCalificacion()              { return idCalificacion; }
    public void setIdCalificacion(Integer v)        { this.idCalificacion = v; }

    public ProyectoTitulacion getProyecto()         { return proyecto; }
    public void setProyecto(ProyectoTitulacion v)   { this.proyecto = v; }

    public Docente getDocente()                     { return docente; }
    public void setDocente(Docente v)               { this.docente = v; }

    public BigDecimal getCalidadTrabajo()           { return calidadTrabajo; }
    public void setCalidadTrabajo(BigDecimal v)     { this.calidadTrabajo = v; }

    public BigDecimal getExposicion()               { return exposicion; }
    public void setExposicion(BigDecimal v)         { this.exposicion = v; }

    public BigDecimal getPreguntas()                { return preguntas; }
    public void setPreguntas(BigDecimal v)          { this.preguntas = v; }

    public BigDecimal getNotaFinal()                { return notaFinal; }
    public void setNotaFinal(BigDecimal v)          { this.notaFinal = v; }

    public String getObservaciones()                { return observaciones; }
    public void setObservaciones(String v)          { this.observaciones = v; }

    public LocalDateTime getFechaCalificacion()     { return fechaCalificacion; }
    public void setFechaCalificacion(LocalDateTime v){ this.fechaCalificacion = v; }
}