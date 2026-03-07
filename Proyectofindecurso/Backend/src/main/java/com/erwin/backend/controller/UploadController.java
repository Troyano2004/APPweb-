
package com.erwin.backend.controller;

import com.erwin.backend.dtos.ImageUploadResponseDto;
import com.erwin.backend.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    // ── Imágenes (existente) ─────────────────────────────────────────────────
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

    // ── PDFs / Archivos (nuevo — para documentos habilitantes) ───────────────
    @PostMapping("/files")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String azureUrl = imageStorageService.storeImage(file);
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