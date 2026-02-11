package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.RolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping // opcional, puedes dejarlo vacío
@CrossOrigin(origins = "http://localhost:4200")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolDto>> listarRoles() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    @PostMapping("/roles")
    public ResponseEntity<RolDto> crear(@RequestBody RolCreateRequest req) {
        return ResponseEntity.ok(rolService.crear(req));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<RolDto> editar(@PathVariable Integer id, @RequestBody RolUpdateRequest req) {
        return ResponseEntity.ok(rolService.editar(id, req));
    }

    @PatchMapping("/roles/{id}/estado")
    public ResponseEntity<RolDto> cambiarEstado(@PathVariable Integer id, @RequestBody RolEstadoRequest req) {
        return ResponseEntity.ok(rolService.cambiarEstado(id, req));
    }

    @PostMapping("/roles/{id}/permisos")
    public ResponseEntity<RolDto> asignarPermisos(@PathVariable Integer id, @RequestBody RolPermisosRequest req) {
        return ResponseEntity.ok(rolService.asignarPermisos(id, req));
    }

    @GetMapping("/permisos")
    public ResponseEntity<List<PermisoDto>> listarPermisos() {
        return ResponseEntity.ok(rolService.listarPermisos());
    }
}