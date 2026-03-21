package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocenteCarreraResponse {
    private Integer idDocenteCarrera;
    private Integer idDocente;
    private String nombres;
    private String apellidos;
    private String username;
    private Integer idCarrera;
    private String carrera;
    private Boolean tieneCarrera;
    private Boolean activo;
}