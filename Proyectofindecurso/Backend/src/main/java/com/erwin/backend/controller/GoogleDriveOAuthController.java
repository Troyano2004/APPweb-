package com.erwin.backend.controller;

import com.erwin.backend.service.GoogleDriveOAuthService;
import com.erwin.backend.service.GoogleDriveUploadService;
import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.repository.BackupDestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup/oauth")
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveOAuthController {

    private final GoogleDriveOAuthService      oauthService;
    private final GoogleDriveUploadService     uploadService;
    private final BackupDestinationRepository  destinationRepo;

    // ── Iniciar flujo OAuth — genera URL y redirige ────────────────────────────

    @GetMapping("/google/init/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> iniciarOAuth(
            @PathVariable Long destinationId) {
        String url = oauthService.generarUrlAutorizacion(destinationId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ── Callback de Google — recibe el código y cierra la ventana ─────────────

    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        try {
            Long destinationId = Long.parseLong(state);
            GoogleDriveOAuthService.TokenResult tokens = oauthService.intercambiarCodigo(code);
            oauthService.guardarTokenEnDestino(destinationId, tokens);

            // Cerrar ventana popup y notificar al frontend
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("""
                <html><body>
                <script>
                  window.opener && window.opener.postMessage(
                    { type: 'GDRIVE_CONNECTED', email: '%s' }, '*'
                  );
                  window.close();
                </script>
                <p>✅ Cuenta conectada correctamente. Puedes cerrar esta ventana.</p>
                </body></html>
                """.formatted(tokens.email()));

        } catch (Exception e) {
            log.error("Error en callback OAuth Google Drive", e);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("""
                <html><body>
                <script>
                  window.opener && window.opener.postMessage(
                    { type: 'GDRIVE_ERROR', mensaje: '%s' }, '*'
                  );
                  window.close();
                </script>
                <p>❌ Error: %s</p>
                </body></html>
                """.formatted(e.getMessage(), e.getMessage()));
        }
    }

    // ── Estado de conexión ─────────────────────────────────────────────────────

    @GetMapping("/google/status/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> estadoConexion(
            @PathVariable Long destinationId) {
        return destinationRepo.findById(destinationId)
                .map(dest -> ResponseEntity.ok(Map.<String, Object>of(
                        "conectado", oauthService.estaConectado(dest),
                        "email",     dest.getGdriveCuenta() != null ? dest.getGdriveCuenta() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Desconectar cuenta ─────────────────────────────────────────────────────

    @DeleteMapping("/google/disconnect/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desconectar(@PathVariable Long destinationId) {
        destinationRepo.findById(destinationId).ifPresent(dest -> {
            dest.setGdriveRefreshTokenEnc(null);
            dest.setGdriveCuenta(null);
            destinationRepo.save(dest);
        });
        return ResponseEntity.noContent().build();
    }

    // ── Listar carpetas del Drive ──────────────────────────────────────────────

    @GetMapping("/google/folders/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listarCarpetas(
            @PathVariable Long destinationId) {
        return destinationRepo.findById(destinationId)
                .map(dest -> {
                    try {
                        List<Map<String, String>> carpetas = uploadService.listarCarpetas(dest);
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "exitoso", true, "carpetas", carpetas));
                    } catch (Exception e) {
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "exitoso", false, "mensaje", e.getMessage(), "carpetas", List.of()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/google/test/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> probarConexion(
            @PathVariable Long destinationId) {
        return destinationRepo.findById(destinationId)
                .map(dest -> {
                    boolean ok = uploadService.probarConexion(dest);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "exitoso", ok,
                            "mensaje", ok ? "Conexión con Google Drive exitosa" : "No se pudo conectar"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}