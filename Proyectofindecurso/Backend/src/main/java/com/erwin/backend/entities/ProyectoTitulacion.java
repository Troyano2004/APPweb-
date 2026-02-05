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
@Table(name = "proyecto_titulacion")
public class ProyectoTitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_propuesta", unique = true)
    private PropuestaTitulacion propuesta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo")
    private PeriodoTitulacion periodo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_director")
    private Docente director;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_trabajo")
    private Tipotrabajotitulacion tipoTrabajo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_eleccion")
    private EleccionTitulacion eleccion;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "estado", length = 20)
    private String estado; // ANTEPROYECTO/DESARROLLO/PREDEFENSA/DEFENSA/FINALIZADO

    @Column(name = "porcentaje_antiplagio", precision = 5, scale = 2)
    private BigDecimal porcentajeAntiplagio;

    @Column(name = "fecha_verificacion_antiplagio")
    private LocalDate fechaVerificacionAntiplagio;

    @Column(name = "url_informe_antiplagio", length = 300)
    private String urlInformeAntiplagio;
}
