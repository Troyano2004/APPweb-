
package com.erwin.backend.controller;

import com.erwin.backend.dtos.EstadoEstudianteDto;
import com.erwin.backend.dtos.EstadoEstudianteDto.EtapaDto;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/estado-estudiante")
@CrossOrigin(origins = "http://localhost:4200")
public class EstadoEstudianteController {

    private final EstudianteRepository             estudianteRepo;
    private final PropuestaTitulacionRepository    propuestaRepo;
    private final AnteproyectoTitulacionRepository anteproyectoRepo;
    private final ProyectoTitulacionRepository     proyectoRepo;
    private final Dt2AsignacionRepository          dt2Repo;
    private final SustentacionRepository           sustentacionRepo;
    // Complexivo
    private final ComplexivoTitulacionRepository      complexivoRepo;
    private final ComplexivoDt1AsignacionRepository   cxDt1Repo;
    private final ComplexivoDt2AsignacionRepository   cxDt2Repo;
    private final ComplexivoInformePracticoRepository informeRepo;
    private final PeriodoTitulacionRepository         periodoRepo;

    public EstadoEstudianteController(
            EstudianteRepository estudianteRepo,
            PropuestaTitulacionRepository propuestaRepo,
            AnteproyectoTitulacionRepository anteproyectoRepo,
            ProyectoTitulacionRepository proyectoRepo,
            Dt2AsignacionRepository dt2Repo,
            SustentacionRepository sustentacionRepo,
            ComplexivoTitulacionRepository complexivoRepo,
            ComplexivoDt1AsignacionRepository cxDt1Repo,
            ComplexivoDt2AsignacionRepository cxDt2Repo,
            ComplexivoInformePracticoRepository informeRepo,
            PeriodoTitulacionRepository periodoRepo) {
        this.estudianteRepo  = estudianteRepo;
        this.propuestaRepo   = propuestaRepo;
        this.anteproyectoRepo = anteproyectoRepo;
        this.proyectoRepo    = proyectoRepo;
        this.dt2Repo         = dt2Repo;
        this.sustentacionRepo = sustentacionRepo;
        this.complexivoRepo  = complexivoRepo;
        this.cxDt1Repo       = cxDt1Repo;
        this.cxDt2Repo       = cxDt2Repo;
        this.informeRepo     = informeRepo;
        this.periodoRepo     = periodoRepo;
    }

    @GetMapping("/{idEstudiante}")
    public ResponseEntity<EstadoEstudianteDto> obtenerEstado(@PathVariable Integer idEstudiante) {

        Estudiante est = estudianteRepo.findById(idEstudiante).orElse(null);
        if (est == null) return ResponseEntity.notFound().build();

        // ── Detectar modalidad: si existe registro en complexivo_titulacion → es Complexivo
        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc().orElse(null);
        boolean esComplexivo = false;
        ComplexivoTitulacion ct = null;
        if (periodo != null) {
            var ctOpt = complexivoRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                    idEstudiante, periodo.getIdPeriodo());
            if (ctOpt.isPresent()) {
                esComplexivo = true;
                ct = ctOpt.get();
            }
        }

        String nombre  = est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos();
        String carrera = est.getCarrera() != null ? est.getCarrera().getNombre() : "Sin carrera";

        if (esComplexivo) {
            return ResponseEntity.ok(buildComplexivo(idEstudiante, nombre, carrera, ct, periodo));
        } else {
            return ResponseEntity.ok(buildIntegracion(idEstudiante, nombre, carrera));
        }
    }

    // ══════════════════════════════════════════════════════
    // FLUJO EXAMEN COMPLEXIVO
    // Etapas: Propuesta → Informe Práctico → Revisión DT2 → Examen Final
    // ══════════════════════════════════════════════════════
    private EstadoEstudianteDto buildComplexivo(Integer idEstudiante, String nombre,
                                                String carrera, ComplexivoTitulacion ct,
                                                PeriodoTitulacion periodo) {
        List<EtapaDto> etapas = new ArrayList<>();
        int completados = 0;

        // ── 1. INSCRIPCIÓN / PROPUESTA ────────────────────────────────────
        var propuestas = propuestaRepo.findByEstudiante_IdEstudiante(idEstudiante);
        PropuestaTitulacion propuesta = propuestas.isEmpty() ? null
                : propuestas.get(propuestas.size() - 1);

        String ep = "PENDIENTE", dp = "Aún no has enviado una propuesta de tema.";
        if (propuesta != null) {
            String raw = nvl(propuesta.getEstado());
            ep = toEstado(raw);
            dp = switch (raw) {
                case "ENVIADA"     -> "Propuesta enviada, esperando revisión del docente DT1.";
                case "EN_REVISION" -> "Propuesta en revisión por el docente DT1.";
                case "APROBADA"    -> "Propuesta aprobada" + (propuesta.getFechaRevision() != null
                        ? " el " + propuesta.getFechaRevision() : "") + ".";
                case "RECHAZADA"   -> "Rechazada: " + nvlStr(propuesta.getObservacionesComision(),
                        "revisa las observaciones.");
                default -> "En proceso.";
            };
        }
        etapas.add(new EtapaDto("PROPUESTA", "Propuesta de tema", dp, ep,
                propuesta != null ? propuesta.getFechaEnvio() : null));
        if ("COMPLETADO".equals(ep)) completados++;

        // ── 2. DOCENTE DT1 ASIGNADO ───────────────────────────────────────
        String eDt1 = "PENDIENTE", dDt1 = "Pendiente de asignación de docente guía (DT1).";
        boolean tieneDt1 = periodo != null &&
                cxDt1Repo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        idEstudiante, periodo.getIdPeriodo());
        if (tieneDt1) {
            eDt1 = "COMPLETADO";
            dDt1 = "Docente guía DT1 asignado.";
            completados++;
        }
        etapas.add(new EtapaDto("DOCENTE_DT1", "Asignación docente guía (DT1)", dDt1, eDt1, null));

        // ── 3. INFORME PRÁCTICO ───────────────────────────────────────────
        String eInf = "PENDIENTE", dInf = "Aún no has iniciado tu informe práctico.";
        var informeOpt = informeRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo());
        if (informeOpt.isPresent()) {
            String raw = nvl(informeOpt.get().getEstado());
            eInf = switch (raw) {
                case "APROBADO" -> "COMPLETADO";
                case "RECHAZADO" -> "RECHAZADO";
                case "ENTREGADO", "EN_REVISION" -> "EN_CURSO";
                default -> "EN_CURSO"; // BORRADOR
            };
            dInf = switch (raw) {
                case "BORRADOR"    -> "Informe en borrador, aún no enviado.";
                case "ENTREGADO"   -> "Informe entregado, esperando revisión del docente DT2.";
                case "EN_REVISION" -> "Informe siendo revisado por el docente DT2.";
                case "APROBADO"    -> "Informe práctico aprobado" +
                        (informeOpt.get().getFechaRevision() != null
                                ? " el " + informeOpt.get().getFechaRevision() : "") + ".";
                case "RECHAZADO"   -> "Informe rechazado: " +
                        nvlStr(informeOpt.get().getObservaciones(), "revisa las observaciones y corrige.");
                default -> "En proceso.";
            };
        }
        etapas.add(new EtapaDto("INFORME", "Informe Práctico", dInf, eInf,
                informeOpt.map(ComplexivoInformePractico::getFechaEntrega).orElse(null)));
        if ("COMPLETADO".equals(eInf)) completados++;

        // ── 4. DOCENTE DT2 ASIGNADO / REVISIÓN ───────────────────────────
        String eDt2 = "PENDIENTE", dDt2 = "Pendiente de asignación de docente revisor (DT2).";
        if (periodo != null) {
            var dt2Opt = cxDt2Repo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                    idEstudiante, periodo.getIdPeriodo());
            if (dt2Opt.isPresent()) {
                String nombreDocente = dt2Opt.get().getDocente().getUsuario().getNombres()
                        + " " + dt2Opt.get().getDocente().getUsuario().getApellidos();
                if ("COMPLETADO".equals(eInf)) {
                    eDt2 = "COMPLETADO";
                    dDt2 = "Revisión por docente DT2 (" + nombreDocente + ") completada.";
                    completados++;
                } else {
                    eDt2 = "EN_CURSO";
                    dDt2 = "Docente revisor DT2 asignado: " + nombreDocente + ".";
                }
            }
        }
        etapas.add(new EtapaDto("DOCENTE_DT2", "Revisión docente (DT2)", dDt2, eDt2, null));

        // ── 5. EXAMEN FINAL ───────────────────────────────────────────────
        String eExamen = "PENDIENTE", dExamen = "Examen complexivo final pendiente.";
        String estadoCt = nvl(ct.getEstado());
        if ("APROBADO".equals(estadoCt)) {
            eExamen = "COMPLETADO";
            dExamen = "Examen complexivo aprobado. Proceso de titulación completado.";
            completados++;
        } else if ("REPROBADO".equals(estadoCt)) {
            eExamen = "RECHAZADO";
            dExamen = "Examen complexivo reprobado. Consulta con tu coordinador.";
        } else if ("EN_CURSO".equals(estadoCt) && "COMPLETADO".equals(eDt2)) {
            eExamen = "EN_CURSO";
            dExamen = "Proceso listo para examen final. Coordina con tu director.";
        }
        etapas.add(new EtapaDto("EXAMEN", "Examen Complexivo Final", dExamen, eExamen,
                ct.getFechaInscripcion()));
        if ("COMPLETADO".equals(eExamen)) completados++;

        int pct = (completados * 100) / etapas.size();
        String etapaActual = etapas.stream()
                .filter(e -> "EN_CURSO".equals(e.getEstado())).map(EtapaDto::getTitulo).findFirst()
                .orElse(etapas.stream().filter(e -> "PENDIENTE".equals(e.getEstado()))
                        .map(EtapaDto::getTitulo).findFirst().orElse("Proceso completado"));

        return new EstadoEstudianteDto(nombre, carrera, "EXAMEN COMPLEXIVO", etapaActual, pct, etapas);
    }

    // ══════════════════════════════════════════════════════
    // FLUJO INTEGRACIÓN CURRICULAR
    // Etapas: Propuesta → Anteproyecto → DT1 → DT2 → Predefensa → Sustentación
    // ══════════════════════════════════════════════════════
    private EstadoEstudianteDto buildIntegracion(Integer idEstudiante, String nombre, String carrera) {
        List<EtapaDto> etapas = new ArrayList<>();
        int completados = 0;

        // ── 1. PROPUESTA ──────────────────────────────────────────────────
        var propuestas = propuestaRepo.findByEstudiante_IdEstudiante(idEstudiante);
        PropuestaTitulacion propuesta = propuestas.isEmpty() ? null
                : propuestas.get(propuestas.size() - 1);

        String ep = "PENDIENTE", dp = "Aún no has enviado una propuesta de tema.";
        if (propuesta != null) {
            String raw = nvl(propuesta.getEstado());
            ep = toEstado(raw);
            dp = switch (raw) {
                case "ENVIADA"     -> "Propuesta enviada, esperando revisión de la comisión.";
                case "EN_REVISION" -> "Tu propuesta está siendo revisada por la comisión.";
                case "APROBADA"    -> "Aprobada" + (propuesta.getFechaRevision() != null
                        ? " el " + propuesta.getFechaRevision() : "") + ".";
                case "RECHAZADA"   -> "Rechazada: " + nvlStr(propuesta.getObservacionesComision(),
                        "revisa las observaciones.");
                default -> "En proceso.";
            };
        }
        etapas.add(new EtapaDto("PROPUESTA", "Propuesta de tema", dp, ep,
                propuesta != null ? propuesta.getFechaEnvio() : null));
        if ("COMPLETADO".equals(ep)) completados++;

        // ── 2. ANTEPROYECTO ───────────────────────────────────────────────
        AnteproyectoTitulacion ante = null;
        if (propuesta != null)
            ante = anteproyectoRepo.findByPropuesta_IdPropuesta(propuesta.getIdPropuesta()).orElse(null);

        String ea = "PENDIENTE", da = "Pendiente de registrar el anteproyecto.";
        if (ante != null) {
            String raw = nvl(ante.getEstado());
            ea = toEstado(raw);
            da = switch (raw) {
                case "BORRADOR"    -> "Anteproyecto guardado como borrador.";
                case "EN_REVISION" -> "En revisión por el director.";
                case "APROBADO"    -> "Aprobado" + (ante.getFechaCreacion() != null
                        ? " — creado el " + ante.getFechaCreacion() : "") + ".";
                case "RECHAZADO"   -> "Rechazado — revisa las observaciones y corrige.";
                default -> "En proceso.";
            };
        }
        etapas.add(new EtapaDto("ANTEPROYECTO", "Anteproyecto", da, ea,
                ante != null ? ante.getFechaCreacion() : null));
        if ("COMPLETADO".equals(ea)) completados++;

        // ── 3. DT1 ────────────────────────────────────────────────────────
        ProyectoTitulacion proyecto = null;
        if (propuesta != null)
            proyecto = proyectoRepo.findByPropuesta_IdPropuesta(propuesta.getIdPropuesta()).orElse(null);

        String edt1 = "PENDIENTE", ddt1 = "Fase DT1 aún no iniciada.";
        if (proyecto != null) {
            String rawProy = nvl(proyecto.getEstado());
            if (List.of("DESARROLLO","PREDEFENSA","DEFENSA","FINALIZADO").contains(rawProy)) {
                edt1 = "COMPLETADO"; ddt1 = "DT1 completado.";
            } else if ("ANTEPROYECTO".equals(rawProy)) {
                edt1 = "EN_CURSO"; ddt1 = "Proyecto activo en etapa anteproyecto/DT1.";
            }
        }
        etapas.add(new EtapaDto("DT1", "Documento de Titulación I (DT1)", ddt1, edt1, null));
        if ("COMPLETADO".equals(edt1)) completados++;

        // ── 4. DT2 ────────────────────────────────────────────────────────
        String edt2 = "PENDIENTE", ddt2 = "Fase DT2 aún no iniciada.";
        if (proyecto != null) {
            boolean tieneDt2 = dt2Repo.existsByProyecto_IdProyecto(proyecto.getIdProyecto());
            if (tieneDt2) { edt2 = "EN_CURSO"; ddt2 = "Director DT2 asignado. Documento en desarrollo."; }
            String rawProy = nvl(proyecto.getEstado());
            if (List.of("PREDEFENSA","DEFENSA","FINALIZADO").contains(rawProy)) {
                edt2 = "COMPLETADO";
                ddt2 = proyecto.getPorcentajeAntiplagio() != null
                        ? "DT2 aprobado. Antiplagio: " + proyecto.getPorcentajeAntiplagio() + "%."
                        : "DT2 aprobado.";
            }
        }
        etapas.add(new EtapaDto("DT2", "Documento de Titulación II (DT2)", ddt2, edt2, null));
        if ("COMPLETADO".equals(edt2)) completados++;

        // ── 5. PREDEFENSA ─────────────────────────────────────────────────
        String epre = "PENDIENTE", dpre = "Predefensa aún no programada.";
        if (proyecto != null) {
            var predef = sustentacionRepo
                    .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(proyecto.getIdProyecto())
                    .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo())).findFirst().orElse(null);
            if (predef != null) {
                epre = "EN_CURSO";
                dpre = "Programada el " + predef.getFecha() + " a las " + predef.getHora()
                        + (predef.getLugar() != null ? " — " + predef.getLugar() : "") + ".";
            }
            if (List.of("DEFENSA","FINALIZADO").contains(nvl(proyecto.getEstado()))) {
                epre = "COMPLETADO"; dpre = "Predefensa superada.";
            }
        }
        etapas.add(new EtapaDto("PREDEFENSA", "Predefensa", dpre, epre, null));
        if ("COMPLETADO".equals(epre)) completados++;

        // ── 6. SUSTENTACIÓN FINAL ─────────────────────────────────────────
        String esust = "PENDIENTE", dsust = "Sustentación final pendiente.";
        if (proyecto != null) {
            var defensa = sustentacionRepo
                    .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(proyecto.getIdProyecto())
                    .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo())).findFirst().orElse(null);
            if (defensa != null) {
                esust = "EN_CURSO";
                dsust = "Programada el " + defensa.getFecha() + " a las " + defensa.getHora()
                        + (defensa.getLugar() != null ? " — " + defensa.getLugar() : "") + ".";
            }
            if ("FINALIZADO".equals(nvl(proyecto.getEstado()))) {
                esust = "COMPLETADO"; dsust = "Proceso de titulación completado exitosamente.";
            }
        }
        etapas.add(new EtapaDto("SUSTENTACION", "Sustentación final", dsust, esust, null));
        if ("COMPLETADO".equals(esust)) completados++;

        int pct = (completados * 100) / etapas.size();
        String etapaActual = etapas.stream()
                .filter(e -> "EN_CURSO".equals(e.getEstado())).map(EtapaDto::getTitulo).findFirst()
                .orElse(etapas.stream().filter(e -> "PENDIENTE".equals(e.getEstado()))
                        .map(EtapaDto::getTitulo).findFirst().orElse("Proceso completado"));

        return new EstadoEstudianteDto(nombre, carrera, "INTEGRACIÓN CURRICULAR", etapaActual, pct, etapas);
    }

    private String nvl(String s)                { return s == null ? "" : s.trim().toUpperCase(); }
    private String nvlStr(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
    private String toEstado(String raw) {
        return switch (raw) {
            case "APROBADA","APROBADO","FINALIZADO"  -> "COMPLETADO";
            case "RECHAZADA","RECHAZADO","REPROBADO" -> "RECHAZADO";
            case "PENDIENTE",""                      -> "PENDIENTE";
            default                                  -> "EN_CURSO";
        };
    }
}