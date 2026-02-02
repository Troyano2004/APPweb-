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
@Table(name = "carrera_modalidad")
public class Carreramodalidad {
    @EmbeddedId
    private Carreramodalidadid id;

    @ManyToOne
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @ManyToOne
    @MapsId("idModalidad")
    @JoinColumn(name = "id_modalidad")
    private Modalidadtitulacion modalidad;

    @Column(name = "activo")
    private Boolean activo = true;
}
