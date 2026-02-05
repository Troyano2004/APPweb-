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
@Table(name = "eleccion_titulacion",
        uniqueConstraints = @UniqueConstraint(name = "uq_eleccion_estudiante_periodo", columnNames = {"id_estudiante","id_periodo"}))
public class EleccionTitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_eleccion")
    private Integer idEleccion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_modalidad")
    private Modalidadtitulacion modalidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo")
    private PeriodoTitulacion periodo;

    @Column(name = "fecha_eleccion")
    private LocalDate fechaEleccion = LocalDate.now();

    @Column(name = "estado", length = 20)
    private String estado; // ACTIVA/ANULADA/FINALIZADA

    @Column(name = "numero_cambios")
    private Integer numeroCambios = 0;

    @Column(name = "motivo_ultimo_cambio", columnDefinition = "TEXT")
    private String motivoUltimoCambio;

    @Column(name = "fecha_ultimo_cambio")
    private LocalDate fechaUltimoCambio;
}
