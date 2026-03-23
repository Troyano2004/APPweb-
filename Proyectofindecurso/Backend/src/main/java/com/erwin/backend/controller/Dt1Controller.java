package com.erwin.backend.controller;

import com.erwin.backend.dtos.Dt1DetalleResponse;
import com.erwin.backend.dtos.Dt1EnviadoResponse;
import com.erwin.backend.dtos.Dt1RevisionRequest;
import com.erwin.backend.service.Dt1Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/dt1")
public class Dt1Controller {

    private final Dt1Service service;

    public Dt1Controller(Dt1Service service) {
        this.service = service;
    }

    @GetMapping("/lista/{idDocente}") // Cambiado de /bandeja a /enviados
    public List<Dt1EnviadoResponse> bandeja(@PathVariable Integer idDocente) {
        return service.enviados(idDocente);
    }

    @GetMapping("/detalle/{idAnteproyecto}/{idDocente}")
    public Dt1DetalleResponse detalle(
            @PathVariable Integer idAnteproyecto,
            @PathVariable Integer idDocente
    ) {
        return service.detalle(idAnteproyecto, idDocente);
    }

    @PostMapping("/revisar")
    public void revisar(@RequestBody Dt1RevisionRequest req) {
        service.revisar(req);
    }
    @GetMapping("/pdf/{idAnteproyecto}/{idDocente}")
    public ResponseEntity<byte[]> pdf(
            @PathVariable Integer idAnteproyecto,
            @PathVariable Integer idDocente
    ) {

        byte[] archivo = service.generarPdf(idAnteproyecto, idDocente);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=dt1.pdf")
                .body(archivo);
    }
}