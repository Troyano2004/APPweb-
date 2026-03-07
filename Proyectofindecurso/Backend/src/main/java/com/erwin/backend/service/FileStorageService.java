package com.erwin.backend.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Servicio para subir archivos PDF/documentos a Azure Blob Storage.
 * Separado de ImageStorageService (que solo acepta imágenes).
 */
@Service
public class FileStorageService {

    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024; // 20 MB

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String storeFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el límite de 20 MB.");
        }

        // Validar PDF por extensión (más confiable que content-type)
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        if (!originalName.endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF.");
        }

        // Guardar en subcarpeta "docs/" dentro del contenedor
        String storedFileName = "docs/" + UUID.randomUUID() + ".pdf";

        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient =
                    blobServiceClient.getBlobContainerClient(containerName);

            BlobClient blobClient = containerClient.getBlobClient(storedFileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo en Azure.", e);
        }
    }
}