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

/**
 * Controlador principal de los 5 módulos de Titulación II.
 * Base path: /api/dt2
 *
 * M1: /api/dt2/proyectos/** — Configuración inicial
 * M2: /api/dt2/asesorias/** — Seguimiento de avances
 * M3: /api/dt2/antiplagio/** — Certificación antiplagio
 * M4: /api/dt2/predefensa/** — Predefensa
 * M5: /api/dt2/sustentacion/** — Sustentación final
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/dt2")
public class Dt2Controller {

    private final Dt2Service service;

    public Dt2Controller(Dt2Service service) {
        this.service = service;
    }

    // =========================================================
    // MÓDULO 1 — Configuración inicial de Titulación II
    // =========================================================

    /**
     * GET /api/dt2/proyectos/pendientes-configuracion
     * Rol: COORDINADOR
     * Lista proyectos con anteproyecto APROBADO pendientes de configuración DT2.
     */
    @GetMapping("/proyectos/pendientes-configuracion")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarPendientesConfiguracion() {
        return service.listarProyectosPendientesConfiguracion();
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/configuracion
     * Rol: COORDINADOR, DOCENTE (DT2, Director)
     * Estado de configuración del proyecto.
     */
    @GetMapping("/proyectos/{idProyecto}/configuracion")
    public Dt2Dtos.ConfiguracionProyectoDto getConfiguracion(@PathVariable Integer idProyecto) {
        return service.getConfiguracion(idProyecto);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/asignar-docente-dt2
     * Rol: COORDINADOR
     * Asigna el docente DT2 al proyecto.
     */
    @PostMapping("/proyectos/{idProyecto}/asignar-docente-dt2")
    public Dt2Dtos.MensajeDto asignarDocenteDt2(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarDocenteDt2Request req) {
        req.setIdProyecto(idProyecto);
        return service.asignarDocenteDt2(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/asignar-director
     * Rol: COORDINADOR
     * Asigna o cambia el Director del Trabajo de Integración Curricular.
     * Cambio requiere motivo justificado.
     */
    @PostMapping("/proyectos/{idProyecto}/asignar-director")
    public Dt2Dtos.MensajeDto asignarDirector(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarDirectorRequest req) {
        req.setIdProyecto(idProyecto);
        return service.asignarDirector(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/asignar-tribunal
     * Rol: COORDINADOR
     * Registra los miembros del Tribunal (mínimo 3).
     */
    @PostMapping("/proyectos/{idProyecto}/asignar-tribunal")
    public Dt2Dtos.MensajeDto asignarTribunal(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.AsignarTribunalDt2Request req) {
        req.setIdProyecto(idProyecto);
        return service.asignarTribunal(req);
    }

    // =========================================================
    // MÓDULO 2 — Seguimiento de avances
    // =========================================================

    /**
     * GET /api/dt2/director/{idDirector}/proyectos
     * Rol: DOCENTE (Director)
     * Lista los proyectos asignados al director en DESARROLLO o PREDEFENSA.
     */
    @GetMapping("/director/{idDirector}/proyectos")
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> proyectosDirector(@PathVariable Integer idDirector) {
        return service.listarProyectosDirector(idDirector);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/asesorias
     * Rol: DOCENTE (Director asignado)
     * Registra una asesoría con fecha, observaciones, porcentaje de avance y corte.
     */
    @PostMapping("/proyectos/{idProyecto}/asesorias")
    public Dt2Dtos.AsesoriaDto registrarAsesoria(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.RegistrarAsesoriaRequest req) {
        req.setIdProyecto(idProyecto);
        return service.registrarAsesoria(req);
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/asesorias
     * Rol: DOCENTE (Director, DT2), ESTUDIANTE (solo lectura)
     * Lista todas las asesorías del proyecto.
     */
    @GetMapping("/proyectos/{idProyecto}/asesorias")
    public List<Dt2Dtos.AsesoriaDto> listarAsesorias(@PathVariable Integer idProyecto) {
        return service.listarAsesorias(idProyecto);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/acta-corte
     * Rol: DOCENTE (Director)
     * Cierra el corte y genera el acta. Advierte si < 5 asesorías.
     */
    @PostMapping("/proyectos/{idProyecto}/acta-corte")
    public Dt2Dtos.ActaCorteDto cerrarCorte(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CerrarCorteRequest req) {
        req.setIdProyecto(idProyecto);
        return service.cerrarCorte(req);
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/seguimiento
     * Rol: DOCENTE (Director, DT2), ESTUDIANTE, COORDINADOR
     * Resumen completo: asesorías, actas, avance.
     */
    @GetMapping("/proyectos/{idProyecto}/seguimiento")
    public Dt2Dtos.SeguimientoDto getSeguimiento(@PathVariable Integer idProyecto) {
        return service.getSeguimiento(idProyecto);
    }

    // =========================================================
    // MÓDULO 3 — Certificación antiplagio
    // =========================================================

    /**
     * POST /api/dt2/proyectos/{idProyecto}/antiplagio
     * Rol: DOCENTE (Director)
     * Sube el informe COMPILATIO (PDF) y registra el porcentaje.
     * Umbral: < 10% = favorable; >= 10% = bloqueado.
     * Multipart: "archivo" (PDF) + params: idDirector, porcentajeCoincidencia, observaciones
     */
    @PostMapping(value = "/proyectos/{idProyecto}/antiplagio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dt2Dtos.CertificadoAntiplacioDto registrarAntiplagio(
            @PathVariable Integer idProyecto,
            @RequestParam Integer idDirector,
            @RequestParam BigDecimal porcentajeCoincidencia,
            @RequestParam(required = false) String observaciones,
            @RequestParam("archivo") MultipartFile archivo) {
        return service.registrarAntiplagio(idProyecto, idDirector, porcentajeCoincidencia, observaciones, archivo);
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/antiplagio
     * Rol: DOCENTE (Director, DT2), ESTUDIANTE, COORDINADOR
     * Estado del certificado + historial de intentos.
     */
    @GetMapping("/proyectos/{idProyecto}/antiplagio")
    public Dt2Dtos.CertificadoAntiplacioDto getCertificado(@PathVariable Integer idProyecto) {
        return service.getCertificadoAntiplagio(idProyecto);
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    /**
     * POST /api/dt2/proyectos/{idProyecto}/predefensa/programar
     * Rol: COORDINADOR o DOCENTE (DT2)
     * Programa la fecha de predefensa. Requiere antiplagio favorable.
     */
    @PostMapping("/proyectos/{idProyecto}/predefensa/programar")
    public Dt2Dtos.MensajeDto programarPredefensa(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.ProgramarPredefensaRequest req) {
        req.setIdProyecto(idProyecto);
        return service.programarPredefensa(req);
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/predefensa
     * Rol: Todos (Director, DT2, Tribunal, Estudiante, Coordinador)
     * Estado completo de la predefensa con notas y cálculo ponderado.
     */
    @GetMapping("/proyectos/{idProyecto}/predefensa")
    public Dt2Dtos.PredefensaDto getPredefensa(@PathVariable Integer idProyecto) {
        return service.getPredefensaDto(idProyecto);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/predefensa/calificar-docente
     * Rol: DOCENTE (DT2 asignado)
     * El docente DT2 registra su calificación (representa el 60%).
     */
    @PostMapping("/proyectos/{idProyecto}/predefensa/calificar-docente")
    public Dt2Dtos.PredefensaDto calificarPredefensaDocente(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CalificarPredefensaDocenteRequest req) {
        req.setIdProyecto(idProyecto);
        return service.calificarPredefensaDocente(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/predefensa/calificar-tribunal
     * Rol: DOCENTE (miembro del tribunal)
     * Un miembro del tribunal registra su calificación (promedio = 40%).
     * Puede indicar solicitud de correcciones.
     */
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

    /**
     * GET /api/dt2/proyectos/{idProyecto}/sustentacion/documentos
     * Rol: COORDINADOR, DOCENTE (Director, DT2)
     * Retorna el checklist de documentos previos requeridos.
     */
    @GetMapping("/proyectos/{idProyecto}/sustentacion/documentos")
    public Dt2Dtos.DocumentosPreviosDto getDocumentosPrevios(@PathVariable Integer idProyecto) {
        return service.getDocumentosPrevios(idProyecto);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/sustentacion/documentos
     * Rol: COORDINADOR o DOCENTE (Director)
     * Registra la entrega de los documentos previos (checklist).
     */
    @PostMapping("/proyectos/{idProyecto}/sustentacion/documentos")
    public Dt2Dtos.DocumentosPreviosDto registrarDocumentosPrevios(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.DocumentosPreviosRequest req) {
        req.setIdProyecto(idProyecto);
        return service.registrarDocumentosPrevios(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/sustentacion/programar
     * Rol: COORDINADOR o DOCENTE (DT2)
     * Programa la fecha de sustentación final.
     * Requiere: documentos previos completos + predefensa aprobada.
     */
    @PostMapping("/proyectos/{idProyecto}/sustentacion/programar")
    public Dt2Dtos.MensajeDto programarSustentacion(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.ProgramarSustentacionRequest req) {
        req.setIdProyecto(idProyecto);
        return service.programarSustentacion(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/sustentacion/calificar
     * Rol: DOCENTE (miembro del tribunal)
     * Un miembro del tribunal califica con los 4 criterios:
     * - calidad_trabajo (20%), originalidad (20%), dominio_tema (30%), preguntas (30%).
     */
    @PostMapping("/proyectos/{idProyecto}/sustentacion/calificar")
    public Dt2Dtos.ResultadoSustentacionDto calificarSustentacion(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.CalificarSustentacionRequest req) {
        req.setIdProyecto(idProyecto);
        return service.calificarSustentacion(req);
    }

    /**
     * POST /api/dt2/proyectos/{idProyecto}/sustentacion/consolidar
     * Rol: COORDINADOR o DOCENTE (DT2)
     * Consolida el resultado final cuando todos los miembros calificaron.
     * Calcula: promedio tribunal + nota de grado (80% récord + 20% sustentación).
     */
    @PostMapping("/proyectos/{idProyecto}/sustentacion/consolidar")
    public Dt2Dtos.ResultadoSustentacionDto consolidarResultado(@PathVariable Integer idProyecto) {
        return service.consolidarResultado(idProyecto);
    }

    /**
     * GET /api/dt2/proyectos/{idProyecto}/sustentacion/resultado
     * Rol: Todos
     * Resultado actual de la sustentación del proyecto.
     */
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

    /**
     * POST /api/dt2/proyectos/{idProyecto}/sustentacion/segunda-oportunidad
     * Rol: COORDINADOR
     * Habilita la segunda oportunidad para un estudiante reprobado (max 15 días).
     */
    @PostMapping("/proyectos/{idProyecto}/sustentacion/segunda-oportunidad")
    public Dt2Dtos.MensajeDto habilitarSegundaOportunidad(
            @PathVariable Integer idProyecto,
            @RequestBody Dt2Dtos.SegundaOportunidadRequest req) {
        req.setIdProyecto(idProyecto);
        return service.habilitarSegundaOportunidad(req);
    }
}
