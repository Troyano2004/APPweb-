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
@Table(name ="asesoria_director")
public class AsesoriaDirector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asesoria")
    private Integer idAsesoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_director", nullable = false)
    private Docente director;

    @Column(name="fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "observaciones", nullable = false, columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "evidencia_url", length = 300)
    private String evidenciaUrl;
}
