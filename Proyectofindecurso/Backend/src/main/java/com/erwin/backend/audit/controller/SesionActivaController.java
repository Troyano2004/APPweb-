package com.erwin.backend.audit.controller;

import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.service.AuditService;
import com.erwin.backend.audit.service.SesionActivaRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria/sesiones")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class SesionActivaController {

    private final SesionActivaRegistry sesionRegistry;
    private final AuditService auditService;

    public SesionActivaController(SesionActivaRegistry sesionRegistry,
                                  AuditService auditService) {
        this.sesionRegistry = sesionRegistry;
        this.auditService   = auditService;
    }

    @GetMapping
    public List<SesionActivaRegistry.SesionActivaInfo> sesionesActivas() {
        return sesionRegistry.getSesionesActivas();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> cerrarSesion(
            @PathVariable String sessionId,
            HttpSession sessionActual,
            Authentication authentication,
            HttpServletRequest request) {

        if (sessionId.equals(sessionActual.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No puedes cerrar tu propia sesion"));
        }

        String adminUsername = authentication != null
                ? authentication.getName() : "administrador";

        // Obtener datos de la sesión ANTES de marcarla (marcar la elimina del mapa)
        SesionActivaRegistry.SesionActivaInfo infoSesion = sesionRegistry
                .getSesionesActivas().stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst().orElse(null);

        String usuarioCerrado = infoSesion != null
                ? infoSesion.getUsername() : "desconocido";

        sesionRegistry.marcarParaCerrar(sessionId, adminUsername);

        try {
            auditService.registrar(AuditEventDto.builder()
                    .entidad("SesionActiva")
                    .accion("CERRAR_SESION_REMOTA")
                    .username(adminUsername)
                    .idUsuario(null)
                    .ipAddress(AuditService.extractIp(request))
                    .metadata(Map.of(
                            "usuarioCerrado", usuarioCerrado,
                            "sessionId", sessionId,
                            "motivo", "Cierre remoto por administrador"
                    ))
                    .build());
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of(
                "mensaje", "Sesion cerrada por " + adminUsername
        ));
    }

    @DeleteMapping("/usuario/{username}")
    public ResponseEntity<Map<String, Object>> cerrarTodasSesiones(
            @PathVariable String username,
            HttpSession sessionActual,
            Authentication authentication,
            HttpServletRequest request) {

        String adminUsername = authentication != null
                ? authentication.getName() : "administrador";

        sesionRegistry.cerrarSesionesPorUsername(username, adminUsername);

        try {
            auditService.registrar(AuditEventDto.builder()
                    .entidad("SesionActiva")
                    .accion("CERRAR_SESION_REMOTA")
                    .username(adminUsername)
                    .idUsuario(null)
                    .ipAddress(AuditService.extractIp(request))
                    .metadata(Map.of(
                            "usuarioCerrado", username,
                            "motivo", "Cierre remoto de todas las sesiones por administrador"
                    ))
                    .build());
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of(
                "mensaje", "Sesiones cerradas para " + username,
                "cerradaPor", adminUsername
        ));
    }
}
