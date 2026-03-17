package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobRequest {

    private String  nombre;
    private String  pgDumpPath;
    private String  pgHost;
    private Integer pgPort;
    private String  pgUsuario;
    private String  pgPassword;
    private String  databases;
    private Boolean comprimir;
    private String  cronFull;
    private String  cronDiferencial;
    private Boolean diferencialActivo;
    private String  zonaHoraria;
    private String  ventanaExcluirInicio;
    private String  ventanaExcluirFin;
    private Integer maxReintentos;
    private String  emailExito;
    private String  emailFallo;
    private Boolean activo;
    private List<BackupDestinationRequest> destinos;
}