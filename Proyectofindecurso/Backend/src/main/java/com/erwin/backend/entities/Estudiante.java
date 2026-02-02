package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estudiante")

public class Estudiante {
    @Id
    @Column(name = "id_estudiante")
    private Integer idEstudiante;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "id_estudiante")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "promedio_record_80", precision = 5, scale = 2)
    private BigDecimal promedioRecord80;

    @Column(name = "discapacidad")
    private Boolean discapacidad = false;
}
