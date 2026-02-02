package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "modalidad_titulacion")
public class Modalidadtitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modalidad")
    private Integer idModalidad;

    @Column(name = "nombre", length = 60, nullable = false, unique = true)
    private String nombre; // EXAMEN_COMPLEXIVO / TRABAJO_INTEGRACION_CURRICULAR
}
