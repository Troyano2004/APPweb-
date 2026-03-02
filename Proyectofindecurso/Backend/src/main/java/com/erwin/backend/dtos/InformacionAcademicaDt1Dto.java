package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class InformacionAcademicaDt1Dto {
    private Integer idCarrera;
    private String carrera;

    private Integer idPeriodoAcademico;
    private String periodoAcademico;

    // 👇 1) Docentes que pertenecen a la carrera (para habilitar)
    private List<DocenteItemDto> docentesCarrera;

    // 👇 2) Docentes ya habilitados como DT1 en el periodo (para asignar tutores)
    private List<DocenteItemDto> docentesDt1;

    private List<EstudianteItemDto> estudiantesDisponibles;

    @Getter @Setter
    public static class DocenteItemDto {
        private Integer idDocente;
        private String nombre;
    }

    @Getter @Setter
    public static class EstudianteItemDto {
        private Integer idEstudiante;
        private String nombre;
    }
}