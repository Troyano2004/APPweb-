package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Dt1RevisionRequest {
    private Integer idAnteproyecto;
    private Integer idDocente;
    private String decision;      // APROBADO / RECHAZADO
    private String observacion;
}