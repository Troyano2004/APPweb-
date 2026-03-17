package com.erwin.backend.dtos;

import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobDto {

    private Long              idJob;
    private String            nombre;
    private String            pgDumpPath;
    private String            pgHost;
    private Integer           pgPort;
    private String            pgUsuario;
    private String            databases;
    private Boolean           comprimir;
    private String            cronFull;
    private String            cronDiferencial;
    private Boolean           diferencialActivo;
    private String            zonaHoraria;
    private String            ventanaExcluirInicio;
    private String            ventanaExcluirFin;
    private Integer           maxReintentos;
    private String            emailExito;
    private String            emailFallo;
    private Boolean           activo;
    private LocalDateTime     proximaEjecucion;
    private LocalDateTime     ultimaEjecucion;
    private LocalDateTime     creadoEn;
    private EstadoEjecucion   ultimoEstado;
    private List<BackupDestinationDto> destinos;
}