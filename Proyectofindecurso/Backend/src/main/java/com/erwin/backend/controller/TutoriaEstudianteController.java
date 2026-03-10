package com.erwin.backend.controller;


import com.erwin.backend.dtos.TutoriaHistorialResponse;
import com.erwin.backend.service.TutoriaEstudianteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estudiante/tutorias")
public class TutoriaEstudianteController {

    private final TutoriaEstudianteService service;

    public TutoriaEstudianteController(TutoriaEstudianteService service) {
        this.service = service;
    }

    @GetMapping("/historial/{idEstudiante}/{idAnteproyecto}")
    public List<TutoriaHistorialResponse> historial(
            @PathVariable Integer idEstudiante,
            @PathVariable Integer idAnteproyecto
    ) {
        return service.historial(idEstudiante, idAnteproyecto);
    }

}