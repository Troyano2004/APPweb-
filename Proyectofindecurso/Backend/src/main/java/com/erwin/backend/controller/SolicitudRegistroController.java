package com.erwin.backend.controller;

import com.erwin.backend.dtos.SolicitudPendienteResponse;
import com.erwin.backend.dtos.SolicitudRegistroRequest;
import com.erwin.backend.dtos.SolicitudRegistroResponse;
import com.erwin.backend.dtos.VerificarCodigoRequest;
import com.erwin.backend.service.SolicitudRegistroService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/solicitud-registro")
public class SolicitudRegistroController {
    private final SolicitudRegistroService solicitudService;

    public SolicitudRegistroController(SolicitudRegistroService solicitudService) {
        this.solicitudService = solicitudService;
    }
    @PostMapping("/correo")
    public SolicitudRegistroResponse EnviarCorreo(@RequestParam String correo) {
        return solicitudService.EnviarCorreo(correo);
    }


    // PASO 2
    @PostMapping("/verificar")
    public SolicitudRegistroResponse verificarCodigo(@RequestBody VerificarCodigoRequest req) {
        return solicitudService.verificarCodigo(req);
    }

    // PASO 3
    @PostMapping("/datos")
    public SolicitudRegistroResponse enviarDatos(@RequestBody SolicitudRegistroRequest req) {
        return solicitudService.enviarDatos(req);
    }

    // ADMIN
    @GetMapping("/pendientes")
    public List<SolicitudPendienteResponse> listarpendientes() {
        return solicitudService.listarPendientes();
    }

    // ADMIN
    @PostMapping("/{idSolicitud}/aprobar")
    public SolicitudRegistroResponse aprobar(@PathVariable Integer idSolicitud) {
        return solicitudService.aprobar(idSolicitud);
    }

    // ADMIN
    @PostMapping("/{idSolicitud}/rechazar")
    public SolicitudRegistroResponse rechazar(@PathVariable Integer idSolicitud, @RequestParam(required = false) String motivo) {
        return solicitudService.rechazar(idSolicitud, motivo);
    }
}
