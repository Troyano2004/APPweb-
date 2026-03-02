package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AnteproyectoVersionResponse {
    private Integer idVersion;
    private LocalDate fechaCreacion;
    private String comentarioCambio;
    private Integer numeroVersion;
    private String estadoVersion;
    // si quieres, agrega campos del formulario:
    private String titulo;
    private String temaInvestigacion;
    private String planteamientoProblema;
    private String objetivosGenerales;
    private String objetivosEspecificos;
    private String marcoTeorico;
    private String metodologia;
    private String resultadosEsperados;
    private String bibliografia;

}
