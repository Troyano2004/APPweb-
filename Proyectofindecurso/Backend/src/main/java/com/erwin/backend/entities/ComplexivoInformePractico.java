package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "complexivo_informe_practico")
public class ComplexivoInformePractico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_informe")
    private Integer idInforme;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_complexivo", nullable = false)
    private ComplexivoTitulacion complexivo;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "planteamiento_problema", nullable = false, columnDefinition = "TEXT")
    private String planteamientoProblema;

    @Column(name = "objetivos", nullable = false, columnDefinition = "TEXT")
    private String objetivos;

    @Column(name = "marco_teorico", columnDefinition = "TEXT")
    private String marcoTeorico;

    @Column(name = "metodologia", columnDefinition = "TEXT")
    private String metodologia;

    @Column(name = "resultados", columnDefinition = "TEXT")
    private String resultados;

    @Column(name = "conclusiones", columnDefinition = "TEXT")
    private String conclusiones;

    @Column(name = "bibliografia", columnDefinition = "TEXT")
    private String bibliografia;

    @Column(name = "estado", length = 20)
    private String estado; // BORRADOR/ENTREGADO/APROBADO/RECHAZADO

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

}
