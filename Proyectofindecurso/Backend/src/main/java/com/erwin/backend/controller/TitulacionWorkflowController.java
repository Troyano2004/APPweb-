package com.erwin.backend.controller;

import com.erwin.backend.dtos.TitulacionWorkflowDtos;
import com.erwin.backend.service.TitulacionWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/titulacion2/workflow")
public class TitulacionWorkflowController {

    private final TitulacionWorkflowService service;

    public TitulacionWorkflowController(TitulacionWorkflowService service) {
        this.service = service;
    }

    @PostMapping("/documento/{idDocumento}/listo-para-tribunal")
    public TitulacionWorkflowDtos.WorkflowResumenDto listoParaTribunal(@PathVariable Integer idDocumento,
                                                                       @RequestBody TitulacionWorkflowDtos.PrepararTribunalRequest req) {
        return service.prepararParaTribunal(idDocumento, req);
    }

    @PostMapping("/documento/{idDocumento}/asignar-tribunal")
    public TitulacionWorkflowDtos.WorkflowResumenDto asignarTribunal(@PathVariable Integer idDocumento,
                                                                     @RequestBody TitulacionWorkflowDtos.AsignarTribunalRequest req) {
        return service.asignarTribunal(idDocumento, req);
    }

    @PostMapping("/documento/{idDocumento}/agendar-sustentacion")
    public TitulacionWorkflowDtos.WorkflowResumenDto agendar(@PathVariable Integer idDocumento,
                                                             @RequestBody TitulacionWorkflowDtos.AgendarSustentacionRequest req) {
        return service.agendarSustentacion(idDocumento, req);
    }

    @PostMapping("/documento/{idDocumento}/registrar-resultado")
    public TitulacionWorkflowDtos.WorkflowResumenDto registrarResultado(@PathVariable Integer idDocumento,
                                                                        @RequestBody TitulacionWorkflowDtos.RegistrarResultadoRequest req) {
        return service.registrarResultado(idDocumento, req);
    }

    @PostMapping("/documento/{idDocumento}/cerrar")
    public TitulacionWorkflowDtos.WorkflowResumenDto cerrar(@PathVariable Integer idDocumento,
                                                            @RequestBody TitulacionWorkflowDtos.CerrarExpedienteRequest req) {
        return service.cerrarExpediente(idDocumento, req);
    }
}
