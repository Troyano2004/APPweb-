package com.erwin.backend.audit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SesionActivaDto {
    private String username;
    private String rol;
    private String correo;
    private String ip;
    private String sessionId;
    private LocalDateTime inicio;
    private long minutosActivo;
}
