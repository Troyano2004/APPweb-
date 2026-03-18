package com.erwin.backend.controller;

import com.erwin.backend.dtos.ComplexivoDtos.*;
import com.erwin.backend.service.ComplexivoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complexivo")
@CrossOrigin(origins = "http://localhost:4200")
public class ComplexivoController {

    private final ComplexivoService service;

    public ComplexivoController(ComplexivoService service) {
        this.service = service;
    }

    // ── Coordinador ──────────────────────────────────────────────────
    @GetMapping("/coordinador/info")
    public InfoCoordinadorComplexivoDto infoCoordinador(@RequestParam Integer idUsuario) {
        return service.infoCoordinador(idUsuario);
    }

    @PostMapping("/coordinador/asignar-docente")
    public ComplexivoDocenteAsignacionResponse asignarDocente(
            @RequestBody AsignarDocenteComplexivoRequest req) {
        return service.asignarDocente(req);
    }

    // ── Estudiante ───────────────────────────────────────────────────
    @GetMapping("/estudiante/{idEstudiante}/estado")
    public EstadoComplexivoEstudianteDto estadoEstudiante(@PathVariable Integer idEstudiante) {
        return service.estadoEstudiante(idEstudiante);
    }

    @GetMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto getInforme(@PathVariable Integer idEstudiante) {
        return service.getInforme(idEstudiante);
    }

    @PutMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto guardarInforme(@PathVariable Integer idEstudiante,
                                               @RequestBody ComplexivoInformeUpdateRequest req) {
        return service.guardarInforme(idEstudiante, req);
    }

    @PostMapping("/estudiante/{idEstudiante}/informe/enviar")
    public ComplexivoInformeDto enviarInforme(@PathVariable Integer idEstudiante) {
        return service.enviarInforme(idEstudiante);
    }

    // ── Docente complexivo ───────────────────────────────────────────
    @GetMapping("/docente/{idDocente}/estudiantes")
    public List<EstudianteDeDocenteDto> misEstudiantes(@PathVariable Integer idDocente) {
        return service.estudiantesDeDocente(idDocente);
    }

    @GetMapping("/docente/{idDocente}/informe/{idComplexivo}")
    public ComplexivoInformeDto getInformeDocente(@PathVariable Integer idDocente,
                                                  @PathVariable Integer idComplexivo) {
        return service.getInformeParaDocente(idDocente, idComplexivo);
    }

    @PostMapping("/docente/{idDocente}/informe/{idInforme}/aprobar")
    public ComplexivoInformeDto aprobarInforme(@PathVariable Integer idDocente,
                                               @PathVariable Integer idInforme,
                                               @RequestBody(required = false) Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "APROBADO",
                body != null ? body.get("observaciones") : null);
    }

    @PostMapping("/docente/{idDocente}/informe/{idInforme}/rechazar")
    public ComplexivoInformeDto rechazarInforme(@PathVariable Integer idDocente,
                                                @PathVariable Integer idInforme,
                                                @RequestBody Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "RECHAZADO",
                body.get("observaciones"));
    }

    @PostMapping("/docente/{idDocente}/asesoria/{idComplexivo}")
    public ComplexivoAsesoriaDto registrarAsesoria(@PathVariable Integer idDocente,
                                                   @PathVariable Integer idComplexivo,
                                                   @RequestBody RegistrarAsesoriaRequest req) {
        return service.registrarAsesoria(idDocente, idComplexivo, req);
    }

    @GetMapping("/docente/{idDocente}/asesorias/{idComplexivo}")
    public List<ComplexivoAsesoriaDto> listarAsesorias(@PathVariable Integer idDocente,
                                                       @PathVariable Integer idComplexivo) {
        return service.listarAsesorias(idDocente, idComplexivo);
    }


}