package com.erwin.backend.controller;

import com.erwin.backend.dtos.AnalizarRequest;
import com.erwin.backend.service.IaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class IaController {

    private final IaService service;

    public IaController(IaService service) {
        this.service = service;
    }

    @PostMapping("/analizar")
    public Map<String, String> analizar(@RequestBody AnalizarRequest request) {

        if (request.getSeccion() == null ||
                request.getContenido() == null ||
                request.getContenido().isBlank()) {

            return Map.of("resultado", "No hay contenido para analizar.");
        }

        String resultado = service.analizarSeccion(
                request.getSeccion(),
                request.getContenido(),
                request.getIdEstudiante()
        );

        return Map.of("resultado", resultado);
    }
}