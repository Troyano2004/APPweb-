package com.erwin.backend.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TitulacionWorkflowDtos {

    @Data
    public static class PrepararTribunalRequest {
        private String avalUrlPdf;
        private String avalComentario;
        private BigDecimal porcentajeAntiplagio;
        private BigDecimal umbralAntiplagio;
        private String urlInformeAntiplagio;
    }

    @Data
    public static class MiembroTribunalRequest {
        private Integer idDocente;
        private String cargo;
    }

    @Data
    public static class AsignarTribunalRequest {
        private List<MiembroTribunalRequest> miembros;
    }

    @Data
    public static class AgendarSustentacionRequest {
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
        private String observaciones;
        private String motivoReprogramacion;
    }

    @Data
    public static class NotaTribunalRequest {
        private Integer idDocente;
        private BigDecimal nota;
    }

    @Data
    public static class RegistrarResultadoRequest {
        private BigDecimal notaDocente;
        private List<NotaTribunalRequest> notasTribunal;
        private String actaUrl;
        private String actaFirmadaUrl;
        private String resultado;
        private String observaciones;
    }

    @Data
    public static class CerrarExpedienteRequest {
        private String resultadoFinal;
        private String observacionesFinales;
    }

    @Data
    public static class WorkflowResumenDto {
        private Integer idDocumento;
        private String estado;
        private String mensaje;
        private BigDecimal notaFinal;
        private BigDecimal notaDocente;
        private BigDecimal notaTribunal;
    }
}
