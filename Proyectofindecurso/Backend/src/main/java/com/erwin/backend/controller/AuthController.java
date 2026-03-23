package com.erwin.backend.controller;

import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.service.AuditService;
import com.erwin.backend.audit.service.SesionActivaRegistry;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.security.JwtService;
import com.erwin.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class AuthController {

    private final AuthService          authService;
    private final SesionActivaRegistry sesionRegistry;
    private final AuditService         auditService;
    private final JwtService           jwtService;

    public AuthController(AuthService authService,
                          SesionActivaRegistry sesionRegistry,
                          AuditService auditService,
                          JwtService jwtService) {
        this.authService    = authService;
        this.sesionRegistry = sesionRegistry;
        this.auditService   = auditService;
        this.jwtService     = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpSession session,
                               HttpServletRequest request) {
        return authService.login(req, session, request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session,
                                       HttpServletRequest request) {
        try {
            // Obtener username desde el JWT del header Authorization
            String username = null;
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    username = jwtService.extractUsername(authHeader.substring(7));
                } catch (Exception ignored) {}
            }

            // Fallback: desde SecurityContext
            if (username == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }
            }

            // Registrar LOGOUT en auditoría y limpiar registry
            if (username != null) {
                System.out.println("[LOGOUT] username=" + username +
                        " | auditService=" + (auditService != null ? "OK" : "NULL"));

                auditService.registrar(AuditEventDto.builder()
                        .entidad("Login")
                        .accion("LOGOUT")
                        .entidadId(null)
                        .username(username)
                        .idUsuario(null)
                        .correoUsuario(null)
                        .ipAddress(AuditService.extractIp(request))
                        .estadoAnterior(null)
                        .estadoNuevo(null)
                        .metadata(null)
                        .build());

                System.out.println("[LOGOUT] auditService.registrar() llamado para: " + username);

                // Cierre voluntario: elimina la sesión del registry por username
                sesionRegistry.cerrarSesionesPorUsername(username, null);
            }

            if (session != null) {
                try { session.invalidate(); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("[Logout] Error: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
