package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignarCarreraDocenteRequest {
    private Integer idDocente;
    private Integer idCarrera;
}
