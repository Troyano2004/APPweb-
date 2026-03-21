
package com.erwin.backend.controller;

import com.erwin.backend.dtos.DocumentoHabilitanteDtos;
import com.erwin.backend.service.DocumentoHabilitanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habilitantes")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentoHabilitanteController {

    private final DocumentoHabilitanteService service;

    public DocumentoHabilitanteController(DocumentoHabilitanteService service) {
        this.service = service;
    }

    // ── ESTUDIANTE TIC ─────────────────────────────────────────
    @GetMapping("/estudiante/{idEstudiante}/resumen")
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto resumenEstudiante(
            @PathVariable Integer idEstudiante) {
        return service.obtenerResumenPorEstudiante(idEstudiante);
    }

    @PostMapping("/estudiante/{idEstudiante}/subir")
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumento(
            @PathVariable Integer idEstudiante,
            @RequestBody DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {
        return service.subirDocumento(idEstudiante, req);
    }

    // ── ESTUDIANTE COMPLEXIVO ──────────────────────────────────
    @GetMapping("/estudiante/{idEstudiante}/resumen-complexivo")
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto resumenComplexivo(
            @PathVariable Integer idEstudiante) {
        return service.obtenerResumenComplexivo(idEstudiante);
    }

    @PostMapping("/estudiante/{idEstudiante}/subir-complexivo")
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumentoComplexivo(
            @PathVariable Integer idEstudiante,
            @RequestBody DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {
        return service.subirDocumentoComplexivo(idEstudiante, req);
    }

    // ── DIRECTOR / COORDINADOR ─────────────────────────────────
    @GetMapping("/proyecto/{idProyecto}/resumen")
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto resumenProyecto(
            @PathVariable Integer idProyecto) {
        return service.obtenerResumenPorProyecto(idProyecto);
    }

    @GetMapping("/director/{idDocente}/pendientes")
    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesDirector(
            @PathVariable Integer idDocente) {
        return service.pendientesPorDirector(idDocente);
    }

    @PostMapping("/director/{idDocente}/validar/{idHabilitante}")
    public ResponseEntity<DocumentoHabilitanteDtos.HabilitanteDto> validar(
            @PathVariable Integer idDocente,
            @PathVariable Integer idHabilitante,
            @RequestBody DocumentoHabilitanteDtos.ValidarHabilitanteRequest req) {
        return ResponseEntity.ok(
                service.validarDocumento(idDocente, idHabilitante, req));
    }
}