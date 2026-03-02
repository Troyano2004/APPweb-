package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dt1AsignarTutorResponse {
    private Integer idTutorEstudiante;
    private Integer idEstudiante;
    private Integer idDocente;
    private Integer idPeriodo;
    private String estado; // "ASIGNADO" / "ACTUALIZADO"
}