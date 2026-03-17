package com.erwin.backend.dtos;

import com.erwin.backend.entities.BackupDestination.TipoDestino;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupDestinationDto {

    private Long        idDestination;
    private TipoDestino tipo;
    private Boolean     activo;

    private String  rutaLocal;

    private String  azureAccount;
    private String  azureContainer;
    private Boolean azureConfigurado;

    private String  gdriveCuenta;
    private String  gdriveFolderId;
    private String  gdriveFolderNombre;
    private Boolean gdriveConectado;

    private String  s3Bucket;
    private String  s3Region;
    private String  s3AccessKey;
    private Boolean s3Configurado;

    private Integer retencionMeses;
    private Integer retencionDias;
    private Integer maxBackups;
}