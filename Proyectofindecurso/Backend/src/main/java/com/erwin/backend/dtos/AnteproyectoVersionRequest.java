package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnteproyectoVersionRequest {
    public String titulo;
    public String temaInvestigacion;
    public String planteamientoProblema;
    public String objetivosGenerales;
    public String objetivosEspecificos;
    public String marcoTeorico;
    public String metodologia;
    public String resultadosEsperados;
    public String bibliografia;
    public String comentarioCambio;
}