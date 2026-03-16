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
public class BackupTestDestinoRequest {

    private TipoDestino tipo;

    private String rutaLocal;

    private String azureAccount;
    private String azureKey;
    private String azureContainer;

    private String s3Bucket;
    private String s3Region;
    private String s3AccessKey;
    private String s3SecretKey;
}