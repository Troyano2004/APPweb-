package com.erwin.backend.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SesionActivaDto {
    private Integer id;
    private Integer idUsuario;
    private String nombres;
    private String apellidos;
    private String rol;
    private String ip;
    private LocalDateTime fechaInicio;
    private LocalDateTime ultimaActividad;
}