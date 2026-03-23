package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.AnteproyectoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/anteproyectos")
public class AnteproyectoController {

    private final AnteproyectoService service;

    public AnteproyectoController(AnteproyectoService service) {
        this.service = service;
    }

    @GetMapping("/mi-anteproyecto/{idEstudiante}")
    public AnteproyectoResponse miAnteproyecto(@PathVariable Integer idEstudiante) {
        return service.cargarMiAnteproyecto(idEstudiante);
    }

    @GetMapping("/{idAnteproyecto}/versiones")
    public List<AnteproyectoVersionResponse> versiones(@PathVariable Integer idAnteproyecto) {
        return service.versiones(idAnteproyecto);
    }

    @GetMapping("/{idAnteproyecto}/ultima-version")
    public AnteproyectoVersionResponse ultimaVersion(@PathVariable Integer idAnteproyecto) {
        return service.ultimaVersion(idAnteproyecto);
    }

    @PostMapping("/{idAnteproyecto}/versiones/borrador")
    public AnteproyectoVersionResponse borrador(@PathVariable Integer idAnteproyecto,
                                                @RequestBody AnteproyectoVersionRequest req) {
        return service.guardarBorrador(idAnteproyecto, req);
    }

    @PostMapping("/{idAnteproyecto}/versiones/enviar")
    public AnteproyectoVersionResponse enviar(@PathVariable Integer idAnteproyecto,
                                              @RequestBody AnteproyectoVersionRequest req) {
        return service.enviarRevision(idAnteproyecto, req);
    }
}