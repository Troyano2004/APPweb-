package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.dtos.CoordinadorDto.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

@Service
public class CoordinadorService {

    private final ProyectoTitulacionRepository proyectoRepo;
    private final UsuarioRepository usuarioRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final DocenteRepository docenteRepo;
    private final ObservacionAdministrativaRepository observacionRepo;
    private final ComisionFormativaRepository comisionRepo;
    private final ComisionMiembroRepository miembroRepo;
    private final ComisionProyectoRepository comisionProyectoRepo;
    private final CarreraRepository carreraRepo;

    private final Dt1AsignacionRepository dt1AsignacionRepo;
    private final Dt1TutorEstudianteRepository dt1TutorRepo;
    private final EstudianteRepository estudianteRepo;
    private final PeriodoTitulacionRepository periodoRepo;

    private final CoordinadorRepository coordinadorRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;

    // ✅ Para llamar los SPs
    private final JdbcTemplate jdbcTemplate;

    public CoordinadorService(
            ProyectoTitulacionRepository proyectoRepo,
            DocumentoTitulacionRepository documentoRepo,
            DocenteRepository docenteRepo,
            ObservacionAdministrativaRepository observacionRepo,
            ComisionFormativaRepository comisionRepo,
            ComisionMiembroRepository miembroRepo,
            ComisionProyectoRepository comisionProyectoRepo,
            CarreraRepository carreraRepo,
            Dt1AsignacionRepository dt1AsignacionRepo,
            Dt1TutorEstudianteRepository dt1TutorRepo,
            EstudianteRepository estudianteRepo,
            PeriodoTitulacionRepository periodoRepo,
            CoordinadorRepository coordinadorRepo,
            DocenteCarreraRepository docenteCarreraRepo,
            JdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepo
    ) {
        this.proyectoRepo          = proyectoRepo;
        this.documentoRepo         = documentoRepo;
        this.docenteRepo           = docenteRepo;
        this.observacionRepo       = observacionRepo;
        this.comisionRepo          = comisionRepo;
        this.miembroRepo           = miembroRepo;
        this.comisionProyectoRepo  = comisionProyectoRepo;
        this.carreraRepo           = carreraRepo;
        this.dt1AsignacionRepo     = dt1AsignacionRepo;
        this.dt1TutorRepo          = dt1TutorRepo;
        this.estudianteRepo        = estudianteRepo;
        this.periodoRepo           = periodoRepo;
        this.coordinadorRepo       = coordinadorRepo;
        this.docenteCarreraRepo    = docenteCarreraRepo;
        this.jdbcTemplate          = jdbcTemplate;
        this.usuarioRepo = usuarioRepo;
    }

    // ==========================================================
    // SIN CAMBIOS - Lo de tu compañero
    // ==========================================================

    public List<SeguimientoProyectoDto> seguimiento() {
        List<SeguimientoProyectoDto> salida = new ArrayList<>();
        for (ProyectoTitulacion proyecto : proyectoRepo.findAll()) {
            SeguimientoProyectoDto dto = new SeguimientoProyectoDto();
            dto.setIdProyecto(proyecto.getIdProyecto());
            dto.setTituloProyecto(proyecto.getTitulo());
            dto.setEstado(proyecto.getEstado());
            dto.setAvance(0);

            if (proyecto.getPropuesta() != null && proyecto.getPropuesta().getEstudiante() != null) {
                Estudiante est = proyecto.getPropuesta().getEstudiante();
                dto.setIdEstudiante(est.getIdEstudiante());
                Usuario u = est.getUsuario();
                dto.setEstudiante(u.getNombres() + " " + u.getApellidos());
            }

            if (proyecto.getDirector() != null && proyecto.getDirector().getUsuario() != null) {
                Usuario u = proyecto.getDirector().getUsuario();
                dto.setDirector(u.getNombres() + " " + u.getApellidos());
            } else {
                dto.setDirector("Sin asignar");
            }

            documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())
                    .ifPresent(doc -> dto.setUltimaRevision(doc.getActualizadoEn()));

            salida.add(dto);
        }
        return salida;
    }

    public List<EstudianteSinDirectorDto> estudiantesSinDirector() {
        List<EstudianteSinDirectorDto> salida = new ArrayList<>();
        for (DocumentoTitulacion doc : documentoRepo.findByDirectorIsNull()) {
            EstudianteSinDirectorDto dto = new EstudianteSinDirectorDto();
            dto.setIdDocumento(doc.getId());
            if (doc.getEstudiante() != null && doc.getEstudiante().getUsuario() != null) {
                Usuario u = doc.getEstudiante().getUsuario();
                dto.setEstudiante(u.getNombres() + " " + u.getApellidos());
                if (doc.getEstudiante().getCarrera() != null) {
                    dto.setCarrera(doc.getEstudiante().getCarrera().getNombre());
                }
            }
            if (doc.getProyecto() != null) {
                dto.setProyecto(doc.getProyecto().getTitulo());
            }
            salida.add(dto);
        }
        return salida;
    }

    public List<DirectorCargaDto> cargaDirectores() {
        List<DirectorCargaDto> salida = new ArrayList<>();
        for (Docente docente : docenteRepo.findAll()) {
            DirectorCargaDto dto = new DirectorCargaDto();
            dto.setIdDocente(docente.getIdDocente());
            if (docente.getUsuario() != null) {
                dto.setDirector(docente.getUsuario().getNombres() + " " + docente.getUsuario().getApellidos());
            }
            dto.setProyectosAsignados(proyectoRepo.countByDirector_IdDocente(docente.getIdDocente()));
            salida.add(dto);
        }
        return salida;
    }

    @Transactional
    public void asignarDirector(AsignarDirectorRequest request) {
        DocumentoTitulacion doc = documentoRepo.findById(request.getIdDocumento())
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        Docente docente = docenteRepo.findById(request.getIdDocente())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        doc.setDirector(docente);
        documentoRepo.save(doc);

        if (doc.getProyecto() != null) {
            ProyectoTitulacion proyecto = doc.getProyecto();
            proyecto.setDirector(docente);
            proyectoRepo.save(proyecto);

            ObservacionAdministrativa obs = new ObservacionAdministrativa();
            obs.setProyecto(proyecto);
            obs.setTipo("ADMINISTRATIVO");
            obs.setDetalle(request.getMotivo() != null ? request.getMotivo() : "Asignación de director");
            obs.setCreadoPor("Coordinación");
            observacionRepo.save(obs);
        }
    }

    @Transactional
    public void validarProyecto(Integer idProyecto) {
        ProyectoTitulacion proyecto = proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        Optional<DocumentoTitulacion> doc = documentoRepo.findByProyecto_IdProyecto(idProyecto);
        if (doc.isEmpty() || doc.get().getEstado() != EstadoDocumento.APROBADO_POR_DIRECTOR) {
            throw new RuntimeException("El proyecto no cumple con el requisito de aprobación por director");
        }

        proyecto.setEstado("VALIDADO_POR_COORDINACION");
        proyectoRepo.save(proyecto);
    }

    public DocumentoTitulacionDto documentoPorProyecto(Integer idProyecto) {
        DocumentoTitulacion doc = documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado para el proyecto"));
        return toDocumentoDto(doc);
    }

    private DocumentoTitulacionDto toDocumentoDto(DocumentoTitulacion d) {
        DocumentoTitulacionDto dto = new DocumentoTitulacionDto();
        dto.setId(d.getId());
        dto.setEstado(d.getEstado());
        dto.setIdEstudiante(d.getEstudiante() != null ? d.getEstudiante().getIdEstudiante() : null);
        dto.setIdDirector(d.getDirector() != null ? d.getDirector().getIdDocente() : null);
        dto.setTitulo(d.getTitulo());
        dto.setCiudad(d.getCiudad());
        dto.setAnio(d.getAnio());
        dto.setResumen(d.getResumen());
        dto.setAbstractText(d.getAbstractText());
        dto.setIntroduccion(d.getIntroduccion());
        dto.setPlanteamientoProblema(d.getPlanteamientoProblema());
        dto.setObjetivoGeneral(d.getObjetivoGeneral());
        dto.setObjetivosEspecificos(d.getObjetivosEspecificos());
        dto.setJustificacion(d.getJustificacion());
        dto.setMarcoTeorico(d.getMarcoTeorico());
        dto.setMetodologia(d.getMetodologia());
        dto.setResultados(d.getResultados());
        dto.setDiscusion(d.getDiscusion());
        dto.setConclusiones(d.getConclusiones());
        dto.setRecomendaciones(d.getRecomendaciones());
        dto.setBibliografia(d.getBibliografia());
        dto.setAnexos(d.getAnexos());
        return dto;
    }

    public List<ObservacionAdministrativaDto> observaciones(Integer idProyecto) {
        List<ObservacionAdministrativa> observaciones =
                idProyecto == null
                        ? observacionRepo.findAll()
                        : observacionRepo.findByProyecto_IdProyectoOrderByCreadoEnDesc(idProyecto);
        List<ObservacionAdministrativaDto> salida = new ArrayList<>();
        for (ObservacionAdministrativa obs : observaciones) {
            ObservacionAdministrativaDto dto = new ObservacionAdministrativaDto();
            dto.setId(obs.getId());
            dto.setIdProyecto(obs.getProyecto().getIdProyecto());
            dto.setProyecto(obs.getProyecto().getTitulo());
            dto.setTipo(obs.getTipo());
            dto.setDetalle(obs.getDetalle());
            dto.setCreadoPor(obs.getCreadoPor());
            dto.setCreadoEn(obs.getCreadoEn());
            salida.add(dto);
        }
        return salida;
    }

    @Transactional
    public ObservacionAdministrativaDto crearObservacion(CrearObservacionAdministrativaRequest request) {
        ProyectoTitulacion proyecto = proyectoRepo.findById(request.getIdProyecto())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ObservacionAdministrativa obs = new ObservacionAdministrativa();
        obs.setProyecto(proyecto);
        obs.setTipo(request.getTipo());
        obs.setDetalle(request.getDetalle());
        obs.setCreadoPor(request.getCreadoPor());
        obs = observacionRepo.save(obs);

        ObservacionAdministrativaDto dto = new ObservacionAdministrativaDto();
        dto.setId(obs.getId());
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setProyecto(proyecto.getTitulo());
        dto.setTipo(obs.getTipo());
        dto.setDetalle(obs.getDetalle());
        dto.setCreadoPor(obs.getCreadoPor());
        dto.setCreadoEn(obs.getCreadoEn());
        return dto;
    }

    public List<ComisionFormativaDto> listarComisiones() {
        List<ComisionFormativaDto> salida = new ArrayList<>();
        for (ComisionFormativa comision : comisionRepo.findAll()) {
            ComisionFormativaDto dto = new ComisionFormativaDto();
            dto.setIdComision(comision.getIdComision());
            dto.setIdCarrera(comision.getCarrera() != null ? comision.getCarrera().getIdCarrera() : null);
            dto.setPeriodoAcademico(comision.getPeriodoAcademico());
            dto.setEstado(comision.getEstado());
            if (comision.getCarrera() != null) {
                dto.setCarrera(comision.getCarrera().getNombre());
            }

            List<ComisionMiembroDto> miembros = new ArrayList<>();
            for (ComisionMiembro miembro : miembroRepo.findByIdcomision_IdComision(comision.getIdComision())) {
                ComisionMiembroDto miembroDto = new ComisionMiembroDto();
                miembroDto.setIdDocente(miembro.getDocente().getIdDocente());
                if (miembro.getDocente().getUsuario() != null) {
                    miembroDto.setDocente(miembro.getDocente().getUsuario().getNombres()
                            + " " + miembro.getDocente().getUsuario().getApellidos());
                }
                miembroDto.setCargo(miembro.getCargo());
                miembros.add(miembroDto);
            }
            dto.setMiembros(miembros);
            salida.add(dto);
        }
        return salida;
    }

    @Transactional
    public ComisionFormativaDto crearComision(CrearComisionRequest request) {
        Carrera carrera = carreraRepo.findById(request.getIdCarrera())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        ComisionFormativa comision = new ComisionFormativa();
        comision.setCarrera(carrera);
        comision.setPeriodoAcademico(request.getPeriodoAcademico().trim());
        comision.setFechaInicio(LocalDate.now());
        comision.setEstado(normalizarEstadoComision(request.getEstado()));
        comision = comisionRepo.save(comision);

        ComisionFormativaDto dto = new ComisionFormativaDto();
        dto.setIdComision(comision.getIdComision());
        dto.setIdCarrera(carrera.getIdCarrera());
        dto.setCarrera(carrera.getNombre());
        dto.setPeriodoAcademico(comision.getPeriodoAcademico());
        dto.setEstado(comision.getEstado());
        return dto;
    }

    @Transactional
    public void asignarMiembros(Integer idComision, AsignarMiembrosRequest request) {
        ComisionFormativa comision = comisionRepo.findById(idComision)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));

        if (request.getMiembros() == null || request.getMiembros().isEmpty()) return;

        for (ComisionMiembroDto miembroDto : request.getMiembros()) {
            if (miembroDto.getIdDocente() == null
                    || miembroDto.getCargo() == null
                    || miembroDto.getCargo().trim().isEmpty()) continue;

            Docente docente = docenteRepo.findById(miembroDto.getIdDocente())
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

            comisionmiembroid id = new comisionmiembroid(comision.getIdComision(), docente.getIdDocente());
            ComisionMiembro miembro = miembroRepo.findById(id).orElseGet(ComisionMiembro::new);
            miembro.setId(id);
            miembro.setIdcomision(comision);
            miembro.setDocente(docente);
            miembro.setCargo(miembroDto.getCargo().trim().toUpperCase());
            miembroRepo.save(miembro);
        }
    }

    public List<CatalogoCarreraDto> carreras() {
        List<CatalogoCarreraDto> salida = new ArrayList<>();
        for (Carrera carrera : carreraRepo.findAll()) {
            CatalogoCarreraDto dto = new CatalogoCarreraDto();
            dto.setIdCarrera(carrera.getIdCarrera());
            dto.setNombre(carrera.getNombre());
            salida.add(dto);
        }
        return salida;
    }

    private String normalizarEstadoComision(String estado) {
        if (estado == null || estado.trim().isEmpty()) return "ACTIVA";
        return estado.trim().toUpperCase().equals("INACTIVA") ? "INACTIVA" : "ACTIVA";
    }

    @Transactional
    public void eliminarComision(Integer idComision) {
        ComisionFormativa comision = comisionRepo.findById(idComision)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));

        List<ComisionProyecto> comisionesProyecto = comisionProyectoRepo.findByComision_IdComision(idComision);
        if (!comisionesProyecto.isEmpty()) comisionProyectoRepo.deleteAll(comisionesProyecto);

        List<ComisionMiembro> miembros = miembroRepo.findByIdcomision_IdComision(idComision);
        if (!miembros.isEmpty()) miembroRepo.deleteAll(miembros);

        comisionRepo.delete(comision);
    }

    @Transactional
    public void asignarComisionAProyecto(AsignarComisionProyectoRequest request) {
        ComisionFormativa comision = comisionRepo.findById(request.getIdComision())
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));
        ProyectoTitulacion proyecto = proyectoRepo.findById(request.getIdProyecto())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        ComisionProyecto comisionProyecto = comisionProyectoRepo
                .findByProyecto_IdProyecto(proyecto.getIdProyecto())
                .orElseGet(ComisionProyecto::new);

        comisionProyecto.setComision(comision);
        comisionProyecto.setProyecto(proyecto);
        comisionProyecto.setFechaConformacion(request.getFechaConformacion() != null
                ? request.getFechaConformacion() : LocalDate.now());
        comisionProyecto.setResolucionActa(request.getResolucionActa());
        comisionProyecto.setObservacion(request.getObservacion());
        comisionProyecto.setEstado(request.getEstado() != null ? request.getEstado() : "COMISION_CONFORMADA");
        comisionProyectoRepo.save(comisionProyecto);
    }

    public Optional<ComisionProyecto> comisionPorProyecto(Integer idProyecto) {
        return comisionProyectoRepo.findByProyecto_IdProyecto(idProyecto);
    }

    // ==========================================================
    // ✅ DT1 - Ahora usando SPs via JdbcTemplate
    // ==========================================================

    public Dt1AsignacionResponse crearAsignacionDt1(Dt1AsignacionCreateRequest req) {
        if (req == null || req.getIdUsuario() == null || req.getIdCarrera() == null
                || req.getIdPeriodo() == null || req.getIdDocente() == null) {
            throw new RuntimeException("Datos incompletos");
        }

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "CALL sp_habilitar_docente_dt1(?, ?, ?, ?, NULL, NULL, NULL)",
                req.getIdUsuario(), req.getIdDocente(),
                req.getIdCarrera(), req.getIdPeriodo()
        );

        int codigo = toInt(result.get("p_codigo"));
        String mensaje = String.valueOf(result.get("p_mensaje"));
        if (codigo != 1) throw new RuntimeException(mensaje);

        Dt1AsignacionResponse r = new Dt1AsignacionResponse();
        r.setIdDocente(req.getIdDocente());
        r.setIdCarrera(req.getIdCarrera());
        r.setIdPeriodo(req.getIdPeriodo());
        r.setActivo(true);
        return r;
    }

    public Dt1AsignarTutorResponse asignarTutorDt1(Dt1AsignarTutorRequest req) {
        if (req == null || req.getIdUsuario() == null || req.getIdEstudiante() == null
                || req.getIdDocente() == null || req.getIdPeriodo() == null) {
            throw new RuntimeException("Datos incompletos");
        }

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "CALL sp_asignar_tutor_dt1(?, ?, ?, ?, NULL, NULL, NULL)",
                req.getIdUsuario(), req.getIdEstudiante(),
                req.getIdDocente(), req.getIdPeriodo()
        );

        int codigo = toInt(result.get("p_codigo"));
        String mensaje = String.valueOf(result.get("p_mensaje"));
        if (codigo != 1) throw new RuntimeException(mensaje);

        Dt1AsignarTutorResponse r = new Dt1AsignarTutorResponse();
        r.setIdEstudiante(req.getIdEstudiante());
        r.setIdDocente(req.getIdDocente());
        r.setIdPeriodo(req.getIdPeriodo());
        r.setEstado("ASIGNADO");
        return r;
    }

    // ==========================================================
    // ✅ Info académica DT1 - sin cambios (solo lecturas)
    // ==========================================================
    public InformacionAcademicaDt1Dto infoDt1(Integer idUsuario) {

        Coordinador coord = coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(idUsuario)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ES_COORDINADOR"));

        Carrera carrera = coord.getCarrera();
        if (carrera == null) throw new RuntimeException("COORDINADOR_SIN_CARRERA");

        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new RuntimeException("NO_HAY_PERIODO_ACTIVO"));

        InformacionAcademicaDt1Dto dto = new InformacionAcademicaDt1Dto();
        dto.setIdCarrera(carrera.getIdCarrera());
        dto.setCarrera(carrera.getNombre());
        dto.setIdPeriodoAcademico(periodo.getIdPeriodo());
        dto.setPeriodoAcademico(periodo.getDescripcion());

        // Docentes de la carrera (TAB 1)
        List<DocenteCarrera> dc = docenteCarreraRepo.findByCarrera_IdCarreraAndActivoTrue(carrera.getIdCarrera());
        List<InformacionAcademicaDt1Dto.DocenteItemDto> docentesCarrera = new ArrayList<>();
        for (DocenteCarrera x : dc) {
            Docente d = x.getDocente();
            if (d == null || d.getUsuario() == null) continue;
            InformacionAcademicaDt1Dto.DocenteItemDto it = new InformacionAcademicaDt1Dto.DocenteItemDto();
            it.setIdDocente(d.getIdDocente());
            it.setNombre(nombreCompleto(d.getUsuario()));
            docentesCarrera.add(it);
        }
        dto.setDocentesCarrera(docentesCarrera);

        // Docentes DT1 habilitados (TAB 2)
        List<Dt1Asignacion> asignaciones = dt1AsignacionRepo
                .findByCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(
                        carrera.getIdCarrera(), periodo.getIdPeriodo());
        List<InformacionAcademicaDt1Dto.DocenteItemDto> docentes = new ArrayList<>();
        for (Dt1Asignacion a : asignaciones) {
            Docente d = a.getDocente();
            if (d == null || d.getUsuario() == null) continue;
            InformacionAcademicaDt1Dto.DocenteItemDto it = new InformacionAcademicaDt1Dto.DocenteItemDto();
            it.setIdDocente(d.getIdDocente());
            it.setNombre(nombreCompleto(d.getUsuario()));
            docentes.add(it);
        }
        dto.setDocentesDt1(docentes);

        // Estudiantes sin tutor (TAB 2)
        List<Dt1TutorEstudiante> tutoresPeriodo =
                dt1TutorRepo.findByPeriodo_IdPeriodoAndActivoTrue(periodo.getIdPeriodo());
        java.util.Set<Integer> conTutor = new java.util.HashSet<>();
        for (Dt1TutorEstudiante t : tutoresPeriodo) {
            if (t.getEstudiante() != null) conTutor.add(t.getEstudiante().getIdEstudiante());
        }

        List<Estudiante> estudiantesCarrera = estudianteRepo.findByCarrera_IdCarrera(carrera.getIdCarrera());
        List<InformacionAcademicaDt1Dto.EstudianteItemDto> estudiantes = new ArrayList<>();
        for (Estudiante e : estudiantesCarrera) {
            if (e.getUsuario() == null) continue;
            if (conTutor.contains(e.getIdEstudiante())) continue;
            InformacionAcademicaDt1Dto.EstudianteItemDto it = new InformacionAcademicaDt1Dto.EstudianteItemDto();
            it.setIdEstudiante(e.getIdEstudiante());
            it.setNombre(nombreCompleto(e.getUsuario()));
            estudiantes.add(it);
        }
        dto.setEstudiantesDisponibles(estudiantes);

        return dto;
    }
    public List<CoordinadorAdminResponse>listarCoordinadores()
    {
        return coordinadorRepo.findAll().stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }
    @Transactional
    public CoordinadorAdminResponse asignarCoordinador(AsignarCoordinadorRequest req)
    {
        if (coordinadorRepo.existsByCarrera_IdCarreraAndActivoTrue(req.getIdCarrera()))
        {
            throw new RuntimeException("El coordinador existe en el sistema");
        }
        Usuario u = usuarioRepo.findById(req.getIdUsuario()).orElseThrow(()-> new RuntimeException("Usuario no encontrado"));
        if(! u.getRolAsignado().equalsIgnoreCase("Coordinador"))
        {
            throw new RuntimeException("EL_USUARIO_NO_TIENE_ROL_COORDINADOR");
        }
        Carrera carrera = carreraRepo.findById(req.getIdCarrera()).orElseThrow(()-> new RuntimeException
                ("Carrera no encontrada"));

        Coordinador coordinador = new Coordinador();
        coordinador.setUsuario(u);
        coordinador.setCarrera(carrera);
        coordinador.setActivo(true);
        coordinadorRepo.save(coordinador);
        return toDto(coordinador);
    }
    public CoordinadorAdminResponse cambiarEstado(Integer idCoordinador, boolean activo)
    {
        Coordinador coordinador = coordinadorRepo.findById(idCoordinador).orElseThrow(()->new RuntimeException("COORDINADOR_NO_ENCONTRADO"));
        if(activo && coordinadorRepo.existsByCarrera_IdCarreraAndActivoTrue(coordinador.getCarrera().getIdCarrera()))
        {
            throw new RuntimeException("YA_EXISTE_COORDINADOR_ACTIVO_EN_ESTA_CARRERA");
        }
        coordinador.setActivo(activo);
        coordinadorRepo.save(coordinador);
        return toDto(coordinador);
    }
    public List<CoordinadorAdminResponse> listarUsuariosCoordinador() {
        List<Integer>hayCoordinador = coordinadorRepo.findByActivoTrue().stream().map(c->
          c.getUsuario().getIdUsuario()).toList();

        return usuarioRepo.findByRolAsignado("COORDINADOR")
                .stream().filter(u -> !hayCoordinador.contains(u.getIdUsuario()))
                .map(u -> {
                    CoordinadorAdminResponse dto = new CoordinadorAdminResponse();
                    dto.setIdUsuario(u.getIdUsuario());
                    dto.setNombres(u.getNombres());
                    dto.setApellidos(u.getApellidos());
                    dto.setUsername(u.getUsername());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    // ==========================================================
    // Helpers privados
    // ==========================================================
    private CoordinadorAdminResponse toDto(Coordinador c)
    {
        CoordinadorAdminResponse dto = new CoordinadorAdminResponse();
        dto.setIdCoordinador(c.getIdCoordinador());
        dto.setActivo(c.getActivo());
        if (c.getUsuario() != null) {
            dto.setApellidos(c.getUsuario().getApellidos());
            dto.setNombres(c.getUsuario().getNombres());
            dto.setUsername(c.getUsuario().getUsername());
            dto.setIdUsuario(c.getUsuario().getIdUsuario());
        }
        if (c.getCarrera() != null) {
            dto.setIdCarrera(c.getCarrera().getIdCarrera());
            dto.setCarrera(c.getCarrera().getNombre());
        }
        return dto;
    }
    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        String n = u.getNombres()   == null ? "" : u.getNombres().trim();
        String a = u.getApellidos() == null ? "" : u.getApellidos().trim();
        return (n + " " + a).trim();
    }

    private int toInt(Object val) {
        if (val == null) return -1;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return -1; }
    }
}