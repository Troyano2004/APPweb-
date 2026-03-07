package com.erwin.backend.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
/**
 * @deprecated Este controlador ha sido reemplazado por {@link Dt2Controller} en /api/dt2.
 * La lógica de ponderación 50/50 era incorrecta.
 * Usar los nuevos endpoints: /api/dt2/proyectos/{id}/...
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/titulacion2/workflow")
@Deprecated
public class TitulacionWorkflowController {
    private static final Map<String, String> DEPRECATED = Map.of(
            "error", "DEPRECATED",
            "mensaje", "Este endpoint ha sido reemplazado. Use /api/dt2/proyectos/{idProyecto}/... para el workflow de Titulación II.",
            "nuevosEndpoints", "/api/dt2"
    );
    @PostMapping("/documento/{idDocumento}/listo-para-tribunal")
    public ResponseEntity<Map<String, String>> listoParaTribunal(@PathVariable Integer idDocumento,
                                                                 @RequestBody Object req) {
        return ResponseEntity.status(HttpStatus.GONE).body(DEPRECATED);
    }
    @PostMapping("/documento/{idDocumento}/asignar-tribunal")
    public ResponseEntity<Map<String, String>> asignarTribunal(@PathVariable Integer idDocumento,
                                                               @RequestBody Object req) {
        return ResponseEntity.status(HttpStatus.GONE).body(DEPRECATED);
    }
    @PostMapping("/documento/{idDocumento}/agendar-sustentacion")
    public ResponseEntity<Map<String, String>> agendar(@PathVariable Integer idDocumento,
                                                       @RequestBody Object req) {
        return ResponseEntity.status(HttpStatus.GONE).body(DEPRECATED);
    }
    @PostMapping("/documento/{idDocumento}/registrar-resultado")
    public ResponseEntity<Map<String, String>> registrarResultado(@PathVariable Integer idDocumento,
                                                                  @RequestBody Object req) {
        return ResponseEntity.status(HttpStatus.GONE).body(DEPRECATED);
    }
    @PostMapping("/documento/{idDocumento}/cerrar")
    public ResponseEntity<Map<String, String>> cerrar(@PathVariable Integer idDocumento,
                                                      @RequestBody Object req) {
        return ResponseEntity.status(HttpStatus.GONE).body(DEPRECATED);
    }
}