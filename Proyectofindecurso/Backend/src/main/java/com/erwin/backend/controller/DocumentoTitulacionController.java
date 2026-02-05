package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.DocumentoTitulacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/titulacion2")
public class DocumentoTitulacionController {

    private final DocumentoTitulacionService service;

    public DocumentoTitulacionController(DocumentoTitulacionService service) {
        this.service = service;
    }

    // ====== ESTUDIANTE ======
    @GetMapping("/estudiante/{idEstudiante}/documento")
    public DocumentoTitulacionDto miDocumento(@PathVariable Integer idEstudiante) {
        return service.obtenerOMiDocumento(idEstudiante);
    }

    @PutMapping("/estudiante/{idEstudiante}/documento")
    public DocumentoTitulacionDto guardar(@PathVariable Integer idEstudiante,
                                          @RequestBody DocumentoUpdateRequest req) {
        return service.guardarCambios(idEstudiante, req);
    }

    @PostMapping("/estudiante/{idEstudiante}/enviar-revision")
    public ResponseEntity<?> enviarRevision(@PathVariable Integer idEstudiante) {
        service.enviarRevision(idEstudiante);
        return ResponseEntity.ok().build();
    }

    // ====== OBSERVACIONES (visible para estudiante y director) ======
    @GetMapping("/documento/{idDocumento}/observaciones")
    public List<ObservacionDto> observaciones(@PathVariable Integer idDocumento) {
        return service.listarObservaciones(idDocumento);
    }

    // ====== DIRECTOR ======
    @GetMapping("/director/{idDocente}/pendientes")
    public List<DocumentoTitulacionDto> pendientes(@PathVariable Integer idDocente) {
        return service.listarPendientesDirector(idDocente);
    }

    @PostMapping("/director/{idDocente}/documento/{idDocumento}/observacion")
    public ObservacionDto agregarObs(@PathVariable Integer idDocente,
                                     @PathVariable Integer idDocumento,
                                     @RequestBody CrearObservacionRequest req) {
        return service.agregarObservacion(idDocente, idDocumento, req);
    }

    @PostMapping("/director/{idDocente}/documento/{idDocumento}/devolver")
    public ResponseEntity<?> devolver(@PathVariable Integer idDocente,
                                      @PathVariable Integer idDocumento) {
        service.devolverConObservaciones(idDocente, idDocumento);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/director/{idDocente}/documento/{idDocumento}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Integer idDocente,
                                     @PathVariable Integer idDocumento) {
        service.aprobar(idDocente, idDocumento);
        return ResponseEntity.ok().build();
    }
}
