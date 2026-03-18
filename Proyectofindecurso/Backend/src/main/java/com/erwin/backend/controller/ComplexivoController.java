package com.erwin.backend.controller;

import com.erwin.backend.dtos.ComplexivoDtos.*;
import com.erwin.backend.service.ComplexivoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complexivo")
@CrossOrigin(origins = "http://localhost:4200")
public class ComplexivoController {

    private final ComplexivoService service;

    public ComplexivoController(ComplexivoService service) {
        this.service = service;
    }

    // ── Coordinador ──────────────────────────────────────────────────
    @GetMapping("/coordinador/info")
    public InfoCoordinadorComplexivoDto infoCoordinador(@RequestParam Integer idUsuario) {
        return service.infoCoordinador(idUsuario);
    }

    @PostMapping("/coordinador/asignar-docente")
    public ComplexivoDocenteAsignacionResponse asignarDocente(
            @RequestBody AsignarDocenteComplexivoRequest req) {
        return service.asignarDocente(req);
    }

    // ── Estudiante ───────────────────────────────────────────────────
    @GetMapping("/estudiante/{idEstudiante}/estado")
    public EstadoComplexivoEstudianteDto estadoEstudiante(@PathVariable Integer idEstudiante) {
        return service.estadoEstudiante(idEstudiante);
    }

    @GetMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto getInforme(@PathVariable Integer idEstudiante) {
        return service.getInforme(idEstudiante);
    }

    @PutMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto guardarInforme(@PathVariable Integer idEstudiante,
                                               @RequestBody ComplexivoInformeUpdateRequest req) {
        return service.guardarInforme(idEstudiante, req);
    }

    @PostMapping("/estudiante/{idEstudiante}/informe/enviar")
    public ComplexivoInformeDto enviarInforme(@PathVariable Integer idEstudiante) {
        return service.enviarInforme(idEstudiante);
    }




}