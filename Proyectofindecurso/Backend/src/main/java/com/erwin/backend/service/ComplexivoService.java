
package com.erwin.backend.service;

import com.erwin.backend.dtos.ComplexivoDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import com.erwin.backend.repository.PropuestaTitulacionRepository; // ✅ NUEVO

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComplexivoService {

    private static final String NOMBRE_MODALIDAD_COMPLEXIVO = "Examen Complexivo";

    private final ComplexivoDocenteAsignacionRepository asignacionRepo;
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


    public ComplexivoService(
            ComplexivoDocenteAsignacionRepository asignacionRepo,
            ComplexivoTitulacionRepository complexivoRepo,
            ComplexivoInformePracticoRepository informeRepo,
            ComplexivoTutoriaRepository complexivoTutoriaRepo,
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo,
            DocenteCarreraRepository docenteCarreraRepo,
            UsuarioRepository usuarioRepo,
            CoordinadorRepository coordinadorRepo,
            PeriodoTitulacionRepository periodoRepo,
            EleccionTitulacionRepository eleccionRepo) {
        this.asignacionRepo        = asignacionRepo;
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
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — información académica para asignación
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public InfoCoordinadorComplexivoDto infoCoordinador(Integer idUsuario) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        Coordinador coord = coordinadorRepo
                .findByUsuario_IdUsuarioAndActivoTrue(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "NO_ES_COORDINADOR_O_NO_ACTIVO"));

        Carrera carrera = coord.getCarrera();

        List<DocenteOpcionDto> docentes = docenteRepo.findAll()
                .stream()
                .map(d -> new DocenteOpcionDto(
                        d.getIdDocente(),
                        d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()))
                .toList();

        List<EstudianteComplexivoSinDocenteDto> sinDocente = eleccionRepo
                .findByCarrera_IdCarreraAndModalidad_NombreAndPeriodo_IdPeriodoAndEstado(
                        carrera.getIdCarrera(),
                        NOMBRE_MODALIDAD_COMPLEXIVO,
                        periodo.getIdPeriodo(),
                        "ACTIVA")
                .stream()
                .filter(e -> !asignacionRepo
                        .existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                                e.getEstudiante().getIdEstudiante(),
                                periodo.getIdPeriodo()))
                .map(e -> {
                    Estudiante est = e.getEstudiante();
                    String nombre = est.getUsuario().getNombres()
                            + " " + est.getUsuario().getApellidos();
                    String estadoCt = complexivoRepo
                            .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                                    est.getIdEstudiante(), periodo.getIdPeriodo())
                            .map(ComplexivoTitulacion::getEstado)
                            .orElse("INSCRITO");
                    return new EstudianteComplexivoSinDocenteDto(
                            est.getIdEstudiante(), nombre,
                            carrera.getNombre(),
                            NOMBRE_MODALIDAD_COMPLEXIVO, estadoCt);
                })
                .toList();

        List<ComplexivoDocenteAsignacionResponse> actuales = asignacionRepo
                .findByCarreraAndPeriodoActivo(carrera.getIdCarrera(), periodo.getIdPeriodo())
                .stream()
                .map(this::toAsignacionResponse)
                .toList();

        return new InfoCoordinadorComplexivoDto(
                carrera.getIdCarrera(), carrera.getNombre(),
                periodo.getIdPeriodo(), periodo.getDescripcion(),
                docentes, sinDocente, actuales);
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — asignar docente
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoDocenteAsignacionResponse asignarDocente(AsignarDocenteComplexivoRequest req) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        if (asignacionRepo.existsByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                req.idEstudiante(), periodo.getIdPeriodo()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ESTUDIANTE_YA_TIENE_DOCENTE_ASIGNADO");

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

        ComplexivoDocenteAsignacion asig = new ComplexivoDocenteAsignacion();
        asig.setEstudiante(estudiante);
        asig.setDocente(docente);
        asig.setPeriodo(periodo);
        asig.setCarrera(eleccion.getCarrera());
        asig.setAsignadoPor(asignadoPor);
        asig.setObservacion(req.observacion());
        asig.setActivo(true);

        return toAsignacionResponse(asignacionRepo.save(asig));
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
        var docOpt = asignacionRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                idEstudiante, periodo.getIdPeriodo());

        return new EstadoComplexivoEstudianteDto(
                true, ct.getIdComplexivo(), ct.getEstado(),
                informeOpt.isPresent(),
                informeOpt.map(ComplexivoInformePractico::getIdInforme).orElse(null),
                informeOpt.map(ComplexivoInformePractico::getEstado).orElse(null),
                docOpt.isPresent(),
                docOpt.map(a -> a.getDocente().getUsuario().getNombres()
                        + " " + a.getDocente().getUsuario().getApellidos()).orElse(null));
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — obtener informe
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public ComplexivoInformeDto getInforme(Integer idEstudiante) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NO_TIENES_REGISTRO_COMPLEXIVO_EN_PERIODO_ACTIVO"));

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

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NO_TIENES_REGISTRO_COMPLEXIVO"));

        ComplexivoInformePractico informe = informeRepo
                .findByComplexivo_IdComplexivo(ct.getIdComplexivo())
                .orElseGet(() -> {
                    ComplexivoInformePractico nuevo = new ComplexivoInformePractico();
                    nuevo.setComplexivo(ct);
                    nuevo.setEstado("BORRADOR");
                    return nuevo;
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

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

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
    // DOCENTE — lista de sus estudiantes
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<EstudianteDeDocenteDto> estudiantesDeDocente(Integer idDocente) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        return asignacionRepo
                .findByPeriodo_IdPeriodoAndActivoTrue(periodo.getIdPeriodo())
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
                            ct.getIdComplexivo(),
                            est.getIdEstudiante(),
                            nombre,
                            est.getCarrera() != null ? est.getCarrera().getNombre() : "",
                            ct.getEstado(),
                            infOpt.isPresent(),
                            infOpt.map(ComplexivoInformePractico::getEstado).orElse(null));
                })
                .filter(d -> d != null)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // DOCENTE — ver informe de un estudiante
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ComplexivoInformeDto getInformeParaDocente(Integer idDocente, Integer idComplexivo) {

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
    // DOCENTE — aprobar o rechazar informe
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
    // DOCENTE — registrar asesoría
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
                saved.getIdAsesoria(),
                idComplexivo,
                saved.getFecha().toString(),
                saved.getObservaciones(),
                docente.getUsuario().getNombres() + " " + docente.getUsuario().getApellidos());
    }

    // ═══════════════════════════════════════════════════════════════
    // DOCENTE — listar asesorías
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<ComplexivoAsesoriaDto> listarAsesorias(Integer idDocente, Integer idComplexivo) {

        return complexivoTutoriaRepo
                .findByInforme_Complexivo_IdComplexivo(idComplexivo)
                .stream()
                .map(a -> new ComplexivoAsesoriaDto(
                        a.getIdAsesoria(),
                        idComplexivo,
                        a.getFecha().toString(),
                        a.getObservaciones(),
                        a.getDocente().getUsuario().getNombres()
                                + " " + a.getDocente().getUsuario().getApellidos()))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers privados
    // ═══════════════════════════════════════════════════════════════
    private ComplexivoDocenteAsignacionResponse toAsignacionResponse(
            ComplexivoDocenteAsignacion a) {
        return new ComplexivoDocenteAsignacionResponse(
                a.getIdAsignacion(),
                a.getEstudiante().getIdEstudiante(),
                a.getEstudiante().getUsuario().getNombres() + " "
                        + a.getEstudiante().getUsuario().getApellidos(),
                a.getDocente().getIdDocente(),
                a.getDocente().getUsuario().getNombres() + " "
                        + a.getDocente().getUsuario().getApellidos(),
                a.getPeriodo().getIdPeriodo(),
                a.getPeriodo().getDescripcion(),
                a.getFechaAsignacion(),
                a.getActivo());
    }

    private ComplexivoInformeDto toInformeDto(ComplexivoInformePractico i,
                                              Integer idEstudiante, Integer idPeriodo) {
        var docOpt = asignacionRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        idEstudiante, idPeriodo);
        return new ComplexivoInformeDto(
                i.getIdInforme(),
                i.getComplexivo().getIdComplexivo(),
                i.getTitulo(), i.getPlanteamientoProblema(), i.getObjetivos(),
                i.getMarcoTeorico(), i.getMetodologia(), i.getResultados(),
                i.getConclusiones(), i.getBibliografia(), i.getEstado(), i.getObservaciones(),
                docOpt.map(a -> a.getDocente().getIdDocente()).orElse(null),
                docOpt.map(a -> a.getDocente().getUsuario().getNombres()
                        + " " + a.getDocente().getUsuario().getApellidos()).orElse(null));
    }
}