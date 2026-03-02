package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Dt1DetalleResponse {

    private Integer idAnteproyecto;
    private String estadoAnteproyecto;

    private Integer idVersion;
    private Integer numeroVersion;
    private String estadoVersion;
    private LocalDate fechaEnvio;

    private String estudiante;
    private String periodo;

    public String titulo;
    public String temaInvestigacion;
    public String planteamientoProblema;
    public String objetivosGenerales;
    public String objetivosEspecificos;
    public String marcoTeorico;
    public String metodologia;
    public String resultadosEsperados;
    public String bibliografia;
}