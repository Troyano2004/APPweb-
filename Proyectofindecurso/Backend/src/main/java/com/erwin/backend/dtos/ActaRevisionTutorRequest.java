package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActaRevisionTutorRequest {

    // Participantes
    private String directorCargo;
    private String directorFirma;

    private String estudianteCargo;
    private String estudianteFirma;

    // Proyecto
    private String tituloProyecto;

    // Proceso revisión
    private String objetivo;
    private String detalleRevision;

    // NINGUNO / PARCIAL / COMPLETO
    private String cumplimiento;

    private String observaciones;

    private String conclusion;
}
