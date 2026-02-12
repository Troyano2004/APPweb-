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

    @PostMapping("/modalidades")
    public Modalidadtitulacion crearModalidad(@RequestBody CrearModalidadRequest req) {
        if (req == null) {
            throw new RuntimeException("Body requerido");
        }
        return service.crearModalidad(req.nombre);
    }

    @GetMapping("/periodo-activo")
    public PeriodoTitulacion periodoActivo() {
        return service.periodoActivo();
    }

    @GetMapping("/carrera-modalidad")
    public List<CatalogoService.CarreraModalidadDto> carreraModalidad() {
        return service.carreraModalidad();
    }

    @PostMapping("/carrera-modalidad")
    public void asignarModalidad(@RequestParam Integer idCarrera,
                                 @RequestParam Integer idModalidad) {
        service.asignarModalidad(idCarrera, idModalidad);
    }

    public static class CrearModalidadRequest {
        public String nombre;
    }
}
