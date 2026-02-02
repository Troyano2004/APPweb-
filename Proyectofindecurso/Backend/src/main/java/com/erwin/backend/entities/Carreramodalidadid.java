package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Carreramodalidadid implements Serializable {
    @Column(name = "id_carrera")
    private Integer idCarrera;
    @Column(name = "id_modalidad")
    private Integer idModalidad;

}
