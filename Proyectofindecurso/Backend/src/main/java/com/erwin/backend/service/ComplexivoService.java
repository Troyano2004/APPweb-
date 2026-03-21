package com.erwin.backend.service;

import com.erwin.backend.dtos.ComplexivoDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComplexivoService {

    private static final String NOMBRE_MODALIDAD_COMPLEXIVO = "Examen Complexivo";

    private final ComplexivoDocenteAsignacionRepository asignacionRepo;   // tabla vieja — mantener
    private final ComplexivoDt1AsignacionRepository     dt1Repo;          // NUEVA tabla DT1
    private final ComplexivoDt2AsignacionRepository     dt2Repo;          // NUEVA tabla DT2
    private final ComplexivoTitulacionRepository        complexivoRepo;
    private final ComplexivoInformePracticoRepository   informeRepo;
    private final ComplexivoTutoriaRepository           complexivoTutoriaRepo;
    private final EstudianteRepository                  estudianteRepo;
    private final DocenteRepository                     docenteRepo;
    private final DocenteCarreraRepository              docenteCarreraRepo;
    private final UsuarioRepository                     usuarioRepo;
    private final CoordinadorRepository                 coordinadorRepo;
    private final PeriodoTitulacionRepository           periodoRepo;
    private final EleccionTitulacionRepository          eleccionRepo;
    private final PropuestaTitulacionRepository         propuestaRepo;

    public ComplexivoService(
            ComplexivoDocenteAsignacionRepository asignacionRepo,
            ComplexivoDt1AsignacionRepository dt1Repo,
            ComplexivoDt2AsignacionRepository dt2Repo,
            ComplexivoTitulacionRepository complexivoRepo,
            ComplexivoInformePracticoRepository informeRepo,
            ComplexivoTutoriaRepository complexivoTutoriaRepo,
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo,
            DocenteCarreraRepository docenteCarreraRepo,
            UsuarioRepository usuarioRepo,
            CoordinadorRepository coordinadorRepo,
            PeriodoTitulacionRepository periodoRepo,
            EleccionTitulacionRepository eleccionRepo,
            PropuestaTitulacionRepository propuestaRepo) {
        this.asignacionRepo        = asignacionRepo;
        this.dt1Repo               = dt1Repo;
        this.dt2Repo               = dt2Repo;
        this.complexivoRepo        = complexivoRepo;
        this.informeRepo           = informeRepo;
        this.complexivoTutoriaRepo = complexivoTutoriaRepo;
        this.estudianteRepo        = estudianteRepo;
        this.docenteRepo           = docenteRepo;
        this.docenteCarreraRepo    = docenteCarreraRepo;
        this.usuarioRepo           = usuarioRepo;
        this.coordinadorRepo       = coordinadorRepo;
        this.periodoRepo           = periodoRepo;
        this.eleccionRepo          = eleccionRepo;
        this.propuestaRepo         = propuestaRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — info DT1 (para asignar docente de Titulación I)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public InfoCoordinadorDt1Dto infoCoordinadorDt1(Integer idUsuario) {
        PeriodoTitulacion periodo = getPeriodoActivo();
        Carrera carrera = getCarreraCoordinador(idUsuario);
        List<DocenteOpcionDto> docentes = getTodosDocentes();

        List<EstudianteComplexivoSinDocenteDto> sinDocente = eleccionRepo
                .findByCarrera_IdCarreraAndModalidad_NombreAndPeriodo_IdPeriodoAndEstado(
                        carrera.getIdCarrera(), NOMBRE_MODALIDAD_COMPLEXIVO,
                        periodo.getIdPeriodo(), "ACTIVA")
                .stream()
                .filter(e -> !dt1Repo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        e.getEstudiante().getIdEstudiante(), periodo.getIdPeriodo()))
                .map(e -> toEstudianteSinDocente(e.getEstudiante(), carrera, periodo))
                .toList();

        List<ComplexivoDocenteAsignacionResponse> actuales = dt1Repo
                .findByCarreraAndPeriodoActivo(carrera.getIdCarrera(), periodo.getIdPeriodo())
                .stream().map(a -> toDt1AsignacionResponse(a)).toList();

        return new InfoCoordinadorDt1Dto(
                carrera.getIdCarrera(), carrera.getNombre(),
                periodo.getIdPeriodo(), periodo.getDescripcion(),
                docentes, sinDocente, actuales);
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — info DT2 (para asignar docente de Titulación II)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public InfoCoordinadorDt2Dto infoCoordinadorDt2(Integer idUsuario) {
        PeriodoTitulacion periodo = getPeriodoActivo();
        Carrera carrera = getCarreraCoordinador(idUsuario);
        List<DocenteOpcionDto> docentes = getTodosDocentes();

        // Solo mostrar estudiantes cuya propuesta fue APROBADA (tienen complexivo_titulacion)
        List<EstudianteComplexivoSinDocenteDto> sinDocente = complexivoRepo
                .findAll().stream()
                .filter(ct -> ct.getPeriodo().getIdPeriodo().equals(periodo.getIdPeriodo())
                        && ct.getCarrera().getIdCarrera().equals(carrera.getIdCarrera())
                        && !dt2Repo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        ct.getEstudiante().getIdEstudiante(), periodo.getIdPeriodo()))
                .map(ct -> {
                    Estudiante est = ct.getEstudiante();
                    String nombre = est.getUsuario().getNombres()
                            + " " + est.getUsuario().getApellidos();
                    return new EstudianteComplexivoSinDocenteDto(
                            est.getIdEstudiante(), nombre,
                            carrera.getNombre(), NOMBRE_MODALIDAD_COMPLEXIVO,
                            ct.getEstado());
                }).toList();

        List<ComplexivoDocenteAsignacionResponse> actuales = dt2Repo
                .findByCarreraAndPeriodoActivo(carrera.getIdCarrera(), periodo.getIdPeriodo())
                .stream().map(a -> toDt2AsignacionResponse(a)).toList();

        return new InfoCoordinadorDt2Dto(
                carrera.getIdCarrera(), carrera.getNombre(),
                periodo.getIdPeriodo(), periodo.getDescripcion(),
                docentes, sinDocente, actuales);
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — asignar DT1
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoDocenteAsignacionResponse asignarDt1(AsignarDt1ComplexivoRequest req) {
        PeriodoTitulacion periodo = getPeriodoActivo();

        if (dt1Repo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                req.idEstudiante(), periodo.getIdPeriodo()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ESTUDIANTE_YA_TIENE_DOCENTE_DT1_ASIGNADO");

        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));
        Docente docente = docenteRepo.findById(req.idDocente())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO"));
        Usuario asignadoPor = usuarioRepo.findById(req.idUsuarioCoordinador())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));
        EleccionTitulacion eleccion = eleccionRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        req.idEstudiante(), periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "ESTUDIANTE_SIN_ELECCION_EN_PERIODO"));

        ComplexivoDt1Asignacion asig = new ComplexivoDt1Asignacion();
        asig.setEstudiante(estudiante);
        asig.setDocente(docente);
        asig.setPeriodo(periodo);
        asig.setCarrera(eleccion.getCarrera());
        asig.setAsignadoPor(asignadoPor);
        asig.setObservacion(req.observacion());
        asig.setActivo(true);
        return toDt1AsignacionResponse(dt1Repo.save(asig));
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — asignar DT2
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoDocenteAsignacionResponse asignarDt2(AsignarDt2ComplexivoRequest req) {
        PeriodoTitulacion periodo = getPeriodoActivo();

        if (dt2Repo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                req.idEstudiante(), periodo.getIdPeriodo()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ESTUDIANTE_YA_TIENE_DOCENTE_DT2_ASIGNADO");

        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));
        Docente docente = docenteRepo.findById(req.idDocente())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO"));
        Usuario asignadoPor = usuarioRepo.findById(req.idUsuarioCoordinador())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        // Para DT2 usamos complexivo_titulacion para la carrera
        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        req.idEstudiante(), periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "ESTUDIANTE_SIN_REGISTRO_COMPLEXIVO_PROPUESTA_NO_APROBADA"));

        ComplexivoDt2Asignacion asig = new ComplexivoDt2Asignacion();
        asig.setEstudiante(estudiante);
        asig.setDocente(docente);
        asig.setPeriodo(periodo);
        asig.setCarrera(ct.getCarrera());
        asig.setAsignadoPor(asignadoPor);
        asig.setObservacion(req.observacion());
        asig.setActivo(true);
        return toDt2AsignacionResponse(dt2Repo.save(asig));
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — estado
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public EstadoComplexivoEstudianteDto estadoEstudiante(Integer idEstudiante) {
        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc().orElse(null);
        if (periodo == null)
            return new EstadoComplexivoEstudianteDto(
                    false, null, null, false, null, null, false, null);

        var ctOpt = complexivoRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                idEstudiante, periodo.getIdPeriodo());
        if (ctOpt.isEmpty())
            return new EstadoComplexivoEstudianteDto(
                    false, null, null, false, null, null, false, null);

        ComplexivoTitulacion ct = ctOpt.get();
        var informeOpt = informeRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo());

        // Para el estudiante mostramos el docente DT2 (quien revisa el informe)
        var dt2Opt = dt2Repo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                idEstudiante, periodo.getIdPeriodo());

        return new EstadoComplexivoEstudianteDto(
                true, ct.getIdComplexivo(), ct.getEstado(),
                informeOpt.isPresent(),
                informeOpt.map(ComplexivoInformePractico::getIdInforme).orElse(null),
                informeOpt.map(ComplexivoInformePractico::getEstado).orElse(null),
                dt2Opt.isPresent(),
                dt2Opt.map(a -> a.getDocente().getUsuario().getNombres()
                        + " " + a.getDocente().getUsuario().getApellidos()).orElse(null));
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — obtener informe
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoInformeDto getInforme(Integer idEstudiante) {
        PeriodoTitulacion periodo = getPeriodoActivo();

        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NO_TIENES_REGISTRO_COMPLEXIVO_EN_PERIODO_ACTIVO"));

        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(ct.getIdComplexivo())
                .orElseGet(() -> {
                    ComplexivoInformePractico nuevo = new ComplexivoInformePractico();
                    nuevo.setComplexivo(ct);
                    nuevo.setTitulo("");
                    nuevo.setPlanteamientoProblema("");
                    nuevo.setObjetivos("");
                    nuevo.setEstado("BORRADOR");
                    return informeRepo.save(nuevo);
                });

        return toInformeDto(informe, idEstudiante, periodo.getIdPeriodo());
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — guardar informe
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoInformeDto guardarInforme(Integer idEstudiante,
                                               ComplexivoInformeUpdateRequest req) {
        PeriodoTitulacion periodo = getPeriodoActivo();
        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NO_TIENES_REGISTRO_COMPLEXIVO"));

        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(ct.getIdComplexivo())
                .orElseGet(() -> {
                    ComplexivoInformePractico n = new ComplexivoInformePractico();
                    n.setComplexivo(ct); n.setEstado("BORRADOR"); return n;
                });

        if (!"BORRADOR".equals(informe.getEstado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "INFORME_NO_EDITABLE_EN_ESTADO_" + informe.getEstado());

        informe.setTitulo(req.titulo());
        informe.setPlanteamientoProblema(req.planteamientoProblema());
        informe.setObjetivos(req.objetivos());
        informe.setMarcoTeorico(req.marcoTeorico());
        informe.setMetodologia(req.metodologia());
        informe.setResultados(req.resultados());
        informe.setConclusiones(req.conclusiones());
        informe.setBibliografia(req.bibliografia());
        return toInformeDto(informeRepo.save(informe), idEstudiante, periodo.getIdPeriodo());
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — enviar informe
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoInformeDto enviarInforme(Integer idEstudiante) {
        PeriodoTitulacion periodo = getPeriodoActivo();
        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NO_TIENES_REGISTRO_COMPLEXIVO"));

        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(ct.getIdComplexivo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NO_HAY_INFORME_PARA_ENVIAR"));

        if (!"BORRADOR".equals(informe.getEstado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INFORME_YA_ENVIADO");

        informe.setEstado("ENTREGADO");
        informe.setFechaEntrega(java.time.LocalDate.now());
        return toInformeDto(informeRepo.save(informe), idEstudiante, periodo.getIdPeriodo());
    }

    // ═══════════════════════════════════════════════════════════════
    // DT1 — propuestas de sus estudiantes (para revisar en Aprobación)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<PropuestaComplexivoDto> propuestasDeDocenteDt1(Integer idDocente) {
        PeriodoTitulacion periodo = getPeriodoActivo();

        List<Integer> idsEstudiantes = dt1Repo
                .findByPeriodo_IdPeriodoAndActivoTrue(periodo.getIdPeriodo())
                .stream()
                .filter(a -> a.getDocente().getIdDocente().equals(idDocente))
                .map(a -> a.getEstudiante().getIdEstudiante())
                .toList();

        if (idsEstudiantes.isEmpty()) return List.of();

        return propuestaRepo.findAll().stream()
                .filter(p -> p.getEstudiante() != null
                        && idsEstudiantes.contains(p.getEstudiante().getIdEstudiante())
                        && p.getEleccion() != null
                        && periodo.getIdPeriodo().equals(
                        p.getEleccion().getPeriodo().getIdPeriodo()))
                .map(this::toPropuestaComplexivoDto)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // DT1 — aprobar o rechazar propuesta
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public PropuestaComplexivoDto decidirPropuestaDt1(Integer idDocente,
                                                      Integer idPropuesta,
                                                      DecisionPropuestaComplexivoRequest req) {
        PropuestaTitulacion propuesta = propuestaRepo.findById(idPropuesta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "PROPUESTA_NO_ENCONTRADA"));

        PeriodoTitulacion periodo = getPeriodoActivo();

        boolean esDt1 = dt1Repo.findByPeriodo_IdPeriodoAndActivoTrue(periodo.getIdPeriodo())
                .stream()
                .anyMatch(a -> a.getDocente().getIdDocente().equals(idDocente)
                        && a.getEstudiante().getIdEstudiante()
                        .equals(propuesta.getEstudiante().getIdEstudiante()));

        if (!esDt1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "NO_ERES_EL_DOCENTE_DT1_DE_ESTE_ESTUDIANTE");

        String estado = req.estado() == null ? "" : req.estado().trim().toUpperCase();
        if (!estado.equals("APROBADA") && !estado.equals("RECHAZADA"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ESTADO_INVALIDO");

        Integer actualizado = propuestaRepo.registrarDecisionStored(
                idPropuesta, estado, req.observaciones(), java.time.LocalDate.now());

        if (actualizado == null || actualizado == 0)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "NO_SE_PUDO_REGISTRAR_LA_DECISION");

        PropuestaTitulacion actualizada = propuestaRepo.findById(idPropuesta).orElseThrow();

        if ("APROBADA".equals(actualizada.getEstado())) {
            boolean yaExiste = complexivoRepo
                    .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                            actualizada.getEstudiante().getIdEstudiante(),
                            periodo.getIdPeriodo())
                    .isPresent();
            if (!yaExiste) {
                ComplexivoTitulacion ct = new ComplexivoTitulacion();
                ct.setEstudiante(actualizada.getEstudiante());
                ct.setCarrera(actualizada.getCarrera());
                ct.setPeriodo(periodo);
                ct.setEleccion(actualizada.getEleccion());
                ct.setEstado("EN_CURSO");
                ct.setFechaInscripcion(java.time.LocalDate.now());
                complexivoRepo.save(ct);
            }
        }

        return toPropuestaComplexivoDto(actualizada);
    }

    // ═══════════════════════════════════════════════════════════════
    // DT2 — lista de sus estudiantes (informes)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<EstudianteDeDocenteDto> estudiantesDeDocenteDt2(Integer idDocente) {
        PeriodoTitulacion periodo = getPeriodoActivo();

        return dt2Repo.findByPeriodo_IdPeriodoAndActivoTrue(periodo.getIdPeriodo())
                .stream()
                .filter(a -> a.getDocente().getIdDocente().equals(idDocente))
                .map(a -> {
                    Estudiante est = a.getEstudiante();
                    String nombre = est.getUsuario().getNombres()
                            + " " + est.getUsuario().getApellidos();
                    var ctOpt = complexivoRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                            est.getIdEstudiante(), periodo.getIdPeriodo());
                    if (ctOpt.isEmpty()) return null;
                    ComplexivoTitulacion ct = ctOpt.get();
                    var infOpt = informeRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo());
                    return new EstudianteDeDocenteDto(
                            ct.getIdComplexivo(), est.getIdEstudiante(), nombre,
                            est.getCarrera() != null ? est.getCarrera().getNombre() : "",
                            ct.getEstado(), infOpt.isPresent(),
                            infOpt.map(ComplexivoInformePractico::getEstado).orElse(null));
                })
                .filter(d -> d != null)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // DT2 — ver informe de un estudiante
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ComplexivoInformeDto getInformeParaDt2(Integer idDocente, Integer idComplexivo) {
        ComplexivoTitulacion ct = complexivoRepo.findById(idComplexivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "COMPLEXIVO_NO_ENCONTRADO"));

        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(idComplexivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ESTUDIANTE_SIN_INFORME_AUN"));

        return toInformeDto(informe,
                ct.getEstudiante().getIdEstudiante(),
                ct.getPeriodo().getIdPeriodo());
    }

    // ═══════════════════════════════════════════════════════════════
    // DT2 — aprobar o rechazar informe
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoInformeDto revisarInforme(Integer idDocente, Integer idInforme,
                                               String nuevoEstado, String observaciones) {
        ComplexivoInformePractico informe = informeRepo.findById(idInforme)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "INFORME_NO_ENCONTRADO"));

        if (!"ENTREGADO".equals(informe.getEstado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "SOLO_SE_PUEDE_REVISAR_INFORMES_EN_ESTADO_ENTREGADO");

        informe.setEstado(nuevoEstado);
        informe.setObservaciones(observaciones);
        informe.setFechaRevision(java.time.LocalDate.now());

        ComplexivoTitulacion ct = informe.getComplexivo();
        return toInformeDto(informeRepo.save(informe),
                ct.getEstudiante().getIdEstudiante(),
                ct.getPeriodo().getIdPeriodo());
    }

    // ═══════════════════════════════════════════════════════════════
    // DT2 — registrar asesoría
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoAsesoriaDto registrarAsesoria(Integer idDocente,
                                                   Integer idComplexivo,
                                                   RegistrarAsesoriaRequest req) {
        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(idComplexivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ESTUDIANTE_SIN_INFORME"));

        Docente docente = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO"));

        ComplexivoTutoria asesoria = new ComplexivoTutoria();
        asesoria.setInforme(informe);
        asesoria.setDocente(docente);
        asesoria.setObservaciones(req.observaciones());
        asesoria.setFecha(java.time.LocalDateTime.now());

        ComplexivoTutoria saved = complexivoTutoriaRepo.save(asesoria);
        return new ComplexivoAsesoriaDto(
                saved.getIdAsesoria(), idComplexivo,
                saved.getFecha().toString(), saved.getObservaciones(),
                docente.getUsuario().getNombres() + " " + docente.getUsuario().getApellidos());
    }

    // ═══════════════════════════════════════════════════════════════
    // DT2 — listar asesorías
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<ComplexivoAsesoriaDto> listarAsesorias(Integer idDocente,
                                                       Integer idComplexivo) {
        return complexivoTutoriaRepo
                .findByInforme_Complexivo_IdComplexivo(idComplexivo)
                .stream()
                .map(a -> new ComplexivoAsesoriaDto(
                        a.getIdAsesoria(), idComplexivo,
                        a.getFecha().toString(), a.getObservaciones(),
                        a.getDocente().getUsuario().getNombres()
                                + " " + a.getDocente().getUsuario().getApellidos()))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // Métodos legacy — mantener compatibilidad con código existente
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public InfoCoordinadorComplexivoDto infoCoordinador(Integer idUsuario) {
        InfoCoordinadorDt1Dto dt1 = infoCoordinadorDt1(idUsuario);
        return new InfoCoordinadorComplexivoDto(
                dt1.idCarrera(), dt1.carrera(), dt1.idPeriodo(), dt1.periodo(),
                dt1.docentesDisponibles(), dt1.estudiantesSinDocente(), dt1.asignacionesActuales());
    }

    @Transactional
    public ComplexivoDocenteAsignacionResponse asignarDocente(AsignarDocenteComplexivoRequest req) {
        return asignarDt1(new AsignarDt1ComplexivoRequest(
                req.idEstudiante(), req.idDocente(),
                req.idUsuarioCoordinador(), req.observacion()));
    }

    @Transactional(readOnly = true)
    public List<PropuestaComplexivoDto> propuestasDeDocente(Integer idDocente) {
        return propuestasDeDocenteDt1(idDocente);
    }

    @Transactional
    public PropuestaComplexivoDto decidirPropuesta(Integer idDocente, Integer idPropuesta,
                                                   DecisionPropuestaComplexivoRequest req) {
        return decidirPropuestaDt1(idDocente, idPropuesta, req);
    }

    @Transactional(readOnly = true)
    public List<EstudianteDeDocenteDto> estudiantesDeDocente(Integer idDocente) {
        return estudiantesDeDocenteDt2(idDocente);
    }

    @Transactional(readOnly = true)
    public ComplexivoInformeDto getInformeParaDocente(Integer idDocente, Integer idComplexivo) {
        return getInformeParaDt2(idDocente, idComplexivo);
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers privados
    // ═══════════════════════════════════════════════════════════════
    private PeriodoTitulacion getPeriodoActivo() {
        return periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));
    }

    private Carrera getCarreraCoordinador(Integer idUsuario) {
        return coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "NO_ES_COORDINADOR_O_NO_ACTIVO"))
                .getCarrera();
    }

    private List<DocenteOpcionDto> getTodosDocentes() {
        return docenteRepo.findAll().stream()
                .map(d -> new DocenteOpcionDto(d.getIdDocente(),
                        d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()))
                .toList();
    }

    private EstudianteComplexivoSinDocenteDto toEstudianteSinDocente(
            Estudiante est, Carrera carrera, PeriodoTitulacion periodo) {
        String nombre = est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos();
        String estadoCt = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        est.getIdEstudiante(), periodo.getIdPeriodo())
                .map(ComplexivoTitulacion::getEstado).orElse("INSCRITO");
        return new EstudianteComplexivoSinDocenteDto(
                est.getIdEstudiante(), nombre,
                carrera.getNombre(), NOMBRE_MODALIDAD_COMPLEXIVO, estadoCt);
    }

    private ComplexivoDocenteAsignacionResponse toDt1AsignacionResponse(
            ComplexivoDt1Asignacion a) {
        return new ComplexivoDocenteAsignacionResponse(
                a.getIdAsignacion(),
                a.getEstudiante().getIdEstudiante(),
                a.getEstudiante().getUsuario().getNombres() + " "
                        + a.getEstudiante().getUsuario().getApellidos(),
                a.getDocente().getIdDocente(),
                a.getDocente().getUsuario().getNombres() + " "
                        + a.getDocente().getUsuario().getApellidos(),
                a.getPeriodo().getIdPeriodo(), a.getPeriodo().getDescripcion(),
                a.getFechaAsignacion(), a.getActivo());
    }

    private ComplexivoDocenteAsignacionResponse toDt2AsignacionResponse(
            ComplexivoDt2Asignacion a) {
        return new ComplexivoDocenteAsignacionResponse(
                a.getIdAsignacion(),
                a.getEstudiante().getIdEstudiante(),
                a.getEstudiante().getUsuario().getNombres() + " "
                        + a.getEstudiante().getUsuario().getApellidos(),
                a.getDocente().getIdDocente(),
                a.getDocente().getUsuario().getNombres() + " "
                        + a.getDocente().getUsuario().getApellidos(),
                a.getPeriodo().getIdPeriodo(), a.getPeriodo().getDescripcion(),
                a.getFechaAsignacion(), a.getActivo());
    }

    private ComplexivoInformeDto toInformeDto(ComplexivoInformePractico i,
                                              Integer idEstudiante, Integer idPeriodo) {
        var dt2Opt = dt2Repo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                idEstudiante, idPeriodo);
        return new ComplexivoInformeDto(
                i.getIdInforme(), i.getComplexivo().getIdComplexivo(),
                i.getTitulo(), i.getPlanteamientoProblema(), i.getObjetivos(),
                i.getMarcoTeorico(), i.getMetodologia(), i.getResultados(),
                i.getConclusiones(), i.getBibliografia(),
                i.getEstado(), i.getObservaciones(),
                dt2Opt.map(a -> a.getDocente().getIdDocente()).orElse(null),
                dt2Opt.map(a -> a.getDocente().getUsuario().getNombres()
                        + " " + a.getDocente().getUsuario().getApellidos()).orElse(null));
    }

    private PropuestaComplexivoDto toPropuestaComplexivoDto(PropuestaTitulacion p) {
        String nombreEst = p.getEstudiante() != null && p.getEstudiante().getUsuario() != null
                ? p.getEstudiante().getUsuario().getNombres() + " "
                + p.getEstudiante().getUsuario().getApellidos() : "";
        return new PropuestaComplexivoDto(
                p.getIdPropuesta(),
                p.getEstudiante() != null ? p.getEstudiante().getIdEstudiante() : null,
                nombreEst, p.getTitulo(), p.getPlanteamientoProblema(),
                p.getObjetivosGenerales(), p.getObjetivosEspecificos(),
                p.getMetodologia(), p.getResultadosEsperados(), p.getBibliografia(),
                p.getEstado(), p.getObservacionesComision(),
                p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : null);
    }
}