package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudRegistroRequest {
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private Integer idCarrera;
}
