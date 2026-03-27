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

    public EstadoEstudianteController(
            EstudianteRepository estudianteRepo,
            PropuestaTitulacionRepository propuestaRepo,
            AnteproyectoTitulacionRepository anteproyectoRepo,
            ProyectoTitulacionRepository proyectoRepo,
            Dt2AsignacionRepository dt2Repo,
            SustentacionRepository sustentacionRepo) {
        this.estudianteRepo   = estudianteRepo;
        this.propuestaRepo    = propuestaRepo;
        this.anteproyectoRepo = anteproyectoRepo;
        this.proyectoRepo     = proyectoRepo;
        this.dt2Repo          = dt2Repo;
        this.sustentacionRepo = sustentacionRepo;
    }

    @GetMapping("/{idEstudiante}")
    public ResponseEntity<EstadoEstudianteDto> obtenerEstado(@PathVariable Integer idEstudiante) {

        Estudiante est = estudianteRepo.findById(idEstudiante).orElse(null);
        if (est == null) return ResponseEntity.notFound().build();

        List<EtapaDto> etapas = new ArrayList<>();
        int completados = 0;

        // ── 1. PROPUESTA ──────────────────────────────────────────────────────
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

        // ── 2. ANTEPROYECTO ───────────────────────────────────────────────────
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

        // ── 3. DT1 ────────────────────────────────────────────────────────────
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

        // ── 4. DT2 ────────────────────────────────────────────────────────────
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

        // ── 5. PREDEFENSA ─────────────────────────────────────────────────────
        String epre = "PENDIENTE", dpre = "Predefensa aún no programada.";
        if (proyecto != null) {
            var preList = sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(proyecto.getIdProyecto());
            var predef  = preList.stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo())).findFirst().orElse(null);
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

        // ── 6. SUSTENTACIÓN FINAL ─────────────────────────────────────────────
        String esust = "PENDIENTE", dsust = "Sustentación final pendiente.";
        if (proyecto != null) {
            var defList = sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(proyecto.getIdProyecto());
            var defensa = defList.stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo())).findFirst().orElse(null);
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

        // ── Resumen ───────────────────────────────────────────────────────────
        int pct = (completados * 100) / etapas.size();
        String etapaActual = etapas.stream()
                .filter(e -> "EN_CURSO".equals(e.getEstado())).map(EtapaDto::getTitulo).findFirst()
                .orElse(etapas.stream().filter(e -> "PENDIENTE".equals(e.getEstado()))
                        .map(EtapaDto::getTitulo).findFirst().orElse("Proceso completado"));

        String nombre  = est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos();
        String carrera = est.getCarrera() != null ? est.getCarrera().getNombre() : "Sin carrera";

        return ResponseEntity.ok(new EstadoEstudianteDto(nombre, carrera, "PROYECTO", etapaActual, pct, etapas));
    }

    private String nvl(String s)                  { return s == null ? "" : s.trim().toUpperCase(); }
    private String nvlStr(String s, String def)   { return (s == null || s.isBlank()) ? def : s; }
    private String toEstado(String raw) {
        return switch (raw) {
            case "APROBADA","APROBADO","FINALIZADO"    -> "COMPLETADO";
            case "RECHAZADA","RECHAZADO","REPROBADO"   -> "RECHAZADO";
            case "PENDIENTE",""                        -> "PENDIENTE";
            default                                    -> "EN_CURSO";
        };
    }
}