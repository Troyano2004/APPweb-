package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecisionSolicitudRequest {

    private String decision;
    // APROBAR o RECHAZAR

    private String motivo;
    // solo si se rechaza

}