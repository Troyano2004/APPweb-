package com.erwin.backend.audit.dto;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditStatsDto {
    private long   totalHoy, totalSemana, eventosCriticos24h;
    private long   totalCriticos;
    private String ultimoEvento;
    private List<EntidadCount> topEntidades;
    private List<AccionCount>  topAcciones;
    private List<UsuarioCount> topUsuarios;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EntidadCount { private String entidad; private Long total; }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AccionCount  { private String accion;  private Long total; }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioCount { private String username; private Long total; }
}
