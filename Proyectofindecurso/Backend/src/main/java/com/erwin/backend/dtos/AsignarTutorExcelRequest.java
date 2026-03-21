package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AsignarTutorExcelRequest {
    private Integer idUsuario;
    private Integer idDocente;
    private List<String> cedulas;
}