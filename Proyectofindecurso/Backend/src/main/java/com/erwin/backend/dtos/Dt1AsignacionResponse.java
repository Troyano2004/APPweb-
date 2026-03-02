package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dt1AsignacionResponse {
    private Integer idAsignacion;
    private Integer idDocente;
    private String docenteNombre;
    private Integer idCarrera;
    private String carreraNombre;
    private Integer idPeriodo;
    private String periodo;
    private Boolean activo;
}