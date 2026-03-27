package com.erwin.backend.controller;

import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificacionController {

    public record NotificacionDto(
            String tipo,       // EXITO | ALERTA | INFO
            String titulo,
            String mensaje,
            String fecha
    ) {}

    private final EstudianteRepository             estudianteRepo;
    private final PropuestaTitulacionRepository    propuestaRepo;
    private final AnteproyectoTitulacionRepository anteproyectoRepo;
    private final ProyectoTitulacionRepository     proyectoRepo;
    private final SustentacionRepository           sustentacionRepo;
    private final ComplexivoTitulacionRepository   complexivoRepo;
    private final ComplexivoInformePracticoRepository informeRepo;
    private final PeriodoTitulacionRepository      periodoRepo;
    private final ObservacionDocumentoRepository   observacionRepo;
    private final TutoriaAnteproyectoRepository    tutoriaRepo;

    public NotificacionController(
            EstudianteRepository estudianteRepo,
            PropuestaTitulacionRepository propuestaRepo,
            AnteproyectoTitulacionRepository anteproyectoRepo,
            ProyectoTitulacionRepository proyectoRepo,
            SustentacionRepository sustentacionRepo,
            ComplexivoTitulacionRepository complexivoRepo,
            ComplexivoInformePracticoRepository informeRepo,
            PeriodoTitulacionRepository periodoRepo,
            ObservacionDocumentoRepository observacionRepo,
            TutoriaAnteproyectoRepository tutoriaRepo) {
        this.estudianteRepo   = estudianteRepo;
        this.propuestaRepo    = propuestaRepo;
        this.anteproyectoRepo = anteproyectoRepo;
        this.proyectoRepo     = proyectoRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.complexivoRepo   = complexivoRepo;
        this.informeRepo      = informeRepo;
        this.periodoRepo      = periodoRepo;
        this.observacionRepo  = observacionRepo;
        this.tutoriaRepo      = tutoriaRepo;
    }

    @GetMapping("/{idEstudiante}")
    public ResponseEntity<List<NotificacionDto>> obtener(@PathVariable Integer idEstudiante) {
        Estudiante est = estudianteRepo.findById(idEstudiante).orElse(null);
        if (est == null) return ResponseEntity.notFound().build();

        List<NotificacionDto> lista = new ArrayList<>();
        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc().orElse(null);

        // ── Detectar si es complexivo ──────────────────────────────────────
        boolean esComplexivo = false;
        if (periodo != null) {
            esComplexivo = complexivoRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                    idEstudiante, periodo.getIdPeriodo()).isPresent();
        }

        // ── PROPUESTA ──────────────────────────────────────────────────────
        var propuestas = propuestaRepo.findByEstudiante_IdEstudiante(idEstudiante);
        if (!propuestas.isEmpty()) {
            var p = propuestas.get(propuestas.size() - 1);
            String estado = nvl(p.getEstado());
            if ("APROBADA".equals(estado)) {
                lista.add(new NotificacionDto("EXITO", "Propuesta aprobada",
                        "Tu propuesta de tema fue aprobada por la comisión.",
                        p.getFechaRevision() != null ? p.getFechaRevision().toString() : ""));
            } else if ("RECHAZADA".equals(estado)) {
                lista.add(new NotificacionDto("ALERTA", "Propuesta rechazada",
                        "Tu propuesta fue rechazada. Revisa las observaciones y corrígela.",
                        p.getFechaRevision() != null ? p.getFechaRevision().toString() : ""));
            } else if ("ENVIADA".equals(estado) || "EN_REVISION".equals(estado)) {
                lista.add(new NotificacionDto("INFO", "Propuesta en revisión",
                        "Tu propuesta está siendo evaluada por la comisión.",
                        p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : ""));
            }

            // ── ANTEPROYECTO (solo Integración Curricular) ─────────────────
            if (!esComplexivo) {
                anteproyectoRepo.findByPropuesta_IdPropuesta(p.getIdPropuesta()).ifPresent(ante -> {
                    String est2 = nvl(ante.getEstado());
                    if ("APROBADO".equals(est2)) {
                        lista.add(new NotificacionDto("EXITO", "Anteproyecto aprobado",
                                "Tu anteproyecto fue aprobado por el director.",
                                ante.getFechaCreacion() != null ? ante.getFechaCreacion().toString() : ""));
                    } else if ("RECHAZADO".equals(est2)) {
                        lista.add(new NotificacionDto("ALERTA", "Anteproyecto rechazado",
                                "Tu anteproyecto tiene observaciones. Revísalo y corrígelo.",
                                ""));
                    } else if ("EN_REVISION".equals(est2)) {
                        lista.add(new NotificacionDto("INFO", "Anteproyecto en revisión",
                                "Tu director está revisando tu anteproyecto.",
                                ""));
                    }

                    // Tutorías programadas pendientes
                    var tutorias = tutoriaRepo.findByAnteproyecto_Estudiante_IdEstudianteAndEstado(
                            idEstudiante, "PROGRAMADA");
                    if (!tutorias.isEmpty()) {
                        lista.add(new NotificacionDto("INFO", "Tutoría programada",
                                "Tienes " + tutorias.size() + " tutoría(s) pendiente(s) con tu director.",
                                ""));
                    }
                });

                // Observaciones de documento pendientes
                proyectoRepo.findByPropuesta_IdPropuesta(p.getIdPropuesta()).ifPresent(proy -> {
                    String estProy = nvl(proy.getEstado());
                    if ("PREDEFENSA".equals(estProy)) {
                        lista.add(new NotificacionDto("ALERTA", "Predefensa próxima",
                                "Tienes una predefensa programada. Verifica fecha y lugar en el sistema.",
                                ""));
                    } else if ("DEFENSA".equals(estProy)) {
                        lista.add(new NotificacionDto("ALERTA", "Sustentación final próxima",
                                "Tu sustentación final está programada. ¡Prepárate!",
                                ""));
                    }
                });
            }
        }

        // ── COMPLEXIVO ─────────────────────────────────────────────────────
        if (esComplexivo && periodo != null) {
            complexivoRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                    idEstudiante, periodo.getIdPeriodo()).ifPresent(ct -> {

                informeRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo()).ifPresent(inf -> {
                    String estInf = nvl(inf.getEstado());
                    if ("APROBADO".equals(estInf)) {
                        lista.add(new NotificacionDto("EXITO", "Informe práctico aprobado",
                                "Tu informe práctico fue aprobado por el docente DT2.",
                                inf.getFechaRevision() != null ? inf.getFechaRevision().toString() : ""));
                    } else if ("RECHAZADO".equals(estInf)) {
                        lista.add(new NotificacionDto("ALERTA", "Informe rechazado",
                                "Tu informe tiene observaciones: " +
                                        (inf.getObservaciones() != null ? inf.getObservaciones() : "revísalo."),
                                inf.getFechaRevision() != null ? inf.getFechaRevision().toString() : ""));
                    } else if ("ENTREGADO".equals(estInf) || "EN_REVISION".equals(estInf)) {
                        lista.add(new NotificacionDto("INFO", "Informe en revisión",
                                "Tu informe práctico está siendo revisado por el docente DT2.",
                                inf.getFechaEntrega() != null ? inf.getFechaEntrega().toString() : ""));
                    } else if ("BORRADOR".equals(estInf)) {
                        lista.add(new NotificacionDto("INFO", "Informe sin enviar",
                                "Tienes un informe práctico en borrador. Complétalo y envíalo.",
                                ""));
                    }
                });

                String estCt = nvl(ct.getEstado());
                if ("APROBADO".equals(estCt)) {
                    lista.add(new NotificacionDto("EXITO", "¡Proceso completado!",
                            "Aprobaste el examen complexivo. ¡Felicitaciones!",
                            ct.getFechaInscripcion() != null ? ct.getFechaInscripcion().toString() : ""));
                } else if ("REPROBADO".equals(estCt)) {
                    lista.add(new NotificacionDto("ALERTA", "Examen reprobado",
                            "Reprobaste el examen complexivo. Consulta con tu coordinador.",
                            ""));
                }
            });
        }

        // Si no hay nada, mensaje de bienvenida
        if (lista.isEmpty()) {
            lista.add(new NotificacionDto("INFO", "Sin novedades",
                    "No tienes notificaciones nuevas.", ""));
        }

        return ResponseEntity.ok(lista);
    }

    private String nvl(String s) { return s == null ? "" : s.trim().toUpperCase(); }
}