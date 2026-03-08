package com.erwin.backend.controller;

import com.erwin.backend.dtos.DocumentoHabilitanteDtos;
import com.erwin.backend.service.DocumentoHabilitanteService;
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

    private final DocumentoHabilitanteService service;

    public DocumentoHabilitanteController(DocumentoHabilitanteService service) {
        this.service = service;
    }

    // ── ESTUDIANTE ────────────────────────────────────────────────────────────

    /**
     * Devuelve el resumen completo de documentos habilitantes del estudiante.
     * Incluye los 7 tipos del reglamento, aunque no hayan sido subidos aún (estado PENDIENTE).
     */
    @GetMapping("/estudiante/{idEstudiante}/resumen")
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto resumenEstudiante(
            @PathVariable Integer idEstudiante) {
        return service.obtenerResumenPorEstudiante(idEstudiante);
    }

    /**
     * Sube (o reemplaza) un documento habilitante.
     * El archivo ya debe estar subido a Azure a través de /api/uploads/files;
     * aquí se registra la URL resultante junto con los metadatos del documento.
     */
    @PostMapping("/estudiante/{idEstudiante}/subir")
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumento(
            @PathVariable Integer idEstudiante,
            @RequestBody DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {
        return service.subirDocumento(idEstudiante, req);
    }

    // ── DIRECTOR / COORDINADOR ────────────────────────────────────────────────

    /**
     * Resumen de habilitantes de un proyecto específico.
     * Usado por el Director, Coordinador o Admin.
     */
    @GetMapping("/proyecto/{idProyecto}/resumen")
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto resumenProyecto(
            @PathVariable Integer idProyecto) {
        return service.obtenerResumenPorProyecto(idProyecto);
    }

    /**
     * Documentos en estado ENVIADO asignados al director para validar.
     */
    @GetMapping("/director/{idDocente}/pendientes")
    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesDirector(
            @PathVariable Integer idDocente) {
        return service.pendientesPorDirector(idDocente);
    }

    /**
     * El Director aprueba o rechaza un documento habilitante.
     * Body: { "decision": "APROBADO" | "RECHAZADO", "comentario": "..." }
     */
    @PostMapping("/director/{idDocente}/validar/{idHabilitante}")
    public ResponseEntity<DocumentoHabilitanteDtos.HabilitanteDto> validar(
            @PathVariable Integer idDocente,
            @PathVariable Integer idHabilitante,
            @RequestBody DocumentoHabilitanteDtos.ValidarHabilitanteRequest req) {
        return ResponseEntity.ok(service.validarDocumento(idDocente, idHabilitante, req));
    }
}