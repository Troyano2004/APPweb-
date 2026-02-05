package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "acta_grado")
public class ActaGrado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acta")
    private Integer idActa;

    @OneToOne
    @JoinColumn(name = "id_estudiante", unique = true)
    private Estudiante estudiante;

    @Column(name = "tipo_titulacion", length = 40)
    private String tipoTitulacion; // TRABAJO_INTEGRACION_CURRICULAR / EXAMEN_COMPLEXIVO

    @Column(name = "id_origen", nullable = false)
    private Integer idOrigen; // depende del tipo: id_proyecto o id_complexivo (no hay FK directa)

    @Column(name = "nota_record", precision = 5, scale = 2)
    private BigDecimal notaRecord;

    @Column(name = "nota_titulacion", precision = 5, scale = 2)
    private BigDecimal notaTitulacion;

    @Column(name = "nota_final", precision = 5, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision = LocalDate.now();
}
