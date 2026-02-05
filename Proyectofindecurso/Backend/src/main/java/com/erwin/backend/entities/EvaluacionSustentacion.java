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

    @Column(name = "calidad_trabajo", precision = 3, scale = 2)
    private BigDecimal calidadTrabajo;

    @Column(name = "originalidad", precision = 3, scale = 2)
    private BigDecimal originalidad;

    @Column(name = "dominio_tema", precision = 3, scale = 2)
    private BigDecimal dominioTema;

    @Column(name = "preguntas", precision = 3, scale = 2)
    private BigDecimal preguntas;

    @Column(name = "nota_final", precision = 4, scale = 2)
    private BigDecimal notaFinal;
}
