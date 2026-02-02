package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "complexivo_asesoria")
public class complexivotutoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asesoria")
    private Integer idAsesoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_informe", nullable = false)
    private complexivoinformepractico informe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private docente docente;

    @Column(name = "fecha")
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "observaciones", nullable = false, columnDefinition = "TEXT")
    private String observaciones;
}
