package com.erwin.backend.controller;

import com.erwin.backend.dtos.RestoreRequest;
import com.erwin.backend.dtos.RestoreResponse;
import com.erwin.backend.dtos.RestoreResultado;
import com.erwin.backend.service.BackupRestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backup/restaurar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupRestoreController {

    private final BackupRestoreService restoreService;

    @GetMapping("/historial/{jobId}")
    public ResponseEntity<List<RestoreResponse>> historial(@PathVariable Long jobId) {
        return ResponseEntity.ok(restoreService.obtenerHistorialConDisponibilidad(jobId));
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<RestoreResultado> restaurar(@RequestBody RestoreRequest req) {
        return ResponseEntity.ok(restoreService.restaurar(req));
    }
}