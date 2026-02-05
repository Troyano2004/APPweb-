package com.erwin.backend.dtos;

import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.enums.SeccionDocumento;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ObservacionDto {
    private Integer id;
    private Integer idDocumento;
    private SeccionDocumento seccion;
    private String comentario;
    private EstadoObservacion estado;

    private Integer idAutor;
    private String autorNombre;

    private LocalDateTime creadoEn;
}
