package com.erwin.backend.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CoordinadorAdminResponse {
    private Integer idCoordinador;
    private Integer idUsuario;
    private String nombres;
    private String apellidos;
    private String username;
    private Integer idCarrera;
    private String carrera;
    private Boolean activo;
}