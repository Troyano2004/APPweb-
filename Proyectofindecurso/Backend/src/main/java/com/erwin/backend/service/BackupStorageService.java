package com.erwin.backend.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.entities.BackupExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupStorageService {

    private final BackupEncryptionUtil    encryption;
    private final GoogleDriveUploadService driveService;

    public void subir(Path archivo, BackupDestination destino, BackupExecution exec) {
        switch (destino.getTipo()) {
            case LOCAL        -> subirLocal(archivo, destino, exec);
            case AZURE        -> subirAzure(archivo, destino, exec);
            case S3           -> subirS3(archivo, destino, exec);
            case GOOGLE_DRIVE -> subirGoogleDrive(archivo, destino, exec);
        }
    }

    private void subirLocal(Path archivo, BackupDestination destino, BackupExecution exec) {
        try {
            Path destDir  = Paths.get(destino.getRutaLocal());
            Files.createDirectories(destDir);
            Path destPath = destDir.resolve(archivo.getFileName());
            Files.copy(archivo, destPath, StandardCopyOption.REPLACE_EXISTING);
            exec.setArchivoRuta(destPath.toString());
            log.info("Backup guardado localmente: {}", destPath);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando backup local: " + e.getMessage(), e);
        }
    }

    private void subirAzure(Path archivo, BackupDestination destino, BackupExecution exec) {
        try {
            String key     = encryption.decrypt(destino.getAzureKeyEnc());
            String connStr = String.format(
                    "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                    destino.getAzureAccount(), key);
            BlobServiceClient   serviceClient   = new BlobServiceClientBuilder().connectionString(connStr).buildClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(destino.getAzureContainer());
            if (!containerClient.exists()) containerClient.create();
            BlobClient blobClient = containerClient.getBlobClient(archivo.getFileName().toString());
            blobClient.uploadFromFile(archivo.toString(), true);
            exec.setArchivoRuta(blobClient.getBlobUrl());
            log.info("Backup subido a Azure: {}", blobClient.getBlobUrl());
        } catch (Exception e) {
            throw new RuntimeException("Error subiendo a Azure: " + e.getMessage(), e);
        }
    }

    private void subirS3(Path archivo, BackupDestination destino, BackupExecution exec) {
        try {
            String   secretKey = encryption.decrypt(destino.getS3SecretKeyEnc());
            S3Client s3        = S3Client.builder()
                    .region(Region.of(destino.getS3Region()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(destino.getS3AccessKey(), secretKey)))
                    .build();
            s3.putObject(
                    PutObjectRequest.builder().bucket(destino.getS3Bucket()).key(archivo.getFileName().toString()).build(),
                    archivo);
            String url = String.format("s3://%s/%s", destino.getS3Bucket(), archivo.getFileName());
            exec.setArchivoRuta(url);
            log.info("Backup subido a S3: {}", url);
        } catch (Exception e) {
            throw new RuntimeException("Error subiendo a S3: " + e.getMessage(), e);
        }
    }

    private void subirGoogleDrive(Path archivo, BackupDestination destino, BackupExecution exec) {
        if (destino.getGdriveRefreshTokenEnc() == null || destino.getGdriveRefreshTokenEnc().isBlank()) {
            throw new RuntimeException("Google Drive no está conectado. Autoriza la cuenta desde la configuración del job.");
        }
        String url = driveService.subirArchivo(archivo, destino, exec);
        exec.setArchivoRuta(url);
    }

    public boolean probarLocal(String ruta) {
        try {
            Path path = Paths.get(ruta);
            Files.createDirectories(path);
            return Files.isWritable(path);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean probarAzure(String account, String key, String container) {
        try {
            String connStr = String.format(
                    "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                    account, key);
            new BlobServiceClientBuilder().connectionString(connStr).buildClient()
                    .getBlobContainerClient(container).exists();
            return true;
        } catch (Exception e) {
            log.warn("Prueba Azure fallida: {}", e.getMessage());
            return false;
        }
    }

    public boolean probarS3(String bucket, String region, String accessKey, String secretKey) {
        try {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            s3.headBucket(b -> b.bucket(bucket));
            return true;
        } catch (Exception e) {
            log.warn("Prueba S3 fallida: {}", e.getMessage());
            return false;
        }
    }
}