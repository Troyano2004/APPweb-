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
public class BackupDestinationRequest {

    private Long        idDestination;
    private TipoDestino tipo;
    private Boolean     activo;

    private String  rutaLocal;

    private String  azureAccount;
    private String  azureKey;
    private String  azureContainer;

    private String  gdriveCuenta;
    private String  gdriveRefreshToken;
    private String  gdriveFolderId;
    private String  gdriveFolderNombre;

    private String  s3Bucket;
    private String  s3Region;
    private String  s3AccessKey;
    private String  s3SecretKey;

    private Integer retencionMeses;
    private Integer retencionDias;
    private Integer maxBackups;
}