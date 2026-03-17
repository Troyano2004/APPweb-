package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportePeriodoDto {
    private String periodo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer totalEstudiantes;
    private Integer proyectosFinalizados;
    private Integer proyectosEnProceso;
    private Integer proyectosAnteproyecto;
    private Integer proyectosPredefensa;
    private Integer proyectosDefensa;
    private List<ProyectoPeriodoDto> proyectos;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoPeriodoDto {
        private String estudiante;
        private String cedula;
        private String titulo;
        private String tipoTrabajo;
        private String estado;
        private String director;
        private String carrera;
    }
}
