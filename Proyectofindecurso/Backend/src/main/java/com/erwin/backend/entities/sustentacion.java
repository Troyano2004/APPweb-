package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sustentacion")
public class sustentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sustentacion")
    private Integer idSustentacion;

    @ManyToOne
    @JoinColumn(name = "id_proyecto")
    private Proyectotitulacion proyecto;

    @Column(name = "tipo", length = 20)
    private String tipo; // PREDEFENSA/DEFENSA_FINAL

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "lugar", length = 150, nullable = false)
    private String lugar;

    @Column(name = "observaciones", length = 300)
    private String observaciones;
}
