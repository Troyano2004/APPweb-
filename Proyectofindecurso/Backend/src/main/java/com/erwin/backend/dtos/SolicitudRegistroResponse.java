package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRegistroResponse {
    private Integer idSolicitud;
    private String correo;
    private String estado;
    private String mensaje;
}
