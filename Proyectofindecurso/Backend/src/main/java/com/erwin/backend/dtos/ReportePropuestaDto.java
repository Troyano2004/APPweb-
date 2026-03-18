package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

public class ReportePropuestaDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ResumenReporte {
        private long total;
        private long enviadas;
        private long enRevision;
        private long aprobadas;
        private long rechazadas;
        private double porcentajeAprobacion;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ItemReporte {
        private Integer idPropuesta;
        private String estudiante;
        private String cedula;
        private String carrera;
        private String titulo;
        private String temaInvestigacion;
        private String estado;
        private LocalDate fechaEnvio;
        private LocalDate fechaRevision;
        private String observacionesComision;
        // Dictamen más reciente
        private String decisionDirector;
        private String observacionesDirector;
        private String nombreDirector;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RespuestaCompleta {
        private ResumenReporte resumen;
        private List<ItemReporte> propuestas;
    }
}