package com.erwin.backend.controller;

import com.erwin.backend.dtos.SesionActivaDto;
import com.erwin.backend.service.SesionActivaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sesiones")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class SesionActivaController {

    private final SesionActivaService service;

    public SesionActivaController(SesionActivaService service) {
        this.service = service;
    }

    @GetMapping
    public List<SesionActivaDto> listarActivas() {
        return service.listarActivas();
    }

    @DeleteMapping("/{id}")
    public SesionActivaDto cerrarSesion(@PathVariable Integer id) {
        return service.cerrarSesion(id);
    }
}