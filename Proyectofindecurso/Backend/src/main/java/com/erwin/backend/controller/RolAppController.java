package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.RolAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:4200","http://26.102.176.187:4200","http://26.122.106.219:4200"})
public class RolAppController {

    private final RolAppService service;

    public RolAppController(RolAppService service) {
        this.service = service;
    }

    @GetMapping("/rol-app")
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(service.listar());
        } catch (Exception e) {
            System.err.println("[ROLES ERROR] " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    @PostMapping("/rol-app")
    public ResponseEntity<RolAppDto> crear(@RequestBody RolAppCreateRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }

    @PutMapping("/rol-app/{id}")
    public ResponseEntity<RolAppDto> editar(@PathVariable Integer id, @RequestBody RolAppUpdateRequest req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    @PatchMapping("/rol-app/{id}/estado")
    public ResponseEntity<RolAppDto> cambiarEstado(@PathVariable Integer id, @RequestBody RolAppEstadoRequest req) {
        return ResponseEntity.ok(service.cambiarEstado(id, req));
    }

    @PostMapping("/rol-app/{id}/permisos")
    public ResponseEntity<RolAppDto> asignarPermisos(@PathVariable Integer id, @RequestBody RolAppPermisosRequest req) {
        return ResponseEntity.ok(service.asignarPermisos(id, req));
    }
}