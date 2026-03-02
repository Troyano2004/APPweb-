package com.erwin.backend.dtos;
//Devolver lista de estudiantes de la carrera

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstudianteItemDto {
    private Integer idEstudiante;
    private String nombre;
}