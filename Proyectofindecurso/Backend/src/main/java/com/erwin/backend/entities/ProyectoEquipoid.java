package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ProyectoEquipoid {
    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @Column(name = "id_estudiante")
    private Integer idEstudiante;
}
