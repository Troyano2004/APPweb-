package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarClaveRequest {
    private Integer idUsuario;
    private String claveActual;
    private String claveNueva;
}
