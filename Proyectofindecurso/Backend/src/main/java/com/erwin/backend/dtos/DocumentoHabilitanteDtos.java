package com.erwin.backend.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DocumentoHabilitanteDtos {

    // ──────────────────────────────────────────────────────────────
    // REQUEST: Subir / actualizar un documento habilitante
    // ──────────────────────────────────────────────────────────────
    @Data
    public static class SubirHabilitanteRequest {

        /** ID del proyecto de titulación al que pertenece */
        private Integer idProyecto;

        /**
         * Tipo de documento. Valores permitidos:
         * INFORME_DIRECTOR | CERTIFICADO_ANTIPLAGIO | TRABAJO_FINAL_PDF |
         * CERTIFICADO_PENSUM | CERTIFICADO_DEUDAS | CERTIFICADO_IDIOMA | CERTIFICADO_PRACTICAS
         */
        private String tipoDocumento;

        /** URL pública del archivo ya subido a Azure Blob (via /api/uploads/files) */
        private String urlArchivo;

        /** Nombre original del archivo para mostrar en UI */
        private String nombreArchivo;

        // ── Solo para CERTIFICADO_ANTIPLAGIO ──
        /** Porcentaje de coincidencia reportado por COMPILATIO */
        private BigDecimal porcentajeCoincidencia;

        /** Umbral permitido (default 10.00 según Art. 57 num.2) */
        private BigDecimal umbralPermitido;
    }

    // ──────────────────────────────────────────────────────────────
    // REQUEST: Validar (aprobar / rechazar) un habilitante
    // ──────────────────────────────────────────────────────────────
    @Data
    public static class ValidarHabilitanteRequest {
        /** APROBADO | RECHAZADO */
        private String decision;
        private String comentario;
        private BigDecimal porcentajeCoincidencia;  // solo para CERTIFICADO_ANTIPLAGIO
    }

    // ──────────────────────────────────────────────────────────────
    // RESPONSE: representación de un documento habilitante
    // ──────────────────────────────────────────────────────────────
    @Data
    public static class HabilitanteDto {
        private Integer id;
        private Integer idProyecto;
        private Integer idEstudiante;
        private String  nombreEstudiante;

        private String  tipoDocumento;
        private String  etiquetaTipo;        // Label legible para UI
        private String  descripcionTipo;     // Texto del reglamento
        private Boolean obligatorio;

        private String  nombreArchivo;
        private String  urlArchivo;
        private String  formato;

        // Antiplagio
        private BigDecimal porcentajeCoincidencia;
        private BigDecimal umbralPermitido;
        private String     resultadoAntiplagio;

        // Flujo
        private String  estado;
        private String  comentarioValidacion;
        private String  validadoPorNombre;
        private LocalDateTime fechaValidacion;
        private LocalDateTime fechaSubida;
        private LocalDateTime actualizadoEn;
    }

    // ──────────────────────────────────────────────────────────────
    // RESPONSE: resumen de habilitación completa del proyecto
    // ──────────────────────────────────────────────────────────────
    @Data
    public static class ResumenHabilitacionDto {
        private Integer idProyecto;
        private String  tituloProyecto;
        private boolean habilitadoParaSustentacion;
        private int     totalDocumentos;
        private int     aprobados;
        private int     pendientes;
        private int     rechazados;
        private java.util.List<HabilitanteDto> documentos;
    }
    // ──────────────────────────────────────────────────────────────
    // REQUEST: Director sube certificado antiplagio (Art. 57 num.2)
    // ──────────────────────────────────────────────────────────────
    @Data
    public static class SubirAntiplagioPorDirectorRequest {
        /** URL del certificado PDF ya subido a Azure */
        private String urlArchivo;
        /** Nombre del archivo para mostrar en UI */
        private String nombreArchivo;
        /** Porcentaje real que indica el reporte COMPILATIO */
        private BigDecimal porcentajeCoincidencia;
    }
}