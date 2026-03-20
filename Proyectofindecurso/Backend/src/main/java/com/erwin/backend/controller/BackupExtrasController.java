package com.erwin.backend.controller;

import com.erwin.backend.dtos.BackupStatsDto;
import com.erwin.backend.dtos.IntegrityResultDto;
import com.erwin.backend.repository.BackupJobRepository;
import com.erwin.backend.service.BackupStatsService;
import com.erwin.backend.service.BackupSseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupExtrasController {

    private final BackupStatsService          statsService;
    private final BackupSseNotificationService sseService;
    private final BackupJobRepository          jobRepo;

    // ── Estadísticas para el dashboard ────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupStatsDto> estadisticas() {
        return ResponseEntity.ok(statsService.obtenerEstadisticas());
    }

    // ── Verificación de integridad ────────────────────────────────────────────

    @PostMapping("/integridad/{idExecution}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IntegrityResultDto> verificarIntegridad(
            @PathVariable Long idExecution,
            @RequestParam Long idJob) {

        return jobRepo.findByIdWithDestinos(idJob)
                .map(job -> ResponseEntity.ok(statsService.verificarIntegridad(idExecution, job)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Retención manual (además del automático nocturno) ─────────────────────

    @PostMapping("/retencion/aplicar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> aplicarRetencionManual() {
        statsService.aplicarPoliticaRetencion();
        return ResponseEntity.ok().build();
    }

    // ── SSE — suscripción a notificaciones en tiempo real ─────────────────────

    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirEventos() {
        return sseService.suscribir();
    }
}