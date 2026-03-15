package com.erwin.backend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Servicio para subir archivos a Google Drive usando una
 * Service Account (cuenta de servicio).
 *
 * ── CÓMO OBTENER LAS CREDENCIALES ──────────────────────────────────────────
 *  1. Ir a https://console.cloud.google.com
 *  2. Crear un proyecto (o usar uno existente)
 *  3. Habilitar la API "Google Drive API"
 *  4. Crear credenciales → "Cuenta de servicio"
 *  5. Descargar el JSON de la cuenta de servicio
 *  6. Poner ese JSON en: src/main/resources/drive-credentials.json
 *  7. En Google Drive, compartir la carpeta destino con el email
 *     de la cuenta de servicio (termina en @...iam.gserviceaccount.com)
 *     dándole permiso de "Editor"
 *  8. Copiar el ID de esa carpeta (parte final de la URL de Drive)
 *     y pegarlo en la configuración del módulo de respaldos.
 * ──────────────────────────────────────────────────────────────────────────
 */
@Service
public class GoogleDriveService {

    private static final String APP_NAME = "SGA-Backup";

    @Value("${backup.drive.credentials-path:classpath:drive-credentials.json}")
    private String credentialsPath;

    /**
     * Sube un archivo al Drive usando la Service Account.
     *
     * @param archivo      Ruta local del archivo a subir (.zip o .sql)
     * @param folderId     ID de la carpeta de destino en Google Drive
     * @return             ID del archivo subido en Drive
     */
    public String subirArchivo(Path archivo, String folderId) throws IOException {
        Drive drive = crearCliente();

        File metadata = new File();
        metadata.setName(archivo.getFileName().toString());
        if (folderId != null && !folderId.isBlank()) {
            metadata.setParents(Collections.singletonList(folderId));
        }

        // Detectar media type
        String fileName = archivo.getFileName().toString().toLowerCase();
        String mimeType = fileName.endsWith(".zip") ? "application/zip" : "application/sql";

        com.google.api.client.http.FileContent mediaContent =
                new com.google.api.client.http.FileContent(mimeType, archivo.toFile());

        File uploaded = drive.files()
                .create(metadata, mediaContent)
                .setFields("id, name, size")
                .execute();

        return uploaded.getId();
    }

    /**
     * Elimina un archivo de Drive por su ID.
     * Se usa cuando se rotan los respaldos viejos.
     */
    public void eliminarArchivo(String driveFileId) {
        try {
            Drive drive = crearCliente();
            drive.files().delete(driveFileId).execute();
        } catch (Exception e) {
            // No lanzar — si falla el borrado en Drive no debe romper el flujo
            System.err.println("⚠️ No se pudo eliminar de Drive el archivo: " + driveFileId + " — " + e.getMessage());
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private Drive crearCliente() throws IOException {
        GoogleCredentials credentials;

        // Intenta cargar desde el path configurado
        if (credentialsPath.startsWith("classpath:")) {
            String resource = credentialsPath.replace("classpath:", "");
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new IOException(
                        "No se encontró el archivo de credenciales de Drive: " + resource +
                                "\nRevisa el archivo README del módulo de respaldos para saber cómo obtenerlo."
                );
            }
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));
        } else {
            try (FileInputStream fis = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(fis)
                        .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));
            }
        }

        try {
            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APP_NAME)
                    .build();
        } catch (Exception e) {
            throw new IOException("Error creando cliente de Google Drive: " + e.getMessage(), e);
        }
    }
}