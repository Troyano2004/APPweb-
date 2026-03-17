package com.erwin.backend.dtos;

import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import com.erwin.backend.entities.BackupExecution.TipoBackup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupExecutionResponse {

    private Long            idExecution;
    private Long            idJob;
    private String          jobNombre;
    private EstadoEjecucion estado;
    private TipoBackup      tipoBackup;
    private String          databaseNombre;
    private String          archivoNombre;
    private Long            tamanoBytes;
    private String          destinoTipo;
    private String          errorMensaje;
    private Integer         intentoNumero;
    private LocalDateTime   iniciadoEn;
    private LocalDateTime   finalizadoEn;
    private Long            duracionSegundos;
    private Boolean         manual;
}