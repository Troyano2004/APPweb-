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
@Table(name = "tipo_trabajo_titulacion")
public class Tipotrabajotitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_trabajo")
    private Integer idTipoTrabajo;

    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "id_modalidad", nullable = false)
    private Modalidadtitulacion modalidadTitulacion;
}
