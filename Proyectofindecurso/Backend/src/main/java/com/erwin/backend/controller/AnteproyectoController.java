package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.AnteproyectoService;
import com.erwin.backend.service.Dt1Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/anteproyectos")
public class AnteproyectoController {
    private final Dt1Service  dt1Service;
    private final AnteproyectoService service;

    public AnteproyectoController(AnteproyectoService service, Dt1Service  dt1Service) {
        this.service = service;
        this.dt1Service = dt1Service;
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
    @GetMapping("/{idAnteproyecto}/ultima-revision")
    public Dt1UltimaRevisionResponse ultimaRevision(@PathVariable Integer idAnteproyecto) {
        return dt1Service.ultimaRevision(idAnteproyecto);
    }
}