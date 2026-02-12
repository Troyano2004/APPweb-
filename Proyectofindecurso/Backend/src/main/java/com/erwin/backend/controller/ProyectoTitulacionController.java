// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/controller/ProyectoTitulacionController.java
package com.erwin.backend.controller;

import com.erwin.backend.dtos.ProyectoTitulacionCreateRequest;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.service.ProyectoTitulacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos-titulacion")
public class ProyectoTitulacionController {

    private final ProyectoTitulacionService proyectoService;

    public ProyectoTitulacionController(ProyectoTitulacionService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProyectoTitulacionCreateRequest req) {
        try {
            ProyectoTitulacion creado = proyectoService.crearProyecto(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
