package com.erwin.backend.controller;

import com.erwin.backend.entities.*;
import com.erwin.backend.repository.AnteproyectoTitulacionRepository;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.*;
import com.erwin.backend.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/comision-temas")
@CrossOrigin(origins = "*")
public class ComisionTemasController {

    private final BancoTemasRepository bancoTemasRepository;
    private final PropuestaTitulacionRepository propuestaRepository;
    private final ComisionMiembroRepository comisionMiembroRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository carreraRepository;
    private final EleccionTitulacionRepository eleccionRepository;
    private final ModalidadTitulacionRepository modalidadRepository;
    private final CarreraModalidadRepository carreraModalidadRepository;
    private final PeriodoTitulacionRepository periodoRepository;
    private final ProyectoTitulacionRepository proyectoTitulacionRepository;
    private final TipoTrabajoTitulacionRepository tipoTrabajoTitulacionRepository;
    private final DocumentoTitulacionRepository documentoTitulacionRepository;
    private final AnteproyectoTitulacionRepository anteproyectoTitulacionRepository;
    private final EmailService emailService;

    public ComisionTemasController(BancoTemasRepository bancoTemasRepository,
                                   PropuestaTitulacionRepository propuestaRepository,
                                   ComisionMiembroRepository comisionMiembroRepository,
                                   DocenteRepository docenteRepository,
                                   EstudianteRepository estudianteRepository,
                                   CarreraRepository carreraRepository,
                                   EleccionTitulacionRepository eleccionRepository,
                                   ModalidadTitulacionRepository modalidadRepository,
                                   CarreraModalidadRepository carreraModalidadRepository,
                                   PeriodoTitulacionRepository periodoRepository,
                                   ProyectoTitulacionRepository proyectoTitulacionRepository,
                                   TipoTrabajoTitulacionRepository tipoTrabajoTitulacionRepository,
                                   DocumentoTitulacionRepository documentoTitulacionRepository,
                                   AnteproyectoTitulacionRepository anteproyectoTitulacionRepository,
                                   EmailService emailService) {
        this.bancoTemasRepository = bancoTemasRepository;
        this.propuestaRepository = propuestaRepository;
        this.comisionMiembroRepository = comisionMiembroRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.carreraRepository = carreraRepository;
        this.eleccionRepository = eleccionRepository;
        this.modalidadRepository = modalidadRepository;
        this.carreraModalidadRepository = carreraModalidadRepository;
        this.periodoRepository = periodoRepository;
        this.proyectoTitulacionRepository = proyectoTitulacionRepository;
        this.tipoTrabajoTitulacionRepository = tipoTrabajoTitulacionRepository;
        this.documentoTitulacionRepository = documentoTitulacionRepository;
        this.anteproyectoTitulacionRepository = anteproyectoTitulacionRepository;
        this.emailService = emailService;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODALIDAD
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/estudiante/{idEstudiante}/estado-modalidad")
    public EstadoModalidadDto estadoModalidad(@PathVariable Integer idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        EleccionTitulacion eleccion = obtenerEleccionVigente(idEstudiante);
        Integer idCarrera = estudiante.getCarrera() != null ? estudiante.getCarrera().getIdCarrera() : null;
        List<ModalidadSimpleDto> modalidadesDisponibles = obtenerModalidadesDisponibles(idCarrera);

        return new EstadoModalidadDto(
                eleccion != null,
                eleccion != null ? eleccion.getIdEleccion() : null,
                eleccion != null && eleccion.getModalidad() != null ? eleccion.getModalidad().getIdModalidad() : null,
                eleccion != null && eleccion.getModalidad() != null ? eleccion.getModalidad().getNombre() : null,
                idCarrera,
                modalidadesDisponibles
        );
    }

    @PostMapping("/estudiante/{idEstudiante}/seleccionar-modalidad")
    public EstadoModalidadDto seleccionarModalidad(@PathVariable Integer idEstudiante,
                                                   @RequestBody SeleccionarModalidadRequest req) {
        if (req == null || req.idModalidad == null) {
            throw new RuntimeException("Debes seleccionar una modalidad de titulación");
        }

        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (estudiante.getCarrera() == null || estudiante.getCarrera().getIdCarrera() == null) {
            throw new RuntimeException("El estudiante no tiene una carrera registrada");
        }

        Integer idCarrera  = estudiante.getCarrera().getIdCarrera();
        Integer idModalidad = req.idModalidad;

        boolean esPermitida = carreraModalidadRepository
                .existsById_IdCarreraAndId_IdModalidadAndActivoTrue(idCarrera, idModalidad);
        if (!esPermitida) {
            throw new RuntimeException("La modalidad seleccionada no está habilitada para tu carrera");
        }

        Modalidadtitulacion modalidad = modalidadRepository.findById(idModalidad)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        PeriodoTitulacion periodoActivo = periodoRepository.findByActivoTrue()
                .orElseThrow(() -> new RuntimeException("No hay un período de titulación activo"));

        EleccionTitulacion eleccion = eleccionRepository
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(idEstudiante, periodoActivo.getIdPeriodo())
                .orElseGet(EleccionTitulacion::new);

        eleccion.setEstudiante(estudiante);
        eleccion.setCarrera(estudiante.getCarrera());
        eleccion.setModalidad(modalidad);
        eleccion.setPeriodo(periodoActivo);
        eleccion.setFechaEleccion(LocalDate.now());
        eleccion.setEstado("ACTIVA");

        EleccionTitulacion guardada = eleccionRepository.save(eleccion);
        return estadoModalidad(idEstudiante).withEleccion(guardada.getIdEleccion(), modalidad.getIdModalidad(), modalidad.getNombre());
    }

    // ════════════════════════════════════════════════════════════════════════
    // BANCO DE TEMAS — COMISIÓN
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/docente/{idDocente}/banco")
    public List<TemaDto> bancoTemas(@PathVariable Integer idDocente) {
        validarMiembroComision(idDocente);
        return bancoTemasRepository.findAll().stream()
                .sorted(Comparator.comparing(BancoTemas::getIdTema).reversed())
                .map(this::toTemaDto)
                .toList();
    }

    @PostMapping("/docente/{idDocente}/banco")
    public TemaDto crearTema(@PathVariable Integer idDocente,
                             @RequestBody CrearTemaRequest req) {
        validarMiembroComision(idDocente);

        if (req == null || req.titulo == null || req.titulo.trim().isEmpty()
                || req.descripcion == null || req.descripcion.trim().isEmpty()) {
            throw new RuntimeException("Título y descripción son obligatorios");
        }

        Docente docente = docenteRepository.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
        Carrera carrera = carreraRepository.findById(req.idCarrera)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        BancoTemas tema = new BancoTemas();
        tema.setTitulo(req.titulo.trim());
        tema.setDescripcion(req.descripcion.trim());
        tema.setEstado("PROPUESTO");
        tema.setCarrera(carrera);
        tema.setDocenteProponente(docente);
        tema.setObservaciones(req.observaciones);

        return toTemaDto(bancoTemasRepository.save(tema));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROPUESTAS DE TITULACIÓN — COMISIÓN
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/docente/{idDocente}/propuestas")
    public List<PropuestaDto> propuestas(@PathVariable Integer idDocente) {
        validarMiembroComision(idDocente);
        return propuestaRepository.findAll().stream()
                .sorted(Comparator.comparing(PropuestaTitulacion::getIdPropuesta).reversed())
                .map(this::toPropuestaDto)
                .toList();
    }

    @PostMapping("/docente/{idDocente}/propuestas/{idPropuesta}/decision")
    public PropuestaDto decidirPropuesta(@PathVariable Integer idDocente,
                                         @PathVariable Integer idPropuesta,
                                         @RequestBody DecisionPropuestaRequest req) {
        validarMiembroComision(idDocente);

        if (!propuestaRepository.existsById(idPropuesta)) {
            throw new RuntimeException("Propuesta no encontrada");
        }

        String estado = req.estado == null ? "" : req.estado.trim().toUpperCase();
        if (!estado.equals("APROBADA") && !estado.equals("RECHAZADA")) {
            throw new RuntimeException("El estado debe ser APROBADA o RECHAZADA");
        }

        Integer actualizado = propuestaRepository.registrarDecisionStored(
                idPropuesta, estado, req.observaciones, LocalDate.now()
        );

        if (actualizado == null || actualizado == 0) {
            throw new RuntimeException("No se pudo registrar la decisión de la propuesta");
        }

        PropuestaTitulacion propuestaActualizada = propuestaRepository.findById(idPropuesta)
                .orElseThrow(() -> new RuntimeException("Propuesta no encontrada"));

        if ("APROBADA".equals(propuestaActualizada.getEstado())) {
            ProyectoTitulacion proyecto = crearProyectoTitulacionDesdePropuesta(propuestaActualizada);
            if (proyecto != null) {
                crearAnteproyectoAprobadoDesdePropuesta(propuestaActualizada, proyecto);
                crearDocumentoTitulacionDesdeProyecto(proyecto, propuestaActualizada);
            }
            try {
                Estudiante est = propuestaActualizada.getEstudiante();
                String emailEst = est != null && est.getUsuario() != null
                        ? est.getUsuario().getCorreoInstitucional() : null;
                if (emailEst != null && !emailEst.isBlank()) {
                    String nombreEst = (est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos()).trim();
                    String periodoDesc = propuestaActualizada.getEleccion() != null
                            && propuestaActualizada.getEleccion().getPeriodo() != null
                            ? propuestaActualizada.getEleccion().getPeriodo().getDescripcion() : "";
                    emailService.notificarPropuestaAprobada(
                            emailEst, nombreEst, propuestaActualizada.getTitulo(), periodoDesc
                    );
                }
            } catch (Exception e) {
                System.err.println("Error al notificar propuesta aprobada: " + e.getMessage());
            }
        }

        return toPropuestaDto(propuestaActualizada);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROPUESTAS DE TITULACIÓN — ESTUDIANTE
    // ════════════════════════════════════════════════════════════════════════

    @PostMapping("/estudiante/{idEstudiante}/propuestas")
    public PropuestaDto crearPropuesta(@PathVariable Integer idEstudiante,
                                       @RequestBody CrearPropuestaRequest req) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (req == null || req.titulo == null || req.titulo.trim().isEmpty()) {
            throw new RuntimeException("El título de la propuesta es obligatorio");
        }

        EleccionTitulacion eleccion = obtenerEleccionVigente(idEstudiante);
        if (eleccion == null) {
            throw new RuntimeException("El estudiante no tiene elección de titulación registrada");
        }

        Carrera carrera = estudiante.getCarrera() != null
                ? estudiante.getCarrera()
                : carreraRepository.findById(req.idCarrera)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        String temaInvestigacion = valueOrDefault(req.temaInvestigacion, "Pendiente de definir");
        Integer idTema = null;

        if (req.idTema != null) {
            BancoTemas tema = bancoTemasRepository.findById(req.idTema)
                    .orElseThrow(() -> new RuntimeException("Tema seleccionado no existe"));
            idTema = tema.getIdTema();
            if (temaInvestigacion.isBlank() || temaInvestigacion.equals("Pendiente de definir")) {
                temaInvestigacion = tema.getTitulo();
            }
        }

        Integer idPropuestaCreada = propuestaRepository.crearPropuestaStored(
                eleccion.getIdEleccion(),
                estudiante.getIdEstudiante(),
                carrera.getIdCarrera(),
                idTema,
                req.titulo.trim(),
                temaInvestigacion,
                valueOrDefault(req.planteamientoProblema, "Pendiente de definir"),
                valueOrDefault(req.objetivosGenerales, "Pendiente de definir"),
                valueOrDefault(req.objetivosEspecificos, "Pendiente de definir"),
                valueOrDefault(req.marcoTeorico, "Pendiente de definir"),
                valueOrDefault(req.metodologia, "Pendiente de definir"),
                valueOrDefault(req.resultadosEsperados, "Pendiente de definir"),
                valueOrDefault(req.bibliografia, "Pendiente de definir"),
                "EN_REVISION",
                LocalDate.now()
        );

        if (idPropuestaCreada == null) {
            throw new RuntimeException("No se pudo crear la propuesta");
        }

        return toPropuestaDto(propuestaRepository.findById(idPropuestaCreada)
                .orElseThrow(() -> new RuntimeException("Propuesta creada no encontrada")));
    }

    @GetMapping("/estudiante/{idEstudiante}/propuestas")
    public List<PropuestaDto> propuestasEstudiante(@PathVariable Integer idEstudiante) {
        return propuestaRepository.findByEstudianteStored(idEstudiante)
                .stream()
                .sorted(Comparator.comparing(PropuestaTitulacion::getIdPropuesta).reversed())
                .map(this::toPropuestaDto)
                .toList();
    }

    @GetMapping("/estudiante/{idEstudiante}/temas-disponibles")
    public List<TemaDto> temasDisponiblesEstudiante(@PathVariable Integer idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        Integer idCarreraEstudiante = estudiante.getCarrera() != null
                ? estudiante.getCarrera().getIdCarrera() : null;

        return bancoTemasRepository.findAll().stream()
                .filter(t -> idCarreraEstudiante == null
                        || (t.getCarrera() != null && idCarreraEstudiante.equals(t.getCarrera().getIdCarrera())))
                .sorted(Comparator.comparing(BancoTemas::getIdTema).reversed())
                .map(this::toTemaDto)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SUGERENCIAS DE TEMAS — ESTUDIANTE → COMISIÓN
    // ════════════════════════════════════════════════════════════════════════

    @PostMapping("/estudiante/{idEstudiante}/sugerir-tema")
    public TemaDto sugerirTema(@PathVariable Integer idEstudiante,
                               @RequestBody SugerirTemaRequest req) {

        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (req == null || req.titulo == null || req.titulo.trim().isEmpty()) {
            throw new RuntimeException("El título es obligatorio");
        }
        if (req.descripcion == null || req.descripcion.trim().isEmpty()) {
            throw new RuntimeException("La descripción es obligatoria");
        }

        Carrera carrera = estudiante.getCarrera();
        if (carrera == null) {
            throw new RuntimeException("El estudiante no tiene carrera asignada");
        }

        BancoTemas tema = new BancoTemas();
        tema.setTitulo(req.titulo.trim());
        tema.setDescripcion(req.descripcion.trim());
        tema.setEstado("SUGERIDO");
        tema.setCarrera(carrera);
        tema.setDocenteProponente(null);
        tema.setEstudianteSugerente(estudiante); // ✅ FIX: guardar quién sugirió el tema

        return toTemaDto(bancoTemasRepository.save(tema));
    }

    @GetMapping("/docente/{idDocente}/sugerencias")
    public List<TemaDto> listarSugerencias(@PathVariable Integer idDocente) {
        validarMiembroComision(idDocente);
        return bancoTemasRepository.findByEstadoOrderByIdTemaDesc("SUGERIDO")
                .stream()
                .map(this::toTemaDto)
                .toList();
    }

    @PostMapping("/docente/{idDocente}/sugerencias/{idTema}/aprobar")
    public TemaDto aprobarSugerencia(@PathVariable Integer idDocente,
                                     @PathVariable Integer idTema,
                                     @RequestBody(required = false) AprobarSugerenciaRequest req) {
        validarMiembroComision(idDocente);

        BancoTemas tema = bancoTemasRepository.findById(idTema)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        if (!"SUGERIDO".equals(tema.getEstado())) {
            throw new RuntimeException("Este tema ya fue procesado");
        }

        Docente docente = docenteRepository.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        tema.setEstado("PROPUESTO");
        tema.setDocenteProponente(docente);
        tema.setFechaRevision(LocalDate.now());
        if (req != null && req.observaciones != null && !req.observaciones.trim().isEmpty()) {
            tema.setObservaciones(req.observaciones.trim());
        }

        return toTemaDto(bancoTemasRepository.save(tema));
    }

    @PostMapping("/docente/{idDocente}/sugerencias/{idTema}/rechazar")
    public TemaDto rechazarSugerencia(@PathVariable Integer idDocente,
                                      @PathVariable Integer idTema,
                                      @RequestBody(required = false) AprobarSugerenciaRequest req) {
        validarMiembroComision(idDocente);

        BancoTemas tema = bancoTemasRepository.findById(idTema)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        if (!"SUGERIDO".equals(tema.getEstado())) {
            throw new RuntimeException("Este tema ya fue procesado");
        }

        tema.setEstado("RECHAZADO");
        tema.setFechaRevision(LocalDate.now());
        if (req != null && req.observaciones != null && !req.observaciones.trim().isEmpty()) {
            tema.setObservaciones(req.observaciones.trim());
        }

        return toTemaDto(bancoTemasRepository.save(tema));
    }

    @GetMapping("/estudiante/{idEstudiante}/temas-aprobados")
    public List<TemaDto> temasAprobadosEstudiante(@PathVariable Integer idEstudiante) {
        estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        return bancoTemasRepository.findAll().stream()
                .filter(t -> "PROPUESTO".equals(t.getEstado()))
                .filter(t -> t.getEstudianteSugerente() != null
                        && idEstudiante.equals(t.getEstudianteSugerente().getIdEstudiante()))
                .sorted(Comparator.comparing(BancoTemas::getIdTema).reversed())
                .map(this::toTemaDto)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ════════════════════════════════════════════════════════════════════════

    private EleccionTitulacion obtenerEleccionVigente(Integer idEstudiante) {
        return periodoRepository.findByActivoTrue()
                .flatMap(periodo -> eleccionRepository
                        .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(idEstudiante, periodo.getIdPeriodo()))
                .orElse(null);
    }

    private List<ModalidadSimpleDto> obtenerModalidadesDisponibles(Integer idCarrera) {
        if (idCarrera == null) return List.of();
        return carreraModalidadRepository.findById_IdCarreraAndActivoTrue(idCarrera)
                .stream()
                .map(cm -> new ModalidadSimpleDto(
                        cm.getModalidad().getIdModalidad(),
                        cm.getModalidad().getNombre()
                ))
                .toList();
    }

    private void validarMiembroComision(Integer idDocente) {
        boolean esMiembro = comisionMiembroRepository.findAll().stream()
                .anyMatch(m -> m.getDocente() != null && idDocente.equals(m.getDocente().getIdDocente()));
        if (!esMiembro) {
            throw new RuntimeException("El docente no pertenece a ninguna comisión formativa");
        }
    }

    private TemaDto toTemaDto(BancoTemas tema) {
        String docente = "Sin docente";
        if (tema.getDocenteProponente() != null && tema.getDocenteProponente().getUsuario() != null) {
            docente = tema.getDocenteProponente().getUsuario().getNombres() + " "
                    + tema.getDocenteProponente().getUsuario().getApellidos();
        }
        String carrera = tema.getCarrera() != null ? tema.getCarrera().getNombre() : "Sin carrera";

        // ✅ FIX: incluir idEstudianteSugerente en el DTO
        Integer idEstudianteSugerente = tema.getEstudianteSugerente() != null
                ? tema.getEstudianteSugerente().getIdEstudiante()
                : null;

        return new TemaDto(
                tema.getIdTema(),
                tema.getTitulo(),
                tema.getDescripcion(),
                carrera,
                docente,
                tema.getEstado(),
                tema.getObservaciones(),
                idEstudianteSugerente  // ✅ FIX: nuevo campo
        );
    }

    private PropuestaDto toPropuestaDto(PropuestaTitulacion propuesta) {
        String estudiante = "Sin estudiante";
        if (propuesta.getEstudiante() != null && propuesta.getEstudiante().getUsuario() != null) {
            estudiante = propuesta.getEstudiante().getUsuario().getNombres() + " "
                    + propuesta.getEstudiante().getUsuario().getApellidos();
        }
        String carrera = propuesta.getCarrera() != null ? propuesta.getCarrera().getNombre() : "Sin carrera";
        String tema = propuesta.getTema() != null
                ? propuesta.getTema().getTitulo()
                : propuesta.getTemaInvestigacion();

        return new PropuestaDto(
                propuesta.getIdPropuesta(),
                propuesta.getTitulo(),
                tema,
                estudiante,
                carrera,
                propuesta.getEstado(),
                propuesta.getFechaEnvio(),
                propuesta.getObservacionesComision()
        );
    }

    private Docente resolverDirectorInicial(PropuestaTitulacion propuesta) {
        if (propuesta.getTema() != null && propuesta.getTema().getDocenteProponente() != null) {
            return propuesta.getTema().getDocenteProponente();
        }
        throw new RuntimeException("No se puede crear el proyecto: la propuesta no tiene docente director asociado");
    }

    private ProyectoTitulacion crearProyectoTitulacionDesdePropuesta(PropuestaTitulacion propuesta) {
        var proyectoExistente = proyectoTitulacionRepository.findByPropuesta_IdPropuesta(propuesta.getIdPropuesta());
        if (proyectoExistente.isPresent()) {
            return proyectoExistente.get();
        }

        EleccionTitulacion eleccion = propuesta.getEleccion();
        if (eleccion == null || eleccion.getModalidad() == null || eleccion.getModalidad().getIdModalidad() == null) {
            throw new RuntimeException("No se puede crear el proyecto: la propuesta no tiene modalidad de titulación asociada");
        }

        PeriodoTitulacion periodo = eleccion.getPeriodo();
        if (periodo == null) {
            periodo = periodoRepository.findByActivoTrue()
                    .orElseThrow(() -> new RuntimeException("No se encontró un periodo activo"));
        }

        List<Tipotrabajotitulacion> tiposTrabajo = tipoTrabajoTitulacionRepository
                .findByModalidadTitulacion_IdModalidad(eleccion.getModalidad().getIdModalidad());

        if (tiposTrabajo.isEmpty()) {
            throw new RuntimeException("No existe un tipo de trabajo configurado para la modalidad seleccionada");
        }

        Docente directorInicial = resolverDirectorInicial(propuesta);

        ProyectoTitulacion proyecto = new ProyectoTitulacion();
        proyecto.setPropuesta(propuesta);
        proyecto.setEleccion(eleccion);
        proyecto.setPeriodo(periodo);
        proyecto.setDirector(directorInicial);
        proyecto.setTipoTrabajo(tiposTrabajo.get(0));
        proyecto.setTitulo(propuesta.getTitulo());
        proyecto.setEstado("ANTEPROYECTO");

        return proyectoTitulacionRepository.save(proyecto);
    }

    private void crearDocumentoTitulacionDesdeProyecto(ProyectoTitulacion proyecto,
                                                       PropuestaTitulacion propuesta) {
        if (documentoTitulacionRepository.findByProyecto_IdProyecto(proyecto.getIdProyecto()).isPresent()) {
            return;
        }

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setProyecto(proyecto);
        documento.setEstudiante(propuesta.getEstudiante());
        documento.setTitulo(propuesta.getTitulo());
        documento.setEstado(EstadoDocumento.BORRADOR);
        documento.setAnio(LocalDate.now().getYear());

        documentoTitulacionRepository.save(documento);
    }

    private void crearAnteproyectoAprobadoDesdePropuesta(PropuestaTitulacion propuesta,
                                                         ProyectoTitulacion proyecto) {
        if (anteproyectoTitulacionRepository.findByPropuesta_IdPropuesta(propuesta.getIdPropuesta()).isPresent()) {
            return;
        }

        AnteproyectoTitulacion ante = new AnteproyectoTitulacion();
        ante.setPropuesta(propuesta);
        ante.setEleccion(propuesta.getEleccion());
        ante.setEstudiante(propuesta.getEstudiante());
        ante.setCarrera(propuesta.getCarrera());
        ante.setEstado("APROBADO");

        anteproyectoTitulacionRepository.save(ante);
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return value.trim();
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs Y REQUESTS
    // ════════════════════════════════════════════════════════════════════════

    public static class CrearTemaRequest {
        public Integer idCarrera;
        public String titulo;
        public String descripcion;
        public String observaciones;
    }

    public static class DecisionPropuestaRequest {
        public String estado;
        public String observaciones;
    }

    public static class CrearPropuestaRequest {
        public Integer idCarrera;
        public Integer idTema;
        public String titulo;
        public String temaInvestigacion;
        public String planteamientoProblema;
        public String objetivosGenerales;
        public String objetivosEspecificos;
        public String marcoTeorico;
        public String metodologia;
        public String resultadosEsperados;
        public String bibliografia;
    }

    public static class SeleccionarModalidadRequest {
        public Integer idModalidad;
    }

    public static class SugerirTemaRequest {
        public String titulo;
        public String descripcion;
    }

    public static class AprobarSugerenciaRequest {
        public String observaciones;
    }

    // ✅ FIX: TemaDto ahora incluye idEstudianteSugerente
    public record TemaDto(
            Integer idTema,
            String titulo,
            String descripcion,
            String carrera,
            String docente,
            String estado,
            String observaciones,
            Integer idEstudianteSugerente
    ) {}

    public record PropuestaDto(
            Integer idPropuesta,
            String titulo,
            String tema,
            String estudiante,
            String carrera,
            String estado,
            LocalDate fechaEnvio,
            String observaciones
    ) {}

    public record ModalidadSimpleDto(
            Integer idModalidad,
            String nombre
    ) {}

    public record EstadoModalidadDto(
            boolean tieneModalidad,
            Integer idEleccion,
            Integer idModalidad,
            String modalidad,
            Integer idCarrera,
            List<ModalidadSimpleDto> modalidadesDisponibles
    ) {
        public EstadoModalidadDto withEleccion(Integer nuevoIdEleccion,
                                               Integer nuevaIdModalidad,
                                               String nuevaModalidad) {
            return new EstadoModalidadDto(true, nuevoIdEleccion, nuevaIdModalidad,
                    nuevaModalidad, idCarrera, modalidadesDisponibles);
        }
    }
}