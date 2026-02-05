package com.erwin.backend.dtos;

import lombok.Data;

@Data
public class DocumentoUpdateRequest {
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
}
