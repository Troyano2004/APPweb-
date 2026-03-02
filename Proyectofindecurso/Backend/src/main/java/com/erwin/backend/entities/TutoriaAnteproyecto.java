package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="tutoria_anteproyecto")
public class TutoriaAnteproyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_tutoria")
    private Integer idTutoria;

    @ManyToOne(optional=false)
    @JoinColumn(name="id_anteproyecto", nullable=false)
    private AnteproyectoTitulacion anteproyecto;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    @JoinColumn(name="id_docente", nullable=false)
    private Docente docente;

    @Column(name="fecha", nullable=false)
    private LocalDate fecha;

    @Column(name="hora")
    private LocalTime hora;

    @Column(name="modalidad", length=20, nullable=false)
    private String modalidad; // PRESENCIAL/VIRTUAL

    @Column(name="estado", length=20, nullable=false)
    private String estado; // PROGRAMADA/REALIZADA/CANCELADA

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;
}