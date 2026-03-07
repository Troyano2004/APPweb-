package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPendienteResponse {
    private Integer idSolicitud;
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private String carrera;
    private String estado;
    private String fechaSolicitud;
}
