package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dt1AsignacionCreateRequest {
    private Integer idUsuario;
    private Integer idDocente;
    private Integer idCarrera;
    private Integer idPeriodo;
}
