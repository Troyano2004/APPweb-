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
@Table(name = "banco_temas")
public class bancotemas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tema")
    private Integer idTema;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @ManyToOne
    @JoinColumn(name = "id_docente_proponente")
    private docente docenteProponente;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "estado", length = 20)
    private String estado; // PROPUESTO/APROBADO/RECHAZADO

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
