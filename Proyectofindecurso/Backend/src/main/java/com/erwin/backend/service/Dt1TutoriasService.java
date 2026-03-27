package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class Dt1TutoriasService {

    private final Dt1AsignacionRepository asigRepo;
    private final AnteproyectoTitulacionRepository anteRepo;
    private final TutoriaAnteproyectoRepository tutRepo;
    private final ActaRevisionDirectorRepository actaRepo; // (en tu código devuelve ActaRevisionTutor, mantengo tal cual tu repo)
    private final DocenteRepository docenteRepo;
    private final Dt1TutorEstudianteRepository dt1TutorRepo;
    private final ZoomService zoomService;
    private final EmailService emailService;
    private final IaEjemploRepository iaEjemploRepo;

    public Dt1TutoriasService(
            Dt1AsignacionRepository asigRepo,
            AnteproyectoTitulacionRepository anteRepo,
            TutoriaAnteproyectoRepository tutRepo,
            ActaRevisionDirectorRepository actaRepo,
            DocenteRepository docenteRepo,
            Dt1TutorEstudianteRepository dt1TutorRepo,
            ZoomService zoomService,
            EmailService emailService,  IaEjemploRepository iaEjemploRepo
    ) {
        this.asigRepo = asigRepo;
        this.anteRepo = anteRepo;
        this.tutRepo = tutRepo;
        this.actaRepo = actaRepo;
        this.docenteRepo = docenteRepo;
        this.dt1TutorRepo = dt1TutorRepo;
        this.zoomService = zoomService;
        this.emailService = emailService;
        this.iaEjemploRepo  = iaEjemploRepo;
    }

    // ==========================
    // 1) Mis anteproyectos (DT1)
    // ==========================
    @Transactional(readOnly = true)
    public List<MisAnteproyectosDt1Response> misAnteproyectos(Integer idDocente) {

        validarDocenteExiste(idDocente);

        List<Dt1Asignacion> asignaciones = obtenerAsignacionesActivasDt1(idDocente);
        if (asignaciones.isEmpty()) return List.of();

        // Usamos Map para evitar duplicados (si el docente tiene varias asignaciones que repiten estudiantes)
        Map<Integer, MisAnteproyectosDt1Response> mapa = new LinkedHashMap<>();

        for (Dt1Asignacion asig : asignaciones) {
            Integer idPeriodo = asig.getPeriodo().getIdPeriodo();

            // Solo estudiantes tutorados por este docente en ese periodo
            List<Dt1TutorEstudiante> tutorados =
                    dt1TutorRepo.findByDocente_IdDocenteAndPeriodo_IdPeriodoAndActivoTrue(idDocente, idPeriodo);

            for (Dt1TutorEstudiante te : tutorados) {
                Integer idEstudiante = te.getEstudiante().getIdEstudiante();

                // Anteproyectos BORRADOR del estudiante en ese periodo
                List<AnteproyectoTitulacion> anteproyectos =
                        anteRepo.findByEstudiante_IdEstudianteAndEleccion_Periodo_IdPeriodoAndEstadoInIgnoreCase(
                                idEstudiante, idPeriodo, List.of("BORRADOR", "EN_REVISION", "ENVIADO")
                        );

                for (AnteproyectoTitulacion ap : anteproyectos) {
                    mapa.putIfAbsent(ap.getIdAnteproyecto(), mapMisAnteproyectos(ap));
                }
            }
        }

        return new ArrayList<>(mapa.values());
    }

    // ==========================
    // 2) Programar tutoría
    // ==========================
    @Transactional
    public TutoriaResponse programarTutoria(Integer idAnteproyecto, Integer idDocente, TutoriaCreateRequest req) {

        validarReqProgramarTutoria(req);
        String modalidad = validarModalidad(req.getModalidad());

        Docente docente = validarDocenteExiste(idDocente);
        AnteproyectoTitulacion ante = validarAnteproyectoExiste(idAnteproyecto);

        validarAnteproyectoEnBorrador(ante);
        validarPermisoTutor(ante, idDocente);

        TutoriaAnteproyecto t = new TutoriaAnteproyecto();
        t.setAnteproyecto(ante);
        t.setDocente(docente);
        t.setFecha(req.getFecha());
        t.setHora(req.getHora());
        t.setModalidad(modalidad);

        t.setEstado("PROGRAMADA");
        if (modalidad.equals("VIRTUAL")) {
            try {
                String tema = "Tutoria Programada - " + ante.getEstudiante().getUsuario().getNombres();
                ZoomMeetingResult zoom = zoomService.crearReunion(idDocente,tema, req.getFecha(), req.getHora());
                t.setLinkReunion(zoom.joinUrl);
                t.setZoomMeetingId(zoom.meetingId);
            } catch (Exception e) {
                System.out.println("ERROR ZOOM: " + e.getMessage()); // ← agregar
                t.setLinkReunion(null);
                t.setZoomMeetingId(null);
            }
        }

        t = tutRepo.save(t);

        try {
            String correoestudiante = ante.getEstudiante().getUsuario().getCorreoInstitucional();
            emailService.noticarReunion(
                    correoestudiante,
                    "Se ha programado una tutoria con tu tutor",
                    t.getLinkReunion(),
                    t.getZoomMeetingId() != null ? t.getZoomMeetingId() : "Sin ID",
                    t.getFecha().toString(),
                    t.getHora() != null ? t.getHora().toString() : null
            );
        } catch (Exception ignored) {}

        return mapTutoria(t);
    }
    // ==========================
    // 3) Listar tutorías del anteproyecto
    // ==========================
    @Transactional(readOnly = true)
    public List<TutoriaResponse> tutoriasPorAnteproyecto(Integer idAnteproyecto, Integer idDocente) {

        AnteproyectoTitulacion ante = validarAnteproyectoExiste(idAnteproyecto);

        validarAnteproyectoEnBorrador(ante);
        validarPermisoTutor(ante, idDocente); // ✅ DT1 asignado + tutor del estudiante

        // Si quieres SOLO las tutorías creadas por ese docente, crea el método en repo:
        // findByAnteproyecto_IdAnteproyectoAndDocente_IdDocenteOrderByFechaAsc(...)
        // Por ahora mantengo tu método original:
        List<TutoriaAnteproyecto> tutorias =
                tutRepo.findByAnteproyecto_IdAnteproyectoOrderByFechaAsc(idAnteproyecto);

        List<TutoriaResponse> salida = new ArrayList<>();
        for (TutoriaAnteproyecto t : tutorias) {
            salida.add(mapTutoria(t));
        }
        return salida;
    }

    // ==========================
    // 4) Cancelar tutoría
    // ==========================
    @Transactional
    public TutoriaResponse cancelarTutoria(Integer idTutoria, Integer idDocente) {

        TutoriaAnteproyecto t = validarTutoriaExiste(idTutoria);

        // Debe ser el dueño de la tutoría (la creó)
        validarDocenteEsDuenioDeTutoria(t, idDocente);

        // ✅ además debe tener permiso (DT1 + tutor del estudiante)
        validarPermisoTutor(t.getAnteproyecto(), idDocente);

        validarTutoriaProgramadaParaCancelar(t);

        t.setEstado("CANCELADA");
        t = tutRepo.save(t);

        return mapTutoria(t);
    }

    // ==========================
    // 5) Obtener acta (si existe)
    // ==========================
    @Transactional(readOnly = true)
    public ActaRevisionTutorResponse obtenerActa(Integer idTutoria, Integer idDocente) {

        TutoriaAnteproyecto t = validarTutoriaExiste(idTutoria);

        // Debe ser el dueño de la tutoría
        validarDocenteEsDuenioDeTutoria(t, idDocente);

        // ✅ además debe tener permiso (DT1 + tutor del estudiante)
        validarPermisoTutor(t.getAnteproyecto(), idDocente);

        ActaRevisionTutor acta = actaRepo.findByTutoria_IdTutoria(idTutoria)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTA_NO_EXISTE"));

        return mapActa(acta, t);
    }

    // ==========================
    // 6) Guardar / editar acta
    // ==========================
    @Transactional
    public ActaRevisionTutorResponse guardarActa(Integer idTutoria, Integer idDocente, ActaRevisionTutorRequest req) {
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");

        TutoriaAnteproyecto t = validarTutoriaExiste(idTutoria);
        validarDocenteEsDuenioDeTutoria(t, idDocente);
        validarPermisoTutor(t.getAnteproyecto(), idDocente);
        validarAnteproyectoEnBorrador(t.getAnteproyecto());
        validarTutoriaParaGuardarActa(t);
        validarCamposMinimosActa(req);

        ActaRevisionTutor acta = actaRepo.findByTutoria_IdTutoria(idTutoria).orElse(new ActaRevisionTutor());
        llenarActa(acta, t, req);
        acta = actaRepo.save(acta);

        t.setEstado("REALIZADA");
        tutRepo.save(t);

        // ── Guardar en ia_ejemplos ──
        try {
            AnteproyectoTitulacion ante = anteRepo.findById(t.getAnteproyecto().getIdAnteproyecto()).orElse(null);
            if(ante != null) {
                IaEjemplo ej = new IaEjemplo();
                ej.setIdEstudiante(ante.getEstudiante().getIdEstudiante());
                ej.setSeccion("tutoria");
                ej.setContenido(
                        "TÍTULO: "            + textoSeguro(req.getTituloProyecto()) +
                                "\nOBJETIVO: "        + textoSeguro(req.getObjetivo()) +
                                "\nDETALLE REVISIÓN: "+ textoSeguro(req.getDetalleRevision())
                );
                ej.setDecision(textoSeguro(req.getCumplimiento()));
                ej.setObservacion(
                        textoSeguro(req.getObservaciones()) + " " + textoSeguro(req.getConclusion())
                );
                ej.setFuente("TUTORIA");
                iaEjemploRepo.save(ej);
            }
        } catch (Exception ignored) {
            // Silenciamos errores de IA para que no detengan el guardado del acta
        }

        return mapActa(acta, t);
    }
    @Transactional(readOnly = true)
    public List<TutoriaCalendarioResponse> calendarioTutorias(Integer idDocente) {
        validarDocenteExiste(idDocente);
        return tutRepo.findByDocente_IdDocenteOrderByFechaDesc(idDocente)
                .stream()
                .map(t -> {
                    TutoriaCalendarioResponse r = new TutoriaCalendarioResponse();
                    r.setIdTutoria(t.getIdTutoria());
                    r.setFecha(t.getFecha());
                    r.setHora(t.getHora());
                    r.setModalidad(t.getModalidad());
                    r.setEstado(t.getEstado());
                    r.setLinkReunion(t.getLinkReunion());
                    r.setEstudianteNombre(nombreCompletoEstudiante(t.getAnteproyecto().getEstudiante()));
                    r.setTituloProyecto(textoSeguro(t.getAnteproyecto().getPropuesta().getTitulo()));
                    return r;
                })
                .toList();
    }
    @Transactional(readOnly = true)
    public List<ReporteAsistenciaResponse> reporteAsistencia(Integer idDocente) {
        validarDocenteExiste(idDocente);

        List<TutoriaAnteproyecto> realizadas = tutRepo
                .findByDocente_IdDocenteAndEstadoOrderByFechaDesc(idDocente, "REALIZADA");

        Map<Integer, ReporteAsistenciaResponse> mapa = new LinkedHashMap<>();

        for (TutoriaAnteproyecto t : realizadas) {
            Estudiante est = t.getAnteproyecto().getEstudiante();
            Integer idEst = est.getIdEstudiante();

            ReporteAsistenciaResponse r = mapa.getOrDefault(idEst, new ReporteAsistenciaResponse());
            r.setIdEstudiante(idEst);
            r.setEstudiante(nombreCompletoEstudiante(est));
            String titulo = "";
            if (t.getAnteproyecto() != null && t.getAnteproyecto().getPropuesta() != null) {
                titulo = textoSeguro(t.getAnteproyecto().getPropuesta().getTitulo());
            }
            r.setTituloProyecto(titulo);
            r.setTotalTutorias(r.getTotalTutorias() + 1);

            ReporteAsistenciaResponse.TutoriaItem item = new ReporteAsistenciaResponse.TutoriaItem();
            item.setIdTutoria(t.getIdTutoria());
            item.setFecha(t.getFecha());
            item.setHora(t.getHora());
            item.setModalidad(t.getModalidad());

            r.getTutorias().add(item);
            mapa.put(idEst, r);
        }

        return new ArrayList<>(mapa.values());
    }

    // ==========================================================
    // ===================== VALIDACIONES =======================
    // ==========================================================

    private void validarPermisoTutor(AnteproyectoTitulacion ante, Integer idDocente) {
        validarDocenteDt1Asignado(ante, idDocente);              // está asignado como DT1 en esa carrera+periodo
        validarDocenteEsTutorDelAnteproyecto(ante, idDocente);   // es tutor del estudiante en ese periodo
    }

    private void validarReqProgramarTutoria(TutoriaCreateRequest req) {
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        if (req.getFecha() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FECHA_REQUERIDA");
        // si tu DTO trae hora obligatoria, descomenta:
        // if (req.getHora() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HORA_REQUERIDA");
    }

    private String validarModalidad(String modalidad) {
        String m = textoSeguro(modalidad).toUpperCase();
        if (!m.equals("PRESENCIAL") && !m.equals("VIRTUAL")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODALIDAD_INVALIDA");
        }
        return m;
    }

    private Docente validarDocenteExiste(Integer idDocente) {
        if (idDocente == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");
        return docenteRepo.findById(idDocente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE_NO_EXISTE"));
    }

    private AnteproyectoTitulacion validarAnteproyectoExiste(Integer idAnteproyecto) {
        if (idAnteproyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        return anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANTEPROYECTO_NO_EXISTE"));
    }

    private TutoriaAnteproyecto validarTutoriaExiste(Integer idTutoria) {
        if (idTutoria == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_TUTORIA_REQUERIDO");
        return tutRepo.findById(idTutoria)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TUTORIA_NO_EXISTE"));
    }

    private void validarDocenteEsDuenioDeTutoria(TutoriaAnteproyecto t, Integer idDocente) {
        if (t.getDocente() == null || !t.getDocente().getIdDocente().equals(idDocente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_PERMITIDO");
        }
    }

    private void validarAnteproyectoEnBorrador(AnteproyectoTitulacion ante) {
        String estado = textoSeguro(ante.getEstado()).toUpperCase();
        if (!estado.equals("BORRADOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLO_BORRADOR_PARA_TUTORIAS");
        }
    }

    private void validarDocenteEsTutorDelAnteproyecto(AnteproyectoTitulacion ante, Integer idDocente) {
        Integer idEstudiante = ante.getEstudiante().getIdEstudiante();
        Integer idPeriodo = ante.getEleccion().getPeriodo().getIdPeriodo();

        boolean esTutor = dt1TutorRepo.existsByDocente_IdDocenteAndEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                idDocente, idEstudiante, idPeriodo
        );

        if (!esTutor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO ES TUTOR DEL ESTUDIANTE");
        }
    }

    private void validarDocenteDt1Asignado(AnteproyectoTitulacion ante, Integer idDocente) {

        List<Dt1Asignacion> asignaciones = obtenerAsignacionesActivasDt1(idDocente);
        if (asignaciones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ES_DT1_ASIGNADO");
        }

        Integer idCarreraAnte = ante.getCarrera().getIdCarrera();
        Integer idPeriodoAnte = ante.getEleccion().getPeriodo().getIdPeriodo();

        boolean coincide = false;
        for (Dt1Asignacion a : asignaciones) {
            Integer idCarrera = a.getCarrera().getIdCarrera();
            Integer idPeriodo = a.getPeriodo().getIdPeriodo();
            if (idCarrera.equals(idCarreraAnte) && idPeriodo.equals(idPeriodoAnte)) {
                coincide = true;
                break;
            }
        }

        if (!coincide) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ES_DT1_ASIGNADO");
        }
    }

    private void validarTutoriaProgramadaParaCancelar(TutoriaAnteproyecto t) {
        String estado = textoSeguro(t.getEstado()).toUpperCase();
        if (!estado.equals("PROGRAMADA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLO_PROGRAMADA_SE_PUEDE_CANCELAR");
        }
    }

    private void validarTutoriaParaGuardarActa(TutoriaAnteproyecto t) {
        String estado = textoSeguro(t.getEstado()).toUpperCase();

        if (estado.equals("CANCELADA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_SE_PUEDE_GUARDAR_EN_CANCELADA");
        }
        if (!estado.equals("PROGRAMADA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOLO_PROGRAMADA_SE_PUEDE_GUARDAR");
        }
    }

    private void validarCamposMinimosActa(ActaRevisionTutorRequest req) {
        if (textoVacio(req.getTituloProyecto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TITULO_REQUERIDO");
        }
        if (textoVacio(req.getObjetivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OBJETIVO_REQUERIDO");
        }
        if (textoVacio(req.getDetalleRevision())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DETALLE_REVISION_REQUERIDO");
        }
    }

    private List<Dt1Asignacion> obtenerAsignacionesActivasDt1(Integer idDocente) {
        return asigRepo.findByDocente_IdDocenteAndActivoTrue(idDocente);
    }

    // ==========================================================
    // ======================= MAPEOS ===========================
    // ==========================================================

    private MisAnteproyectosDt1Response mapMisAnteproyectos(AnteproyectoTitulacion ap) {
        MisAnteproyectosDt1Response dto = new MisAnteproyectosDt1Response();
        dto.setIdAnteproyecto(ap.getIdAnteproyecto());
        dto.setTituloProyecto(textoSeguro(ap.getPropuesta().getTitulo()));
        dto.setEstudianteNombre(nombreCompletoEstudiante(ap.getEstudiante()));
        dto.setPeriodo(textoSeguro(ap.getEleccion().getPeriodo().getDescripcion()));
        dto.setEstadoAnteproyecto(textoSeguro(ap.getEstado()));
        return dto;
    }

    private TutoriaResponse mapTutoria(TutoriaAnteproyecto t) {
        TutoriaResponse r = new TutoriaResponse();
        r.setIdTutoria(t.getIdTutoria());
        r.setFecha(t.getFecha());
        r.setHora(t.getHora());
        r.setModalidad(t.getModalidad());
        r.setEstado(t.getEstado());
        r.setLinkReunion(t.getLinkReunion());  // ← ¿tienes esta línea?
        return r;
    }

    private ActaRevisionTutorResponse mapActa(ActaRevisionTutor acta, TutoriaAnteproyecto t) {
        ActaRevisionTutorResponse r = new ActaRevisionTutorResponse();

        r.setIdActa(acta.getIdActa());
        r.setIdTutoria(t.getIdTutoria());

        r.setDirectorCargo(acta.getDirectorCargo());
        r.setDirectorFirma(acta.getDirectorFirma());
        r.setEstudianteCargo(acta.getEstudianteCargo());
        r.setEstudianteFirma(acta.getEstudianteFirma());

        r.setTituloProyecto(acta.getTituloProyecto());
        r.setObjetivo(acta.getObjetivo());
        r.setDetalleRevision(acta.getDetalleRevision());
        r.setCumplimiento(acta.getCumplimiento());
        r.setObservaciones(acta.getObservaciones());
        r.setConclusion(acta.getConclusion());

        r.setDirectorNombre(nombreCompletoDocente(t.getDocente()));
        r.setEstudianteNombre(nombreCompletoEstudiante(t.getAnteproyecto().getEstudiante()));

        return r;
    }

    private void llenarActa(ActaRevisionTutor acta, TutoriaAnteproyecto t, ActaRevisionTutorRequest req) {

        acta.setTutoria(t);

        acta.setDirectorNombre(nombreCompletoDocente(t.getDocente()));
        acta.setEstudianteNombre(nombreCompletoEstudiante(t.getAnteproyecto().getEstudiante()));

        acta.setDirectorCargo(textoSeguro(req.getDirectorCargo()));
        acta.setDirectorFirma(textoSeguro(req.getDirectorFirma()));
        acta.setEstudianteCargo(textoSeguro(req.getEstudianteCargo()));
        acta.setEstudianteFirma(textoSeguro(req.getEstudianteFirma()));

        acta.setTituloProyecto(textoSeguro(req.getTituloProyecto()));
        acta.setObjetivo(textoSeguro(req.getObjetivo()));
        acta.setDetalleRevision(textoSeguro(req.getDetalleRevision()));
        acta.setCumplimiento(textoSeguro(req.getCumplimiento()));
        acta.setObservaciones(textoSeguro(req.getObservaciones()));
        acta.setConclusion(textoSeguro(req.getConclusion()));
    }

    // ==========================================================
    // ======================= HELPERS ==========================
    // ==========================================================

    private boolean textoVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String textoSeguro(String s) {
        return s == null ? "" : s.trim();
    }

    private String nombreCompletoDocente(Docente d) {
        if (d == null || d.getUsuario() == null) return "";
        return (textoSeguro(d.getUsuario().getNombres()) + " " + textoSeguro(d.getUsuario().getApellidos())).trim();
    }

    private String nombreCompletoEstudiante(Estudiante e) {
        if (e == null || e.getUsuario() == null) return "";
        return (textoSeguro(e.getUsuario().getNombres()) + " " + textoSeguro(e.getUsuario().getApellidos())).trim();
    }
}