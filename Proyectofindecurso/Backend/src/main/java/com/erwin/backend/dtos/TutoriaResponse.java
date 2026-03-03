package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class TutoriaResponse {
    private Integer idTutoria;
    private LocalDate fecha;
    private LocalTime hora;
    private String modalidad;
    private String estado;
}
