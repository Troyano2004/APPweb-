package com.erwin.backend.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AsignarCoordinadorRequest {
    private Integer idUsuario;
    private Integer idCarrera;
}