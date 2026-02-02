package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name ="propuesta_titulacion")
public class Propuestatitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propuesta")
    private Integer idPropuesta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_eleccion")
    private Elecciontitulacion eleccion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @ManyToOne
    @JoinColumn(name = "id_tema")
    private bancotemas tema;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "tema_investigacion", length = 500, nullable = false)
    private String temaInvestigacion;

    @Column(name = "planteamiento_problema", nullable = false, columnDefinition = "TEXT")
    private String planteamientoProblema;

    @Column(name = "objetivos_generales", nullable = false, columnDefinition = "TEXT")
    private String objetivosGenerales;

    @Column(name = "objetivos_especificos", nullable = false, columnDefinition = "TEXT")
    private String objetivosEspecificos;

    @Column(name = "marco_teorico", nullable = false, columnDefinition = "TEXT")
    private String marcoTeorico;

    @Column(name = "metodologia", nullable = false, columnDefinition = "TEXT")
    private String metodologia;

    @Column(name = "resultados_esperados", nullable = false, columnDefinition = "TEXT")
    private String resultadosEsperados;

    @Column(name = "bibliografia", nullable = false, columnDefinition = "TEXT")
    private String bibliografia;

    @Column(name = "estado", length = 20)
    private String estado; // ENVIADA/EN_REVISION/APROBADA/RECHAZADA

    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio = LocalDate.now();

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Column(name = "observaciones_comision", columnDefinition = "TEXT")
    private String observacionesComision;
}
