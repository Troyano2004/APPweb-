package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class Dt1EnviadoResponse {
    private Integer idAnteproyecto;
    private Integer idEstudiante;
    private String estudiante;
    private String titulo;
    private String estado;
    private Integer version;
    private LocalDate fechaEnvio;
}