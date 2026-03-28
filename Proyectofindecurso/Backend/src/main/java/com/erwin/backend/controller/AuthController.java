package com.erwin.backend.controller;

import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.service.AuditService;
import com.erwin.backend.audit.service.SesionActivaRegistry;
import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.security.JwtService;
import com.erwin.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @PostMapping(value = "/logout",
                 consumes = {MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE,
                             MediaType.ALL_VALUE})
    public ResponseEntity<Void> logout(HttpSession session,
                                       HttpServletRequest request) {
        System.out.println("=== LOGOUT ENDPOINT ALCANZADO ===");
        try {
            String username = null;

            // PRIMERO: buscar en los atributos de la sesión actual
            if (session != null) {
                Object dbUser = session.getAttribute(DbSessionFilter.SES_DB_USER);
                System.out.println("[LOGOUT] SES_DB_USER en sesion: " + dbUser);

                java.util.Enumeration<String> attrs = session.getAttributeNames();
                while (attrs.hasMoreElements()) {
                    String attr = attrs.nextElement();
                    System.out.println("[LOGOUT] Atributo sesion: " + attr
                            + " = " + session.getAttribute(attr));
                }
            }

            // SEGUNDO: desde el JWT
            String authHeader = request.getHeader("Authorization");
            System.out.println("[LOGOUT] Auth header: " + authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    username = jwtService.extractUsername(authHeader.substring(7));
                    System.out.println("[LOGOUT] Username del JWT: " + username);
                } catch (Exception e) {
                    System.out.println("[LOGOUT] JWT error: " + e.getMessage());
                }
            }

            // TERCERO: desde SecurityContext
            if (username == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("[LOGOUT] Auth en SecurityContext: " + auth);
                if (auth != null && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }
            }

            System.out.println("[LOGOUT] Username final: " + username);

            if (username != null) {
                System.out.println("[LOGOUT] Intentando registrar auditoria para: " + username);
                try {
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
                    System.out.println("[LOGOUT] registrar() llamado exitosamente");
                } catch (Exception e) {
                    System.err.println("[LOGOUT] Error llamando registrar(): " + e.getMessage());
                    e.printStackTrace();
                }

                sesionRegistry.cerrarSesionesPorUsername(username);
            }

            if (session != null) {
                try { session.invalidate(); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("[LOGOUT] Error: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/logout-beacon",
                 consumes = {MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE,
                             MediaType.ALL_VALUE})
    public ResponseEntity<Void> logoutBeacon(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        try {
            String username = body != null ? body.get("username") : null;
            System.out.println("[LOGOUT-BEACON] username=" + username);

            if (username != null && !username.isBlank()) {
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
                sesionRegistry.cerrarSesionesPorUsername(username);
                System.out.println("[LOGOUT-BEACON] Sesion cerrada para: " + username);
            }
        } catch (Exception e) {
            System.err.println("[LOGOUT-BEACON] Error: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(HttpServletRequest request,
                                          Authentication authentication) {
        if (authentication != null && !"anonymousUser".equals(authentication.getName())) {
            sesionRegistry.actualizarActividadPorUsername(authentication.getName());
        } else {
            // Fallback: extraer username del JWT si Spring no lo resolvió
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String username = jwtService.extractUsername(authHeader.substring(7));
                    if (username != null) {
                        sesionRegistry.actualizarActividadPorUsername(username);
                    }
                } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok().build();
    }
}
