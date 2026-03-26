package com.erwin.backend.controller;

import com.erwin.backend.dtos.RevisionPropuestaIARequest;
import com.erwin.backend.dtos.RevisionPropuestaIAResponse;
import com.erwin.backend.service.RevisionPropuestaIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador IA — Fase 1: Evaluación de Propuesta de Titulación.
 *
 * Endpoint: POST /api/revision-ia/propuesta/{idPropuesta}
 *
 * Separado intencionalmente de los otros controladores IA (Fases 3/4 y 5).
 * Usa la misma API Groq pero con prompts específicos para propuestas,
 * incluyendo el contexto de la carrera del estudiante.
 *
 * ─── Ruta del archivo ──────────────────────────────────────────────────────
 * src/main/java/com/erwin/backend/controller/RevisionPropuestaIAController.java
 *
 * ─── Quién puede llamar este endpoint ─────────────────────────────────────
 *   - El propio ESTUDIANTE: para revisar su propuesta antes de enviarla
 *   - El DOCENTE de la comisión: para tener una pre-evaluación automatizada
 *   - El COORDINADOR: para revisar el estado de propuestas en lote
 *
 * ─── Cómo se conecta al endpoint desde Angular ─────────────────────────────
 * Agrega este método en revision-director.ts (o crea un servicio nuevo):
 *
 *   evaluarPropuesta(idPropuesta: number, payload: any = {}): Observable<any> {
 *     return this.http.post(`http://localhost:8080/api/revision-ia/propuesta/${idPropuesta}`, payload);
 *   }
 */
@RestController
@RequestMapping("/api/revision-ia")
@CrossOrigin(origins = "*")
public class RevisionPropuestaIAController {

    @Autowired
    private RevisionPropuestaIAService service;

    /**
     * POST /api/revision-ia/propuesta/{idPropuesta}
     *
     * Body (JSON, todos opcionales):
     * {
     *   "modo": "integral" | "coherencia" | "pertinencia" | "viabilidad",
     *   "instruccionAdicional": "texto libre del docente o estudiante"
     * }
     *
     * Response: RevisionPropuestaIAResponse con feedbackIa en JSON string.
     * El frontend debe hacer JSON.parse(response.feedbackIa) para acceder
     * a los campos individuales (estado_evaluacion, puntaje_estimado, etc.)
     */
    @PostMapping("/propuesta/{idPropuesta}")
    public ResponseEntity<RevisionPropuestaIAResponse> evaluarPropuesta(
            @PathVariable Integer idPropuesta,
            @RequestBody(required = false) RevisionPropuestaIARequest request) {

        if (request == null) {
            request = new RevisionPropuestaIARequest();
        }

        try {
            RevisionPropuestaIAResponse response = service.evaluarPropuesta(idPropuesta, request);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 404: propuesta no encontrada
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}