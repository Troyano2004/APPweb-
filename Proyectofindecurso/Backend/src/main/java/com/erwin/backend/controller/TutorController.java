package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.Dt1TutoriasService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/director")
public class TutorController {

    private final Dt1TutoriasService service;

    public TutorController(Dt1TutoriasService service) {
        this.service = service;
    }

    // 1) Mis anteproyectos como DIRECTOR
    @GetMapping("/mis-anteproyectos/{idDocente}")
    public List<MisAnteproyectosDt1Response> misAnteproyectos(@PathVariable Integer idDocente) {
        return service.misAnteproyectos(idDocente);
    }

    // 2) Programar tutoría
    @PostMapping("/{idAnteproyecto}/tutorias/{idDocente}")
    public TutoriaResponse programarTutoria(
            @PathVariable Integer idAnteproyecto,
            @PathVariable Integer idDocente,
            @RequestBody TutoriaCreateRequest req
    ) {
        return service.programarTutoria(idAnteproyecto, idDocente, req);
    }

    // 3) Listar tutorías del anteproyecto
    @GetMapping("/{idAnteproyecto}/tutorias/{idDocente}")
    public List<TutoriaResponse> tutoriasPorAnteproyecto(
            @PathVariable Integer idAnteproyecto,
            @PathVariable Integer idDocente
    ) {
        return service.tutoriasPorAnteproyecto(idAnteproyecto, idDocente);
    }

    // 4) Cancelar tutoría
    @PostMapping("/tutorias/{idTutoria}/cancelar/{idDocente}")
    public TutoriaResponse cancelarTutoria(
            @PathVariable Integer idTutoria,
            @PathVariable Integer idDocente
    ) {
        return service.cancelarTutoria(idTutoria, idDocente);
    }

    // 5) Obtener acta (si existe)
    @GetMapping("/tutorias/{idTutoria}/acta/{idDocente}")
    public ActaRevisionTutorResponse obtenerActa(
            @PathVariable Integer idTutoria,
            @PathVariable Integer idDocente
    ) {
        return service.obtenerActa(idTutoria, idDocente);
    }

    // 6) Guardar / editar acta
    @PostMapping("/tutorias/{idTutoria}/acta/{idDocente}")
    public ActaRevisionTutorResponse guardarActa(
            @PathVariable Integer idTutoria,
            @PathVariable Integer idDocente,
            @RequestBody ActaRevisionTutorRequest req
    ) {
        return service.guardarActa(idTutoria, idDocente, req);
    }
}
