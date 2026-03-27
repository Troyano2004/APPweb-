
package com.erwin.backend.controller;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.ImageUploadResponseDto;
import com.erwin.backend.service.FileStorageService;
import com.erwin.backend.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService imageStorageService;
    private final FileStorageService  fileStorageService;

    public UploadController(ImageStorageService imageStorageService,
                            FileStorageService  fileStorageService) {
        this.imageStorageService = imageStorageService;
        this.fileStorageService  = fileStorageService;
    }

    // ── Imágenes (existente sin cambios) ────────────────────────────────────
    @Auditable(entidad = "ImagenSubida", accion = "UPLOAD", capturarArgs = false)
    @PostMapping("/images")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String azureUrl = imageStorageService.storeImage(file);
            return ResponseEntity.ok(new ImageUploadResponseDto(azureUrl, azureUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── PDFs / Documentos habilitantes ──────────────────────────────────────
    @Auditable(entidad = "ArchivoSubido", accion = "UPLOAD", capturarArgs = false)
    @PostMapping("/files")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String azureUrl = fileStorageService.storeFile(file);
            return ResponseEntity.ok(Map.of(
                    "url",      azureUrl,
                    "filename", file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : "documento.pdf"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}