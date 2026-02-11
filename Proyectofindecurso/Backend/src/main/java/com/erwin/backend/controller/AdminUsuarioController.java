package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.AdminUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminUsuarioController {

    private final AdminUsuarioService service;

    public AdminUsuarioController(AdminUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioAdminDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminDto> crear(@RequestBody UsuarioCreateRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAdminDto> editar(@PathVariable Integer id, @RequestBody UsuarioUpdateRequest req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioAdminDto> cambiarEstado(@PathVariable Integer id, @RequestBody UsuarioEstadoRequest req) {
        return ResponseEntity.ok(service.cambiarEstado(id, req));
    }
}
// hola bjghhhghfg