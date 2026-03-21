package com.erwin.backend.controller;

import com.erwin.backend.dtos.EstudianteCarreraResponse;
import com.erwin.backend.service.GestionEstudiantesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinador/gestion-estudiantes")
@CrossOrigin(origins = "http://localhost:4200")
public class GestionEstudiantesController {

    private final GestionEstudiantesService service;

    public GestionEstudiantesController(GestionEstudiantesService service) {
        this.service = service;
    }

    @GetMapping("/{idUsuario}")
    public List<EstudianteCarreraResponse> listar(@PathVariable Integer idUsuario) {
        return service.listarEstudiantes(idUsuario);
    }
}