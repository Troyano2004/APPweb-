package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActaConstanciaDto {
    private String tipoActa; // TUTORIA / SUSTENTACION / PREDEFENSA
    private String periodo;
    private String estudiante;
    private String cedula;
    private String tituloProyecto;
    private String director;
    private LocalDate fecha;
    private LocalTime hora;
    private String lugar;
    private String modalidad;
    private String objetivo;
    private String detalleRevision;
    private String cumplimiento;
    private String conclusion;
    private String observaciones;
    private List<EvaluacionActaDto> evaluaciones;
    private BigDecimal notaFinalPromedio;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EvaluacionActaDto {
        private String docente;
        private String cargo;
        private BigDecimal calidadTrabajo;
        private BigDecimal originalidad;
        private BigDecimal dominioTema;
        private BigDecimal preguntas;
        private BigDecimal notaFinal;
        private String observaciones;
    }
}
