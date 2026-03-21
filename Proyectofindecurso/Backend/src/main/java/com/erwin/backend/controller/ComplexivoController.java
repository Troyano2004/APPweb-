
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

    // ── Coordinador DT1 ──────────────────────────────────────────────
    @GetMapping("/coordinador/dt1/info")
    public InfoCoordinadorDt1Dto infoCoordinadorDt1(@RequestParam Integer idUsuario) {
        return service.infoCoordinadorDt1(idUsuario);
    }

    @PostMapping("/coordinador/dt1/asignar")
    public ComplexivoDocenteAsignacionResponse asignarDt1(
            @RequestBody AsignarDt1ComplexivoRequest req) {
        return service.asignarDt1(req);
    }

    // ── Coordinador DT2 ──────────────────────────────────────────────
    @GetMapping("/coordinador/dt2/info")
    public InfoCoordinadorDt2Dto infoCoordinadorDt2(@RequestParam Integer idUsuario) {
        return service.infoCoordinadorDt2(idUsuario);
    }

    @PostMapping("/coordinador/dt2/asignar")
    public ComplexivoDocenteAsignacionResponse asignarDt2(
            @RequestBody AsignarDt2ComplexivoRequest req) {
        return service.asignarDt2(req);
    }

    // ── Endpoints legacy (compatibilidad con frontend existente) ─────
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
    public EstadoComplexivoEstudianteDto estadoEstudiante(
            @PathVariable Integer idEstudiante) {
        return service.estadoEstudiante(idEstudiante);
    }

    @GetMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto getInforme(@PathVariable Integer idEstudiante) {
        return service.getInforme(idEstudiante);
    }

    @PutMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto guardarInforme(
            @PathVariable Integer idEstudiante,
            @RequestBody ComplexivoInformeUpdateRequest req) {
        return service.guardarInforme(idEstudiante, req);
    }

    @PostMapping("/estudiante/{idEstudiante}/informe/enviar")
    public ComplexivoInformeDto enviarInforme(@PathVariable Integer idEstudiante) {
        return service.enviarInforme(idEstudiante);
    }

    // ── DT1 — propuestas ─────────────────────────────────────────────
    @GetMapping("/dt1/{idDocente}/propuestas")
    public List<PropuestaComplexivoDto> propuestasDeDocenteDt1(
            @PathVariable Integer idDocente) {
        return service.propuestasDeDocenteDt1(idDocente);
    }

    @PostMapping("/dt1/{idDocente}/propuestas/{idPropuesta}/decision")
    public PropuestaComplexivoDto decidirPropuestaDt1(
            @PathVariable Integer idDocente,
            @PathVariable Integer idPropuesta,
            @RequestBody DecisionPropuestaComplexivoRequest req) {
        return service.decidirPropuestaDt1(idDocente, idPropuesta, req);
    }

    // Endpoints legacy propuestas
    @GetMapping("/docente/{idDocente}/propuestas")
    public List<PropuestaComplexivoDto> propuestasDeDocente(
            @PathVariable Integer idDocente) {
        return service.propuestasDeDocente(idDocente);
    }

    @PostMapping("/docente/{idDocente}/propuestas/{idPropuesta}/decision")
    public PropuestaComplexivoDto decidirPropuesta(
            @PathVariable Integer idDocente,
            @PathVariable Integer idPropuesta,
            @RequestBody DecisionPropuestaComplexivoRequest req) {
        return service.decidirPropuesta(idDocente, idPropuesta, req);
    }

    // ── DT2 — informes ───────────────────────────────────────────────
    @GetMapping("/dt2/{idDocente}/estudiantes")
    public List<EstudianteDeDocenteDto> estudiantesDt2(
            @PathVariable Integer idDocente) {
        return service.estudiantesDeDocenteDt2(idDocente);
    }

    @GetMapping("/dt2/{idDocente}/informe/{idComplexivo}")
    public ComplexivoInformeDto getInformeDt2(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.getInformeParaDt2(idDocente, idComplexivo);
    }

    @PostMapping("/dt2/{idDocente}/informe/{idInforme}/aprobar")
    public ComplexivoInformeDto aprobarInforme(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody(required = false) Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "APROBADO",
                body != null ? body.get("observaciones") : null);
    }

    @PostMapping("/dt2/{idDocente}/informe/{idInforme}/rechazar")
    public ComplexivoInformeDto rechazarInforme(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "RECHAZADO",
                body.get("observaciones"));
    }

    @PostMapping("/dt2/{idDocente}/asesoria/{idComplexivo}")
    public ComplexivoAsesoriaDto registrarAsesoria(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo,
            @RequestBody RegistrarAsesoriaRequest req) {
        return service.registrarAsesoria(idDocente, idComplexivo, req);
    }

    @GetMapping("/dt2/{idDocente}/asesorias/{idComplexivo}")
    public List<ComplexivoAsesoriaDto> listarAsesorias(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.listarAsesorias(idDocente, idComplexivo);
    }

    // Endpoints legacy informes
    @GetMapping("/docente/{idDocente}/estudiantes")
    public List<EstudianteDeDocenteDto> misEstudiantes(
            @PathVariable Integer idDocente) {
        return service.estudiantesDeDocente(idDocente);
    }

    @GetMapping("/docente/{idDocente}/informe/{idComplexivo}")
    public ComplexivoInformeDto getInformeDocente(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.getInformeParaDocente(idDocente, idComplexivo);
    }

    @PostMapping("/docente/{idDocente}/informe/{idInforme}/aprobar")
    public ComplexivoInformeDto aprobarInformeLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody(required = false) Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "APROBADO",
                body != null ? body.get("observaciones") : null);
    }

    @PostMapping("/docente/{idDocente}/informe/{idInforme}/rechazar")
    public ComplexivoInformeDto rechazarInformeLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "RECHAZADO",
                body.get("observaciones"));
    }

    @PostMapping("/docente/{idDocente}/asesoria/{idComplexivo}")
    public ComplexivoAsesoriaDto registrarAsesoriaLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo,
            @RequestBody RegistrarAsesoriaRequest req) {
        return service.registrarAsesoria(idDocente, idComplexivo, req);
    }

    @GetMapping("/docente/{idDocente}/asesorias/{idComplexivo}")
    public List<ComplexivoAsesoriaDto> listarAsesoriasLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.listarAsesorias(idDocente, idComplexivo);
    }
}