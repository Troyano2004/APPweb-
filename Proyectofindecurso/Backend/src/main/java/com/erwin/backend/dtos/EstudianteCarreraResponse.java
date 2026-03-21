package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteCarreraResponse {
    private Integer idEstudiante;
    private String nombres;
    private String apellidos;
    private String cedula;
    private String carrera;
    private Boolean tieneTutor;
    private String tutorNombre;
}