package com.erwin.backend.controller;

import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@CrossOrigin(origins = "http://localhost:4200")
public class CatalogoController {
    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/carreras")
    public List<Carrera> carreras() {
        return service.carreras();
    }

    @GetMapping("/modalidades")
    public List<Modalidadtitulacion> modalidades() {
        return service.modalidades();
    }

    @GetMapping("/periodo-activo")
    public PeriodoTitulacion periodoActivo() {
        return service.periodoActivo();
    }

    @PostMapping("/carrera-modalidad")
    public void asignarModalidad(@RequestParam Integer idCarrera,
                                 @RequestParam Integer idModalidad) {
        service.asignarModalidad(idCarrera, idModalidad);
    }
}
