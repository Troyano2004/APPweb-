package com.erwin.backend.controller;

import com.erwin.backend.entities.RolSistema;
import com.erwin.backend.repository.RolesSistemaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los roles del sistema (tabla roles_sistema) para que el
 * frontend pueda mostrarlos en el select "Rol base" al crear/editar
 * un rol del aplicativo.
 *
 * FIX Error 2: sin este endpoint, el frontend no puede enviar
 * idRolBase al SP sp_crear_rol_app, lo que deja id_rol_base = NULL
 * en rol_app y rompe sp_crear_usuario_v3.
 */
@RestController
@RequestMapping("/roles-sistema")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class RolesSistemaController {

    private final RolesSistemaRepository repo;

    public RolesSistemaController(RolesSistemaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<RolSistema>> listar() {
        return ResponseEntity.ok(repo.findAll());
    }
}