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
@Table(name = "complexivo_titulacion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_estudiante","id_periodo"}))
public class complexivotitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_complexivo")
    private Integer idComplexivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private Periodotitulacion periodo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_eleccion", nullable = false)
    private Elecciontitulacion eleccion;

    @Column(name = "fecha_inscripcion")
    private LocalDate fechaInscripcion = LocalDate.now();

    @Column(name = "estado", length = 20)
    private String estado; // INSCRITO/EN_CURSO/APROBADO/REPROBADO
}
