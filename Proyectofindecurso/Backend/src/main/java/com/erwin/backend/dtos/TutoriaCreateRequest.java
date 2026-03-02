package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class TutoriaCreateRequest {

    private LocalDate fecha;
    private LocalTime hora;

    // PRESENCIAL / VIRTUAL
    private String modalidad;
}
