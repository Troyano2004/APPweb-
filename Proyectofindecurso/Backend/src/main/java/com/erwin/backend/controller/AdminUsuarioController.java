
package com.erwin.backend.controller;

import com.erwin.backend.dtos.UsuarioAdminDto;
import com.erwin.backend.dtos.UsuarioCreateRequest;
import com.erwin.backend.dtos.UsuarioEstadoRequest;
import com.erwin.backend.dtos.UsuarioUpdateRequest;
import com.erwin.backend.service.AdminUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/usuarios")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class AdminUsuarioController {

    private final AdminUsuarioService service;

    public AdminUsuarioController(AdminUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioAdminDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // ── FIX Error 1: capturar excepciones del SP y retornar HTTP correcto ──
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioCreateRequest req) {
        try {
            return ResponseEntity.ok(service.crear(req));
        } catch (RuntimeException e) {
            return mapearError(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id,
                                    @RequestBody UsuarioUpdateRequest req) {
        try {
            return ResponseEntity.ok(service.editar(id, req));
        } catch (RuntimeException e) {
            return mapearError(e);
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id,
                                           @RequestBody UsuarioEstadoRequest req) {
        try {
            return ResponseEntity.ok(service.cambiarEstado(id, req));
        } catch (RuntimeException e) {
            return mapearError(e);
        }
    }

    // ── Mapear mensaje del SP a código HTTP adecuado ──────────────────────
    private ResponseEntity<Map<String, String>> mapearError(RuntimeException e) {
        String msg = extraerMensaje(e);

        // Usuario duplicado → 409 Conflict
        if (msg.contains("Ya existe un usuario") || msg.contains("El usuario ya existe")
                || msg.contains("ya existe un usuario")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", msg));
        }

        // Datos de negocio inválidos → 422 Unprocessable Entity
        if (msg.contains("id_rol_base") || msg.contains("rol_app no existen")
                || msg.contains("Debe enviar al menos un rol")
                || msg.contains("username es obligatorio")
                || msg.contains("No existe usuario con id")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", msg));
        }

        // Error genérico → 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg));
    }

    /** Extrae el mensaje útil aunque venga envuelto en capas de excepción */
    private String extraerMensaje(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && !msg.isBlank()) {
                // El mensaje del SP viene como: "ERROR: <texto> Where: ..."
                // Extraer solo la parte útil
                int idx = msg.indexOf("ERROR:");
                if (idx >= 0) {
                    String sub = msg.substring(idx + 6).trim();
                    int end = sub.indexOf("\n");
                    return end > 0 ? sub.substring(0, end).trim() : sub.trim();
                }
                return msg;
            }
            t = t.getCause();
        }
        return "Error interno del servidor";
    }
}