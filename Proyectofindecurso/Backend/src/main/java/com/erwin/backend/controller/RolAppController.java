
package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.RolAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class RolAppController {

    private final RolAppService service;

    public RolAppController(RolAppService service) {
        this.service = service;
    }

    // ✅ Lista roles del aplicativo (con idRolBase y rolBd incluidos)
    @GetMapping("/rol-app")
    public ResponseEntity<List<RolAppDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // ✅ NUEVO: Lista roles físicos de la BD (pg_roles) dinámicamente
    // El frontend los muestra en la pantalla de Roles como venían del video
    @GetMapping("/roles-bd")
    public ResponseEntity<List<RolBdDto>> listarRolesBd() {
        return ResponseEntity.ok(service.listarRolesBd());
    }

    // ✅ Crear rol app (acepta idRolBase en el body)
    @PostMapping("/rol-app")
    public ResponseEntity<RolAppDto> crear(@RequestBody RolAppCreateRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }

    // ✅ Editar (acepta idRolBase en el body via RolAppUpdateRequestExtended)
    @PutMapping("/rol-app/{id}")
    public ResponseEntity<RolAppDto> editar(@PathVariable Integer id,
                                            @RequestBody RolAppUpdateRequestExtended req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    @PatchMapping("/rol-app/{id}/estado")
    public ResponseEntity<RolAppDto> cambiarEstado(@PathVariable Integer id,
                                                   @RequestBody RolAppEstadoRequest req) {
        return ResponseEntity.ok(service.cambiarEstado(id, req));
    }

    @PostMapping("/rol-app/{id}/permisos")
    public ResponseEntity<RolAppDto> asignarPermisos(@PathVariable Integer id,
                                                     @RequestBody RolAppPermisosRequest req) {
        return ResponseEntity.ok(service.asignarPermisos(id, req));
    }
}