package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dt1_asignacion", uniqueConstraints = @UniqueConstraint(columnNames = {"id_docente", "id_carrera", "id_periodo"}
)
)
public class Dt1Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Integer idAsignacion;

    // Docente DT1
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    // Carrera
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    // Periodo
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodo;

    @Column(name = "activo")
    private Boolean activo = true;
}