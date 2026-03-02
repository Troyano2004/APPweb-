package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActaRevisionTutorResponse {
    private Integer idActa;
    private Integer idTutoria;

    private String directorNombre;
    private String directorCargo;
    private String directorFirma;

    private String estudianteNombre;
    private String estudianteCargo;
    private String estudianteFirma;

    private String tituloProyecto;
    private String objetivo;
    private String detalleRevision;
    private String cumplimiento;
    private String observaciones;

    private String conclusion;
}
