package com.erwin.backend.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");

    // Inyectamos las credenciales desde el application.properties
    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ninguna imagen");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido de 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten archivos de imagen");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato de imagen no permitido");
        }

        String storedFileName = UUID.randomUUID() + "." + extension;

        try {
            // Conexión con tu cuenta appwebtroyano2026
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            // Referencia a tu contenedor (carpeta)
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Preparar el archivo que se va a subir
            BlobClient blobClient = containerClient.getBlobClient(storedFileName);

            // Subir a Azure
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            // Retornar la URL pública generada por Azure
            return blobClient.getBlobUrl();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen en Azure", e);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}