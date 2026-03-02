package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisAnteproyectosDt1Response {
    private Integer idAnteproyecto;
    private String tituloProyecto;
    private String estudianteNombre;
    private String periodo;
    private String estadoAnteproyecto;
}
