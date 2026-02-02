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
@Table(name = "proyecto_equipo")
public class proyectoequipo {
    @EmbeddedId
    private proyectoequipoid id;

    @ManyToOne
    @MapsId("idProyecto")
    @JoinColumn(name = "id_proyecto")
    private Proyectotitulacion proyecto;

    @ManyToOne
    @MapsId("idEstudiante")
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;
}
