package com.erwin.backend.controller;

import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.service.Dt2Service;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/dt2")
public class Dt2Controller {

    private final Dt2Service service;

    public Dt2Controller(Dt2Service service) {
        this.service = service;
    }

    // =========================================================
    // MÓDULO 1 — Configuración inicial
    // =========================================================

    @GetMapping("/proyectos/pendientes-configuracion")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarPendientesConfiguracion() {
        return service.listarProyectosPendientesConfiguracion();
    }

    @GetMapping("/proyectos/{idProyecto}/configuracion")
    public Dt2Dtos.ConfiguracionProyectoDto getConfiguracion(@PathVariable Integer idProyecto) {
        return service.getConfiguracion(idProyecto);
    }

    @PostMapping("/proyectos/{idProyecto}/asignar-docente-dt2")
    public Dt2Dtos.MensajeDto asignarDocenteDt2(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarDocenteDt2Request req) {
        req.setIdProyecto(idProyecto);
        return service.asignarDocenteDt2(req);
    }

    @PostMapping("/proyectos/{idProyecto}/asignar-director")
    public Dt2Dtos.MensajeDto asignarDirector(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarDirectorRequest req) {
        req.setIdProyecto(idProyecto);
        return service.asignarDirector(req);
    }

    @PostMapping("/proyectos/{idProyecto}/asignar-tribunal")
    public Dt2Dtos.MensajeDto asignarTribunal(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarTribunalDt2Request req) {
        req.setIdProyecto(idProyecto);
        return service.asignarTribunal(req);
    }


    /**
     * GET /api/dt2/proyectos/en-predefensa
     * ✅ NUEVO: proyectos en estado PREDEFENSA para el coordinador
     */
    @GetMapping("/proyectos/en-predefensa")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> proyectosEnPredefensa() {
        return service.listarProyectosEnPredefensa();
    }

    // =========================================================
    // MÓDULO 2 — Seguimiento de avances
    // =========================================================

    @GetMapping("/director/{idDirector}/proyectos")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> proyectosDirector(@PathVariable Integer idDirector) {
        return service.listarProyectosDirector(idDirector);
    }

    /**
     * GET /api/dt2/docente-dt2/{idDocenteDt2}/proyectos
     * ✅ NUEVO: proyectos donde el docente está asignado como DT2
     * y el documento está en APROBADO_POR_DIRECTOR (listos para antiplagio)
     */
    @GetMapping("/docente-dt2/{idDocenteDt2}/proyectos")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> proyectosDocenteDt2(@PathVariable Integer idDocenteDt2) {
        return service.listarProyectosDocenteDt2(idDocenteDt2);
    }

    /**
     * GET /api/dt2/tribunal/{idDocente}/proyectos
     * ✅ NUEVO: proyectos en PREDEFENSA donde el docente es miembro del tribunal
     */
    @GetMapping("/tribunal/{idDocente}/proyectos")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> proyectosTribunal(@PathVariable Integer idDocente) {
        return service.listarProyectosTribunal(idDocente);
    }

    @PostMapping("/proyectos/{idProyecto}/asesorias")
    public Dt2Dtos.AsesoriaDto registrarAsesoria(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.RegistrarAsesoriaRequest req) {
        req.setIdProyecto(idProyecto);
        return service.registrarAsesoria(req);
    }

    @GetMapping("/proyectos/{idProyecto}/asesorias")
    public List<Dt2Dtos.AsesoriaDto> listarAsesorias(@PathVariable Integer idProyecto) {
        return service.listarAsesorias(idProyecto);
    }

    @PostMapping("/proyectos/{idProyecto}/acta-corte")
    public Dt2Dtos.ActaCorteDto cerrarCorte(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CerrarCorteRequest req) {
        req.setIdProyecto(idProyecto);
        return service.cerrarCorte(req);
    }

    @GetMapping("/proyectos/{idProyecto}/seguimiento")
    public Dt2Dtos.SeguimientoDto getSeguimiento(@PathVariable Integer idProyecto) {
        return service.getSeguimiento(idProyecto);
    }

    // =========================================================
    // MÓDULO 3 — Certificación antiplagio
    // =========================================================

    /**
     * POST /api/dt2/proyectos/{idProyecto}/antiplagio
     * ✅ CAMBIO: parámetro idDirector → idDocenteDt2
     * Requiere documento.estado = APROBADO_POR_DIRECTOR
     * Si favorable → documento pasa a ANTIPLAGIO_APROBADO
     */
    @PostMapping(value = "/proyectos/{idProyecto}/antiplagio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dt2Dtos.CertificadoAntiplacioDto registrarAntiplagio(
            @PathVariable Integer idProyecto,
            @RequestParam Integer idDocenteDt2,
            @RequestParam BigDecimal porcentajeCoincidencia,
            @RequestParam(required = false) String observaciones,
            @RequestParam("archivo") MultipartFile archivo) {
        return service.registrarAntiplagio(idProyecto, idDocenteDt2, porcentajeCoincidencia, observaciones, archivo);
    }

    @GetMapping("/proyectos/{idProyecto}/antiplagio")
    public Dt2Dtos.CertificadoAntiplacioDto getCertificado(@PathVariable Integer idProyecto) {
        return service.getCertificadoAntiplagio(idProyecto);
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    /**
     * POST /api/dt2/proyectos/{idProyecto}/predefensa/programar
     * Requiere documento.estado = ANTIPLAGIO_APROBADO
     * Al programar → documento pasa a EN_PREDEFENSA
     */
    @PostMapping("/proyectos/{idProyecto}/predefensa/programar")
    public Dt2Dtos.MensajeDto programarPredefensa(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.ProgramarPredefensaRequest req) {
        req.setIdProyecto(idProyecto);
        return service.programarPredefensa(req);
    }

    @GetMapping("/proyectos/{idProyecto}/predefensa")
    public Dt2Dtos.PredefensaDto getPredefensa(@PathVariable Integer idProyecto) {
        return service.getPredefensaDto(idProyecto);
    }

    @PostMapping("/proyectos/{idProyecto}/predefensa/calificar-docente")
    public Dt2Dtos.PredefensaDto calificarPredefensaDocente(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CalificarPredefensaDocenteRequest req) {
        req.setIdProyecto(idProyecto);
        return service.calificarPredefensaDocente(req);
    }

    @PostMapping("/proyectos/{idProyecto}/predefensa/calificar-tribunal")
    public Dt2Dtos.PredefensaDto calificarPredefensaTribunal(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CalificarPredefensaTribunalRequest req) {
        req.setIdProyecto(idProyecto);
        return service.calificarPredefensaTribunal(req);
    }

    // =========================================================
    // MÓDULO 5 — Sustentación final
    // =========================================================

    @GetMapping("/proyectos/{idProyecto}/sustentacion/documentos")
    public Dt2Dtos.DocumentosPreviosDto getDocumentosPrevios(@PathVariable Integer idProyecto) {
        return service.getDocumentosPrevios(idProyecto);
    }

    @PostMapping("/proyectos/{idProyecto}/sustentacion/documentos")
    public Dt2Dtos.DocumentosPreviosDto registrarDocumentosPrevios(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.DocumentosPreviosRequest req) {
        req.setIdProyecto(idProyecto);
        return service.registrarDocumentosPrevios(req);
    }

    @PostMapping("/proyectos/{idProyecto}/sustentacion/programar")
    public Dt2Dtos.MensajeDto programarSustentacion(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.ProgramarSustentacionRequest req) {
        req.setIdProyecto(idProyecto);
        return service.programarSustentacion(req);
    }

    @PostMapping("/proyectos/{idProyecto}/sustentacion/calificar")
    public Dt2Dtos.ResultadoSustentacionDto calificarSustentacion(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CalificarSustentacionRequest req) {
        req.setIdProyecto(idProyecto);
        return service.calificarSustentacion(req);
    }

    @PostMapping("/proyectos/{idProyecto}/sustentacion/consolidar")
    public Dt2Dtos.ResultadoSustentacionDto consolidarResultado(@PathVariable Integer idProyecto) {
        return service.consolidarResultado(idProyecto);
    }

    @GetMapping("/proyectos/{idProyecto}/sustentacion/resultado")
    public ResponseEntity<Map<String, Object>> getResultado(@PathVariable Integer idProyecto) {
        Dt2Dtos.CertificadoAntiplacioDto antiplagio = service.getCertificadoAntiplagio(idProyecto);
        Dt2Dtos.DocumentosPreviosDto docs = service.getDocumentosPrevios(idProyecto);
        Dt2Dtos.PredefensaDto predefensa = service.getPredefensaDto(idProyecto);
        return ResponseEntity.ok(Map.of(
                "antiplagio", antiplagio,
                "documentosPrevios", docs,
                "predefensa", predefensa
        ));
    }

    @PostMapping("/proyectos/{idProyecto}/sustentacion/segunda-oportunidad")
    public Dt2Dtos.MensajeDto habilitarSegundaOportunidad(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.SegundaOportunidadRequest req) {
        req.setIdProyecto(idProyecto);
        return service.habilitarSegundaOportunidad(req);
    }
}