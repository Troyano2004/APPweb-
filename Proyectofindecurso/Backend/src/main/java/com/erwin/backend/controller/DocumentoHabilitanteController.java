
package com.erwin.backend.controller;

import com.erwin.backend.dtos.DocumentoHabilitanteDtos;
import com.erwin.backend.service.DocumentosHabilitanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para el módulo de Documentos Habilitantes (DT2 - Sustentación).
 *
 * BASE: /api/habilitantes
 *
 * Estudiante:
 *   GET  /estudiante/{idEstudiante}/resumen          → resumen de sus habilitantes
 *   POST /estudiante/{idEstudiante}/subir             → subir/actualizar un documento
 *
 * Director / Coordinador:
 *   GET  /proyecto/{idProyecto}/resumen               → resumen por proyecto
 *   POST /director/{idDocente}/validar/{idHabilitante}→ aprobar / rechazar
 *   GET  /director/{idDocente}/pendientes             → documentos por validar
 */
@RestController
@RequestMapping("/api/habilitantes")
public class DocumentoHabilitanteController {

    private final DocumentosHabilitanteService service;

    public DocumentoHabilitanteController(DocumentosHabilitanteService service) {
        this.service = service;
    }

    // ── ESTUDIANTE ────────────────────────────────────────────────────────────

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

    // ── DIRECTOR / COORDINADOR ────────────────────────────────────────────────

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
        return ResponseEntity.ok(service.validarDocumento(idDocente, idHabilitante, req));
    }
}