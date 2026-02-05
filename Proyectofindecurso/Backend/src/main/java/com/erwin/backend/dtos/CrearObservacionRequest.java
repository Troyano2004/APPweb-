package com.erwin.backend.dtos;

import com.erwin.backend.enums.SeccionDocumento;
import lombok.Data;

@Data
public class CrearObservacionRequest {
    private SeccionDocumento seccion;
    private String comentario;
}
