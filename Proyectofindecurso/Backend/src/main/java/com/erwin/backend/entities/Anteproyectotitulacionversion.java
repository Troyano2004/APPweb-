package com.erwin.backend.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name ="anteproyecto_version",uniqueConstraints = @UniqueConstraint(columnNames = {"id_anteproyecto","numero_version"}))
public class Anteproyectotitulacionversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_version")
    private Integer idVersion;
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_anteproyecto", nullable = false)
    private AnteproyectoTitulacion anteproyecto;

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "estado_version", nullable = false, length = 20)
    private String estadoVersion;

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



    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio = LocalDate.now();

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Column(name = "comentario_cambio", length = 300)
    private String comentarioCambio;
}
