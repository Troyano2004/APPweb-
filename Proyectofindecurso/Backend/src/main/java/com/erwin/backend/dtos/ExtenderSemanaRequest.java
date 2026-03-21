package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExtenderSemanaRequest {
    private LocalDate fechaFin;        // Nueva fecha fin (puede ser igual o mayor)
    private LocalTime horaInicio;      // Nueva hora inicio del día
    private LocalTime horaFin;         // Nueva hora fin del día
    private Integer   duracionMinutos; // Nueva duración por defensa
    private String    lugarDefecto;    // Nuevo lugar por defecto
    private String    observaciones;   // Nuevas observaciones
}