package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReporteAsistenciaResponse {
    private Integer idEstudiante;
    private String estudiante;
    private String tituloProyecto;
    private int totalTutorias = 0;
    private List<TutoriaItem> tutorias = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor
    public static class TutoriaItem {
        private Integer idTutoria;
        private LocalDate fecha;
        private LocalTime hora;
        private String modalidad;
    }
}