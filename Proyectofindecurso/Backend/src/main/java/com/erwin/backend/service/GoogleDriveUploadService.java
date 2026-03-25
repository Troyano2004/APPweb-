package com.erwin.backend.service;

import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.repository.BackupDestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveUploadService {

    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String FILES_URL  = "https://www.googleapis.com/drive/v3/files";

    private final GoogleDriveOAuthService     oauthService;
    private final BackupDestinationRepository destinationRepo;
    private final RestTemplate                restTemplate;

    public String subirArchivo(Path archivo, BackupDestination destino, BackupExecution exec) {
        String accessToken = oauthService.obtenerAccessToken(destino.getGdriveRefreshTokenEnc());
        String folderId    = obtenerOCrearCarpeta(accessToken, destino);

        String metadata = String.format(
                "{\"name\":\"%s\",\"parents\":[\"%s\"]}",
                archivo.getFileName().toString(), folderId);

        // ── CORRECCIÓN: cada parte del multipart necesita su propio Content-Type,
        //    pero el Bearer va sólo en los headers exteriores de la petición. ──────
        HttpHeaders metaHeaders = new HttpHeaders();
        metaHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("metadata", new HttpEntity<>(metadata, metaHeaders));
        body.add("file",     new HttpEntity<>(new FileSystemResource(archivo.toFile()), fileHeaders));

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setBearerAuth(accessToken);          // ← Bearer en los headers raíz
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                UPLOAD_URL, HttpMethod.POST,
                new HttpEntity<>(body, requestHeaders), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error subiendo archivo a Google Drive");
        }

        String fileId  = (String) response.getBody().get("id");
        String fileUrl = "https://drive.google.com/file/d/" + fileId + "/view";
        log.info("Backup subido a Google Drive: {} -> {}", archivo.getFileName(), fileUrl);
        return fileUrl;
    }

    private String obtenerOCrearCarpeta(String accessToken, BackupDestination destino) {
        String nombreCarpeta = (destino.getGdriveFolderNombre() != null
                && !destino.getGdriveFolderNombre().isBlank())
                ? destino.getGdriveFolderNombre().trim()
                : "Backups_Sistema";

        // 1. Si ya tenemos el ID guardado, verificar que la carpeta aún exista en Drive
        if (destino.getGdriveFolderId() != null && !destino.getGdriveFolderId().isBlank()) {
            try {
                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(accessToken);
                restTemplate.exchange(
                        FILES_URL + "/" + destino.getGdriveFolderId() + "?fields=id",
                        HttpMethod.GET, new HttpEntity<>(h), Map.class);
                log.info("Usando carpeta Drive existente id={}", destino.getGdriveFolderId());
                return destino.getGdriveFolderId();
            } catch (Exception e) {
                log.warn("Carpeta Drive id={} no encontrada, buscando por nombre...",
                        destino.getGdriveFolderId());
            }
        }

        // 2. Buscar por nombre exacto en Drive
        String query = "mimeType='application/vnd.google-apps.folder' and name='"
                + nombreCarpeta.replace("'", "\\'")
                + "' and trashed=false";

        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(FILES_URL)
                .queryParam("q", query)
                .queryParam("fields", "files(id,name)")
                .encode()
                .build()
                .toUri();

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);

        try {
            ResponseEntity<Map> search = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(h), Map.class);

            if (search.getBody() != null) {
                List<Map<String, Object>> files =
                        (List<Map<String, Object>>) search.getBody().get("files");
                if (files != null && !files.isEmpty()) {
                    String existingId = (String) files.get(0).get("id");
                    log.info("Carpeta '{}' encontrada en Drive id={}", nombreCarpeta, existingId);
                    guardarFolderId(destino, existingId);
                    return existingId;
                }
            }
        } catch (Exception e) {
            log.warn("Error buscando carpeta '{}' en Drive: {}", nombreCarpeta, e.getMessage());
        }

        // 3. No existe — crear carpeta nueva
        String folderMeta = String.format(
                "{\"name\":\"%s\",\"mimeType\":\"application/vnd.google-apps.folder\"}",
                nombreCarpeta);

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setBearerAuth(accessToken);
        createHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> created = restTemplate.exchange(
                FILES_URL, HttpMethod.POST,
                new HttpEntity<>(folderMeta, createHeaders), Map.class);

        if (created.getBody() == null) {
            throw new RuntimeException("Error creando carpeta en Google Drive");
        }

        String newId = (String) created.getBody().get("id");
        log.info("Carpeta '{}' creada en Drive id={}", nombreCarpeta, newId);
        guardarFolderId(destino, newId);
        return newId;
    }

    private void guardarFolderId(BackupDestination destino, String folderId) {
        try {
            destino.setGdriveFolderId(folderId);
            destinationRepo.save(destino);
        } catch (Exception e) {
            log.warn("No se pudo guardar folderId en BD: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> listarCarpetas(BackupDestination destino) {
        String accessToken = oauthService.obtenerAccessToken(destino.getGdriveRefreshTokenEnc());

        String query = "mimeType='application/vnd.google-apps.folder' and trashed=false";

        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromUriString(FILES_URL)
                .queryParam("q", query)
                .queryParam("fields", "files(id,name)")
                .queryParam("orderBy", "name")
                .queryParam("pageSize", "100")
                .encode()
                .build()
                .toUri();

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(h), Map.class);

        if (response.getBody() == null) return List.of();

        List<Map<String, Object>> files =
                (List<Map<String, Object>>) response.getBody().get("files");

        if (files == null) return List.of();

        return files.stream()
                .map(f -> Map.of(
                        "id",   String.valueOf(f.getOrDefault("id",   "")),
                        "name", String.valueOf(f.getOrDefault("name", ""))
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean probarConexion(BackupDestination destino) {
        try {
            String accessToken = oauthService.obtenerAccessToken(destino.getGdriveRefreshTokenEnc());
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(accessToken);
            restTemplate.exchange(
                    "https://www.googleapis.com/drive/v3/about?fields=user",
                    HttpMethod.GET, new HttpEntity<>(h), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Prueba Google Drive fallida: {}", e.getMessage());
            return false;
        }
    }
}