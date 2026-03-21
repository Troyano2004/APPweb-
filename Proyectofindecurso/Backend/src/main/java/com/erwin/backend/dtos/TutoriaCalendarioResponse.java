package com.erwin.backend.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutoriaCalendarioResponse {
    private Integer idTutoria;
    private LocalDate fecha;
    private LocalTime hora;
    private String modalidad;
    private String estado;
    private String linkReunion;
    private String estudianteNombre;
    private String tituloProyecto;
}