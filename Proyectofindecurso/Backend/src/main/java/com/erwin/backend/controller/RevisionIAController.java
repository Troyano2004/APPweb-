package com.erwin.backend.controller;

import com.erwin.backend.entities.DocumentoTitulacion;
import com.erwin.backend.dtos.RevisionIARequest;
import com.erwin.backend.service.RevisionIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revision-ia")
@CrossOrigin(origins = "*") // Permite que Angular se conecte sin errores de CORS
public class RevisionIAController {

    @Autowired
    private RevisionIAService revisionIAService;

    @PostMapping("/evaluar/{idDocumento}")
    public ResponseEntity<DocumentoTitulacion> evaluarDocumento(@PathVariable Integer idDocumento,
                                                                @RequestBody(required = false) RevisionIARequest request) {
        try {
            // Llama al servicio de Gemini que creamos antes
            DocumentoTitulacion docActualizado = revisionIAService.evaluarTituloYObjetivos(idDocumento, request);
            return ResponseEntity.ok(docActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}