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
@Table(name = "universidad")
public class Universidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_universidad")
    private Integer idUniversidad;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "mision", nullable = false, columnDefinition = "TEXT")
    private String mision;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "lema", length = 200)
    private String lema;

    @Column(name = "campus", length = 200)
    private String campus;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "contacto_info", length = 100)
    private String contactoInfo;
}
