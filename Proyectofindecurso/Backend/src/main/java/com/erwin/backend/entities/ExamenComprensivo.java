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
@Table(name = "examen_complexivo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_complexivo","tipo_intento"}))
public class ExamenComprensivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_examen")
    private Integer idExamen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_complexivo", nullable = false)
    private ComplexivoTitulacion complexivo;

    @Column(name = "tipo_intento", length = 15, nullable = false)
    private String tipoIntento; // FINAL/SUPLETORIO

    @Column(name = "fecha_rendicion")
    private LocalDate fechaRendicion;

    @Column(name = "nota_teorica", precision = 4, scale = 2)
    private BigDecimal notaTeorica;

    @Column(name = "nota_practica", precision = 4, scale = 2)
    private BigDecimal notaPractica;

    @Column(name = "nota_final", precision = 4, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "aprobado")
    private Boolean aprobado;
}
