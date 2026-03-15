package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificarCodigoRequest {
    private String correo;
    private String codigo;
}
