package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class TutoriaHistorialResponse {

    private Integer idTutoria;
    private LocalDate fecha;
    private LocalTime hora;
    private String modalidad;
    private String estado;

    private String directorNombre;
    private String linkReunion;

    // Datos del acta (si existe)
    private Integer idActa;
    private String objetivo;
    private String detalleRevision;
    private String observaciones;
    private String cumplimiento;
    private String conclusion;
}