package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "docente_carrera",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_docente", "id_carrera"})
)
public class DocenteCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente_carrera")
    private Integer idDocenteCarrera;

    @ManyToOne
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @ManyToOne
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Column(nullable = false)
    private Boolean activo = true;
}