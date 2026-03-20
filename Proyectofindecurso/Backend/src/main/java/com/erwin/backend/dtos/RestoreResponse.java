package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResponse {

    private Long          idExecution;
    private Long          idJob;
    private String        jobNombre;
    private String        databaseNombre;
    private String        archivoNombre;
    private String        archivoRuta;
    private Long          tamanoBytes;
    private String        estado;          // EXITOSO, FALLIDO, EN_PROCESO, PENDIENTE
    private LocalDateTime iniciadoEn;
    private boolean       archivoDisponible; // si el archivo físico existe
    private String        destinoTipo;
}