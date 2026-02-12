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

    public ComisionTemasController(BancoTemasRepository bancoTemasRepository,
                                   PropuestaTitulacionRepository propuestaRepository,
                                   ComisionMiembroRepository comisionMiembroRepository,
                                   DocenteRepository docenteRepository,
                                   EstudianteRepository estudianteRepository,
                                   CarreraRepository carreraRepository,
                                   EleccionTitulacionRepository eleccionRepository) {
        this.bancoTemasRepository = bancoTemasRepository;
        this.propuestaRepository = propuestaRepository;
        this.comisionMiembroRepository = comisionMiembroRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.carreraRepository = carreraRepository;
        this.eleccionRepository = eleccionRepository;
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

        EleccionTitulacion eleccion = eleccionRepository.findByEstudiante_IdEstudiante(idEstudiante)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El estudiante no tiene elección de titulación registrada"));

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
}
