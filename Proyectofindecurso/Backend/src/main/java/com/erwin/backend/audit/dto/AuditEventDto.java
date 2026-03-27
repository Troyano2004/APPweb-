package com.erwin.backend.audit.dto;
import lombok.*;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEventDto {
    private String  entidad;
    private String  entidadId;
    private String  accion;
    private Integer idUsuario;
    private String  username;
    private String  correoUsuario;
    private String  ipAddress;
    private Object  estadoAnterior;
    private Object  estadoNuevo;
    private Map<String, Object> metadata;
    private Integer duracionMs;
}
