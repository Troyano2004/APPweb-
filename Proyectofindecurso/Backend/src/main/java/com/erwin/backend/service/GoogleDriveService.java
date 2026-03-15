
package com.erwin.backend.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Servicio Google Drive con OAuth 2.0 (cuenta personal).
 *
 * La primera vez que se ejecute abrirá el navegador para
 * que el usuario autorice el acceso. Después guarda el token
 * localmente y no vuelve a pedir autorización.
 */
@Service
public class GoogleDriveService {

    private static final String APP_NAME   = "SGA-Backup";
    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_FILE);

    @Value("${backup.drive.credentials-path:classpath:drive-oauth-credentials.json}")
    private String credentialsPath;

    @Value("${backup.drive.tokens-path:tokens}")
    private String tokensPath;

    // ── Subir archivo ─────────────────────────────────────────────────────────

    public String subirArchivo(Path archivo, String folderId) throws IOException {
        Drive drive = crearCliente();

        File metadata = new File();
        metadata.setName(archivo.getFileName().toString());
        if (folderId != null && !folderId.isBlank()) {
            metadata.setParents(Collections.singletonList(folderId));
        }

        String fileName = archivo.getFileName().toString().toLowerCase();
        String mimeType = fileName.endsWith(".zip") ? "application/zip" : "text/plain";

        FileContent mediaContent = new FileContent(mimeType, archivo.toFile());

        File uploaded = drive.files()
                .create(metadata, mediaContent)
                .setFields("id, name, size")
                .execute();

        return uploaded.getId();
    }

    // ── Eliminar archivo ──────────────────────────────────────────────────────

    public void eliminarArchivo(String driveFileId) {
        try {
            Drive drive = crearCliente();
            drive.files().delete(driveFileId).execute();
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo eliminar de Drive: "
                    + driveFileId + " — " + e.getMessage());
        }
    }

    // ── Crear cliente OAuth ───────────────────────────────────────────────────

    private Drive crearCliente() throws IOException {
        try {
            // Cargar credenciales OAuth desde resources
            InputStream is = obtenerInputStream();
            GoogleClientSecrets secrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(is)
            );

            // Construir el flujo OAuth
            // Los tokens se guardan en la carpeta "tokens" dentro del directorio
            // de trabajo del servidor (junto al .jar o en el proyecto)
            java.io.File tokensDir = new java.io.File(tokensPath);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    secrets,
                    SCOPES
            )
                    .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                    .setAccessType("offline")
                    .build();

            // Si ya hay token guardado lo usa directamente.
            // Si no, abre el navegador para autorizar (solo la primera vez).
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(8888)
                    .build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                    .authorize("user");

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            )
                    .setApplicationName(APP_NAME)
                    .build();

        } catch (Exception e) {
            throw new IOException("Error creando cliente de Google Drive: " + e.getMessage(), e);
        }
    }

    private InputStream obtenerInputStream() throws IOException {
        String path = credentialsPath;

        if (path.startsWith("classpath:")) {
            String resource = path.replace("classpath:", "");
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new IOException(
                        "No se encontró: " + resource + " en resources/. " +
                                "Verifica que drive-oauth-credentials.json esté en src/main/resources/"
                );
            }
            return is;
        }

        return new java.io.FileInputStream(path);
    }
}