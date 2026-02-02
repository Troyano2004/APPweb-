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
@Table(name = "comision_formativa")
public class comisionformativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comision")
    private Integer idComision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "periodo_academico", length = 20, nullable = false)
    private String periodoAcademico;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "estado", length = 15, nullable = false)
    private String estado; // ACTIVA/INACTIVA

}
