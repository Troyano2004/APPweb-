package com.erwin.backend.controller;

import com.erwin.backend.dtos.ZoomConfigDto;
import com.erwin.backend.service.ZoomConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docente/zoom-config")
@CrossOrigin(origins = "http://localhost:4200")
public class ZoomConfigController {
    private final ZoomConfigService service;

    public ZoomConfigController(ZoomConfigService service) {
        this.service = service;
    }
    @GetMapping("/{idDocente}")
    public ZoomConfigDto obtener(@PathVariable Integer idDocente) {
        return service.obtener(idDocente);
    }
    @PostMapping("/{idDocente}")
    public ZoomConfigDto guardar(@PathVariable Integer idDocente, @RequestBody ZoomConfigDto req) {
        return service.guardar(idDocente, req);
    }
    @DeleteMapping("/{idDocente}")
    public void eliminar(@PathVariable Integer idDocente) {
        service.eliminar(idDocente);
    }
}
