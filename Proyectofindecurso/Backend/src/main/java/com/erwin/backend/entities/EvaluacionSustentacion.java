package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "evaluacion_sustentacion")
public class EvaluacionSustentacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Integer idEvaluacion;

    @ManyToOne
    @JoinColumn(name = "id_sustentacion")
    private Sustentacion sustentacion;
    /** Docente miembro del tribunal que registró esta evaluación. */
    @ManyToOne
    @JoinColumn(name = "id_docente")
    private Docente docente;
    /**
     * PREDEFENSA: calificación directa del miembro del tribunal (su promedio = 40%).
     * SUSTENTACION: suma ponderada de los 4 criterios desglosados.
     */
    @Column(name = "tipo", length = 20)
    private String tipo;
    /** Observaciones del miembro (obligatorias si hay rechazo en predefensa). */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
    @Column(name = "calidad_trabajo", precision = 4, scale = 2)
    private BigDecimal calidadTrabajo;
    @Column(name = "originalidad", precision = 4, scale = 2)
    private BigDecimal originalidad;
    @Column(name = "dominio_tema", precision = 4, scale = 2)
    private BigDecimal dominioTema;
    @Column(name = "preguntas", precision = 4, scale = 2)
    private BigDecimal preguntas;
    @Column(name = "nota_final", precision = 4, scale = 2)
    private BigDecimal notaFinal;
}
