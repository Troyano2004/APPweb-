package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnteproyectoResponse {
    private Integer idAnteproyecto;
    private String estado; // NO_DISPONIBLE / BORRADOR / EN_REVISION / APROBADA / RECHAZADA

    private Integer idEstudiante;
    private String nombresEstudiante;
    private String apellidosEstudiante;

    private String mensaje;

    private AnteproyectoVersionResponse ultimaVersion;

    private PropuestaSnapshot propuesta;

    @Getter @Setter
    public static class PropuestaSnapshot {
        public Integer idPropuesta;
        public Integer idTema;
        public String tituloTema;

        public String titulo;
        public String temaInvestigacion;
        public String planteamientoProblema;
        public String objetivoGeneral;
        public String objetivosEspecificos;
        public String metodologia;
        public String bibliografia;
    }
}