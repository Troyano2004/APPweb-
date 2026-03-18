
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

    private final ComplexivoDocenteAsignacionRepository asignacionRepo;
    private final ComplexivoTitulacionRepository        complexivoRepo;
    private final ComplexivoInformePracticoRepository   informeRepo;
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
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo,
            DocenteCarreraRepository docenteCarreraRepo,
            UsuarioRepository usuarioRepo,
            CoordinadorRepository coordinadorRepo,
            PeriodoTitulacionRepository periodoRepo,
            EleccionTitulacionRepository eleccionRepo) {
        this.asignacionRepo    = asignacionRepo;
        this.complexivoRepo    = complexivoRepo;
        this.informeRepo       = informeRepo;
        this.estudianteRepo    = estudianteRepo;
        this.docenteRepo       = docenteRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
        this.usuarioRepo       = usuarioRepo;
        this.coordinadorRepo   = coordinadorRepo;
        this.periodoRepo       = periodoRepo;
        this.eleccionRepo      = eleccionRepo;
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

        // Obtener carrera del coordinador usando el repo correcto
        Coordinador coord = coordinadorRepo
                .findByUsuario_IdUsuarioAndActivoTrue(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "NO_ES_COORDINADOR_O_NO_ACTIVO"));

        Carrera carrera = coord.getCarrera();

        // Docentes disponibles — todos los docentes activos del sistema
        List<DocenteOpcionDto> docentes = docenteRepo.findAll()
                .stream()
                .map(d -> new DocenteOpcionDto(
                        d.getIdDocente(),
                        d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()))
                .toList();

        // Estudiantes en modalidad complexivo SIN docente asignado
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

                    // Buscar si ya tiene registro complexivo para mostrar estado
                    String estadoCt = complexivoRepo
                            .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                                    est.getIdEstudiante(), periodo.getIdPeriodo())
                            .map(ComplexivoTitulacion::getEstado)
                            .orElse("INSCRITO");

                    return new EstudianteComplexivoSinDocenteDto(
                            est.getIdEstudiante(),
                            nombre,
                            carrera.getNombre(),
                            NOMBRE_MODALIDAD_COMPLEXIVO,
                            estadoCt);
                })
                .toList();

        // Asignaciones actuales del periodo y carrera
        List<ComplexivoDocenteAsignacionResponse> actuales = asignacionRepo
                .findByCarreraAndPeriodoActivo(carrera.getIdCarrera(), periodo.getIdPeriodo())
                .stream()
                .map(this::toAsignacionResponse)
                .toList();

        return new InfoCoordinadorComplexivoDto(
                carrera.getIdCarrera(),
                carrera.getNombre(),
                periodo.getIdPeriodo(),
                periodo.getDescripcion(),   // ← PeriodoTitulacion usa "descripcion", no "nombre"
                docentes,
                sinDocente,
                actuales);
    }

    // ═══════════════════════════════════════════════════════════════
    // COORDINADOR — asignar docente a estudiante complexivo
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
    // ESTUDIANTE — estado de su complexivo
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public EstadoComplexivoEstudianteDto estadoEstudiante(Integer idEstudiante) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElse(null);

        if (periodo == null)
            return new EstadoComplexivoEstudianteDto(
                    false, null, null, false, null, null, false, null);

        var ctOpt = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo());

        if (ctOpt.isEmpty())
            return new EstadoComplexivoEstudianteDto(
                    false, null, null, false, null, null, false, null);

        ComplexivoTitulacion ct = ctOpt.get();

        var informeOpt = informeRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo());

        var docOpt = asignacionRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        idEstudiante, periodo.getIdPeriodo());

        String nombreDocente = docOpt.map(a ->
                        a.getDocente().getUsuario().getNombres()
                                + " " + a.getDocente().getUsuario().getApellidos())
                .orElse(null);

        return new EstadoComplexivoEstudianteDto(
                true,
                ct.getIdComplexivo(),
                ct.getEstado(),
                informeOpt.isPresent(),
                informeOpt.map(ComplexivoInformePractico::getIdInforme).orElse(null),
                informeOpt.map(ComplexivoInformePractico::getEstado).orElse(null),
                docOpt.isPresent(),
                nombreDocente);
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTUDIANTE — obtener informe práctico (crea borrador si no existe)
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
    // ESTUDIANTE — enviar informe a revisión
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
    // Helpers privados
    // ═══════════════════════════════════════════════════════════════

    private ComplexivoDocenteAsignacionResponse toAsignacionResponse(
            ComplexivoDocenteAsignacion a) {

        String nombreEst = a.getEstudiante().getUsuario().getNombres()
                + " " + a.getEstudiante().getUsuario().getApellidos();
        String nombreDoc = a.getDocente().getUsuario().getNombres()
                + " " + a.getDocente().getUsuario().getApellidos();

        return new ComplexivoDocenteAsignacionResponse(
                a.getIdAsignacion(),
                a.getEstudiante().getIdEstudiante(),
                nombreEst,
                a.getDocente().getIdDocente(),
                nombreDoc,
                a.getPeriodo().getIdPeriodo(),
                a.getPeriodo().getDescripcion(),   // ← "descripcion", no "nombre"
                a.getFechaAsignacion(),
                a.getActivo());
    }

    private ComplexivoInformeDto toInformeDto(ComplexivoInformePractico i,
                                              Integer idEstudiante,
                                              Integer idPeriodo) {

        var docOpt = asignacionRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                        idEstudiante, idPeriodo);

        Integer idDoc = docOpt.map(a -> a.getDocente().getIdDocente()).orElse(null);
        String  nomDoc = docOpt.map(a ->
                        a.getDocente().getUsuario().getNombres()
                                + " " + a.getDocente().getUsuario().getApellidos())
                .orElse(null);

        return new ComplexivoInformeDto(
                i.getIdInforme(),
                i.getComplexivo().getIdComplexivo(),
                i.getTitulo(),
                i.getPlanteamientoProblema(),
                i.getObjetivos(),
                i.getMarcoTeorico(),
                i.getMetodologia(),
                i.getResultados(),
                i.getConclusiones(),
                i.getBibliografia(),
                i.getEstado(),
                i.getObservaciones(),
                idDoc,
                nomDoc);
    }



}