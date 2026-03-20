package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequest {

    private Long    idExecution;      // ID del backup a restaurar
    private Long    idJob;            // Job al que pertenece
    private String  modo;             // "REEMPLAZAR" o "NUEVA_BD"
    private String  nombreBdNueva;    // Solo si modo = NUEVA_BD
}