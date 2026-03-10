package com.erwin.backend.dtos;

import com.erwin.backend.enums.EstadoDocumento;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;


@Data
public class DocumentoTitulacionDto {
    private Integer id;
    private Integer idEstudiante;
    private Integer idDirector;
    private EstadoDocumento estado;

    // ✅ NUEVO: Datos del estudiante
    private String nombreEstudiante;
    private String carreraEstudiante;

    private String titulo;
    private String ciudad;
    private Integer anio;

    private String resumen;
    private String abstractText;
    private String introduccion;
    private String planteamientoProblema;
    private String objetivoGeneral;
    private String objetivosEspecificos;
    private String justificacion;
    private String marcoTeorico;
    private String metodologia;
    private String resultados;
    private String discusion;
    private String conclusiones;
    private String recomendaciones;
    private String bibliografia;
    private String anexos;

    private String tribunal;
    private LocalDate fechaSustentacion;
    private LocalTime horaSustentacion;
    private String lugarSustentacion;

    private String feedbackIa;
    private String estadoRevisionIa;
}