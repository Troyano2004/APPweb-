package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpedienteEstudianteDto {
    // Datos del estudiante
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private String carrera;
    private BigDecimal promedioRecord;
    private Boolean discapacidad;

    // Datos del proyecto
    private String tituloProyecto;
    private String tipoTrabajo;
    private String estadoProyecto;
    private String periodo;
    private String director;
    private BigDecimal porcentajeAntiplagio;
    private LocalDate fechaAntiplagio;

    // Tutorías
    private Integer totalTutorias;
    private Integer tutoriasRealizadas;
    private Integer tutoriasPendientes;
    private List<TutoriaReporteDto> tutorias;

    // Tribunal
    private List<TribunalReporteDto> tribunal;

    // Sustentaciones
    private List<SustentacionReporteDto> sustentaciones;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TutoriaReporteDto {
        private LocalDate fecha;
        private String modalidad;
        private String estado;
        private String docente;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TribunalReporteDto {
        private String docente;
        private String cargo;
        private String titulo4toNivel;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SustentacionReporteDto {
        private String tipo;
        private LocalDate fecha;
        private String lugar;
        private BigDecimal notaFinal;
        private String observaciones;
    }
}
