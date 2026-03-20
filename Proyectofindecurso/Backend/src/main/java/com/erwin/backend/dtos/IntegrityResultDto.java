package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class IntegrityResultDto {
    private boolean valido;
    private String  mensaje;
    private int     objetosEncontrados;   // tablas/secuencias listadas en el dump
    private long    tamanoBytes;
    private String  detalle;
}