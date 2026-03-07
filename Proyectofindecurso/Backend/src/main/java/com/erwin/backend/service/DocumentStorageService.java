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
/**
 * Servicio para subir documentos (PDF) a Azure Blob Storage.
 * Complementa ImageStorageService que solo maneja imágenes.
 */
@Service
public class DocumentStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "PDF");
    @Value("${azure.storage.connection-string}")
    private String connectionString;
    @Value("${azure.storage.container-name}")
    private String containerName;
    /**
     * Sube un PDF a Azure Blob Storage y retorna la URL pública.
     */
    public String storeDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 50MB");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)
                && (contentType == null || !contentType.equalsIgnoreCase("application/pdf"))) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        String storedFileName = "documentos/" + UUID.randomUUID() + ".pdf";
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(storedFileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            return blobClient.getBlobUrl();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el documento en Azure: " + e.getMessage(), e);
        }
    }
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}