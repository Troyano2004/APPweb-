package com.erwin.backend.controller;

import com.erwin.backend.dtos.ReportePropuestaDto;
import com.erwin.backend.service.ReportePropuestaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reportes/propuestas")
public class ReportePropuestaController {

    private final ReportePropuestaService service;

    public ReportePropuestaController(ReportePropuestaService service) {
        this.service = service;
    }

    /**
     * GET /api/reportes/propuestas?estado=APROBADA
     * Devuelve JSON con resumen + listado completo.
     * El parámetro ?estado es opcional (sin él devuelve todas).
     */
    @GetMapping
    public ResponseEntity<ReportePropuestaDto.RespuestaCompleta> reporte(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.obtenerReporte(estado));
    }

    /**
     * GET /api/reportes/propuestas/pdf?estado=APROBADA
     * Devuelve el PDF directamente para visualizar/descargar.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) String estado) {
        byte[] archivo = service.generarPdf(estado);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=reporte-propuestas.pdf")
                .body(archivo);
    }
}