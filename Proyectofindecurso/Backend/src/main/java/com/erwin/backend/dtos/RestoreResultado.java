package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResultado {

    private boolean exitoso;
    private String  mensaje;
    private String  log;
    private String  bdRestaurada;   // nombre de la BD donde se restauró
    private Long    duracionSegundos;
}