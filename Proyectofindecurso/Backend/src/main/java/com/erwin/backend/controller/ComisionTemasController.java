package com.erwin.backend.controller;

import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
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

    public ComisionTemasController(BancoTemasRepository bancoTemasRepository,
                                   PropuestaTitulacionRepository propuestaRepository,
                                   ComisionMiembroRepository comisionMiembroRepository,
                                   DocenteRepository docenteRepository,
                                   EstudianteRepository estudianteRepository,
                                   CarreraRepository carreraRepository,
                                   EleccionTitulacionRepository eleccionRepository,
                                   ModalidadTitulacionRepository modalidadRepository,
                                   CarreraModalidadRepository carreraModalidadRepository,
                                   PeriodoTitulacionRepository periodoRepository) {
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
    }

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

        Integer idCarrera = estudiante.getCarrera().getIdCarrera();
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


    private EleccionTitulacion obtenerEleccionVigente(Integer idEstudiante) {
        return periodoRepository.findByActivoTrue()
                .flatMap(periodo -> eleccionRepository.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(idEstudiante, periodo.getIdPeriodo()))
                .orElseGet(() -> eleccionRepository.findByEstudiante_IdEstudiante(idEstudiante)
                        .stream()
                        .max(Comparator.comparing(EleccionTitulacion::getIdEleccion))
                        .orElse(null));
    }

    private List<ModalidadSimpleDto> obtenerModalidadesDisponibles(Integer idCarrera) {
        if (idCarrera == null) {
            return List.of();
        }

        return carreraModalidadRepository.findById_IdCarreraAndActivoTrue(idCarrera)
                .stream()
                .map(cm -> new ModalidadSimpleDto(
                        cm.getModalidad().getIdModalidad(),
                        cm.getModalidad().getNombre()
                ))
                .toList();
    }

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

        if (req == null || req.titulo == null || req.titulo.trim().isEmpty() || req.descripcion == null || req.descripcion.trim().isEmpty()) {
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

        PropuestaTitulacion propuesta = propuestaRepository.findById(idPropuesta)
                .orElseThrow(() -> new RuntimeException("Propuesta no encontrada"));

        String estado = req.estado == null ? "" : req.estado.trim().toUpperCase();
        if (!estado.equals("APROBADA") && !estado.equals("RECHAZADA")) {
            throw new RuntimeException("El estado debe ser APROBADA o RECHAZADA");
        }

        propuesta.setEstado(estado);
        propuesta.setFechaRevision(LocalDate.now());
        propuesta.setObservacionesComision(req.observaciones);

        return toPropuestaDto(propuestaRepository.save(propuesta));
    }

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

        PropuestaTitulacion propuesta = new PropuestaTitulacion();
        propuesta.setEleccion(eleccion);
        propuesta.setEstudiante(estudiante);
        propuesta.setCarrera(carrera);
        propuesta.setTitulo(req.titulo.trim());
        propuesta.setTemaInvestigacion(valueOrDefault(req.temaInvestigacion, "Pendiente de definir"));
        propuesta.setPlanteamientoProblema(valueOrDefault(req.planteamientoProblema, "Pendiente de definir"));
        propuesta.setObjetivosGenerales(valueOrDefault(req.objetivosGenerales, "Pendiente de definir"));
        propuesta.setObjetivosEspecificos(valueOrDefault(req.objetivosEspecificos, "Pendiente de definir"));
        propuesta.setMarcoTeorico(valueOrDefault(req.marcoTeorico, "Pendiente de definir"));
        propuesta.setMetodologia(valueOrDefault(req.metodologia, "Pendiente de definir"));
        propuesta.setResultadosEsperados(valueOrDefault(req.resultadosEsperados, "Pendiente de definir"));
        propuesta.setBibliografia(valueOrDefault(req.bibliografia, "Pendiente de definir"));

        propuesta.setEstado("EN_REVISION");
        propuesta.setFechaEnvio(LocalDate.now());

        if (req.idTema != null) {
            BancoTemas tema = bancoTemasRepository.findById(req.idTema)
                    .orElseThrow(() -> new RuntimeException("Tema seleccionado no existe"));
            propuesta.setTema(tema);
            if (propuesta.getTemaInvestigacion() == null || propuesta.getTemaInvestigacion().isBlank() || propuesta.getTemaInvestigacion().equals("Pendiente de definir")) {
                propuesta.setTemaInvestigacion(tema.getTitulo());
            }
        }

        return toPropuestaDto(propuestaRepository.save(propuesta));
    }

    @GetMapping("/estudiante/{idEstudiante}/propuestas")
    public List<PropuestaDto> propuestasEstudiante(@PathVariable Integer idEstudiante) {
        return propuestaRepository.findByEstudiante_IdEstudiante(idEstudiante)
                .stream()
                .sorted(Comparator.comparing(PropuestaTitulacion::getIdPropuesta).reversed())
                .map(this::toPropuestaDto)
                .toList();
    }

    @GetMapping("/estudiante/{idEstudiante}/temas-disponibles")
    public List<TemaDto> temasDisponiblesEstudiante(@PathVariable Integer idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        Integer idCarreraEstudiante = estudiante.getCarrera() != null ? estudiante.getCarrera().getIdCarrera() : null;

        return bancoTemasRepository.findAll().stream()
                .filter(t -> idCarreraEstudiante == null
                        || (t.getCarrera() != null && idCarreraEstudiante.equals(t.getCarrera().getIdCarrera())))
                .sorted(Comparator.comparing(BancoTemas::getIdTema).reversed())
                .map(this::toTemaDto)
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
            docente = tema.getDocenteProponente().getUsuario().getNombres() + " " + tema.getDocenteProponente().getUsuario().getApellidos();
        }

        String carrera = tema.getCarrera() != null ? tema.getCarrera().getNombre() : "Sin carrera";

        return new TemaDto(
                tema.getIdTema(),
                tema.getTitulo(),
                tema.getDescripcion(),
                carrera,
                docente,
                tema.getEstado(),
                tema.getObservaciones()
        );
    }

    private PropuestaDto toPropuestaDto(PropuestaTitulacion propuesta) {
        String estudiante = "Sin estudiante";
        if (propuesta.getEstudiante() != null && propuesta.getEstudiante().getUsuario() != null) {
            estudiante = propuesta.getEstudiante().getUsuario().getNombres() + " " + propuesta.getEstudiante().getUsuario().getApellidos();
        }

        String carrera = propuesta.getCarrera() != null ? propuesta.getCarrera().getNombre() : "Sin carrera";
        String tema = propuesta.getTema() != null ? propuesta.getTema().getTitulo() : propuesta.getTemaInvestigacion();

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

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

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

    public record TemaDto(
            Integer idTema,
            String titulo,
            String descripcion,
            String carrera,
            String docente,
            String estado,
            String observaciones
    ) {
    }

    public record PropuestaDto(
            Integer idPropuesta,
            String titulo,
            String tema,
            String estudiante,
            String carrera,
            String estado,
            LocalDate fechaEnvio,
            String observaciones
    ) {
    }

    public record ModalidadSimpleDto(
            Integer idModalidad,
            String nombre
    ) {
    }

    public record EstadoModalidadDto(
            boolean tieneModalidad,
            Integer idEleccion,
            Integer idModalidad,
            String modalidad,
            Integer idCarrera,
            List<ModalidadSimpleDto> modalidadesDisponibles
    ) {
        public EstadoModalidadDto withEleccion(Integer nuevoIdEleccion, Integer nuevaIdModalidad, String nuevaModalidad) {
            return new EstadoModalidadDto(true, nuevoIdEleccion, nuevaIdModalidad, nuevaModalidad, idCarrera, modalidadesDisponibles);
        }
    }
}
