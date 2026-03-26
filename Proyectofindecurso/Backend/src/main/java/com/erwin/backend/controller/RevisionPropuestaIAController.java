
package com.erwin.backend.controller;

import com.erwin.backend.dtos.RevisionPropuestaPreviaRequest;
import com.erwin.backend.dtos.RevisionPropuestaIARequest;
import com.erwin.backend.dtos.RevisionPropuestaIAResponse;
import com.erwin.backend.service.RevisionPropuestaPreviaService;
import com.erwin.backend.service.RevisionPropuestaIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador unificado para todos los endpoints IA del sistema.
 *
 * ─── Endpoints ─────────────────────────────────────────────────────────────
 *
 *  POST /api/revision-ia/propuesta/previa
 *    → Revisa el BORRADOR del formulario ANTES de guardar en BD.
 *    → Requiere: idEstudiante + todos los campos del form en el body.
 *    → NO guarda nada. Solo analiza y devuelve feedback.
 *    → Usado por: propuesta-nueva.component.ts (botón "Analizar con IA")
 *
 *  POST /api/revision-ia/propuesta/{idPropuesta}
 *    → Revisa una propuesta YA GUARDADA en BD.
 *    → Requiere: idPropuesta en la URL.
 *    → Usado por: docentes/coordinadores en revisión posterior.
 *
 * ─── Ruta del archivo ──────────────────────────────────────────────────────
 *  src/main/java/com/erwin/backend/controller/RevisionPropuestaIAController.java
 */
@RestController
@RequestMapping("/api/revision-ia")
@CrossOrigin(origins = "*")
public class RevisionPropuestaIAController {

    @Autowired
    private RevisionPropuestaIAService service;

    @Autowired
    private RevisionPropuestaPreviaService previaService;

    // ─── ENDPOINT 1: Revisión PREVIA (sin guardar en BD) ─────────────────

    /**
     * POST /api/revision-ia/propuesta/previa
     *
     * Body (JSON):
     * {
     *   "idEstudiante": 123,
     *   "titulo": "...",
     *   "temaInvestigacion": "...",
     *   "planteamientoProblema": "...",
     *   "objetivosGenerales": "...",
     *   "objetivosEspecificos": "...",
     *   "marcoTeorico": "...",
     *   "metodologia": "...",
     *   "resultadosEsperados": "...",
     *   "bibliografia": "...",
     *   "modo": "integral",
     *   "instruccionAdicional": "..."
     * }
     *
     * IMPORTANTE: La ruta "/previa" debe declararse ANTES de "/{idPropuesta}"
     * para que Spring no intente convertir "previa" en un Integer.
     */
    @PostMapping("/propuesta/previa")
    public ResponseEntity<RevisionPropuestaIAResponse> evaluarPropuestaPrevia(
            @RequestBody RevisionPropuestaPreviaRequest request) {

        try {
            RevisionPropuestaIAResponse response = previaService.evaluarPropuestaPrevia(request);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── ENDPOINT 2: Revisión de propuesta YA GUARDADA ───────────────────

    /**
     * POST /api/revision-ia/propuesta/{idPropuesta}
     *
     * Body (JSON, todos opcionales):
     * {
     *   "modo": "integral" | "coherencia" | "pertinencia" | "viabilidad",
     *   "instruccionAdicional": "texto libre"
     * }
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
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}