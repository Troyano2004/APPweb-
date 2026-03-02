package com.erwin.backend.dtos;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarreraDto {
    private Integer idCarrera;
    private String nombre;
    private Integer idFacultad;
    private String nombreFacultad;
}