package com.erwin.backend.service;

import com.erwin.backend.dtos.CoordinadorDtos.*;
import com.erwin.backend.dtos.DocumentoTitulacionDto;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CoordinadorService {
    private final ProyectoTitulacionRepository proyectoRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final DocenteRepository docenteRepo;
    private final ObservacionAdministrativaRepository observacionRepo;
    private final ComisionFormativaRepository comisionRepo;
    private final ComisionMiembroRepository miembroRepo;
    private final ComisionProyectoRepository comisionProyectoRepo;
    private final CarreraRepository carreraRepo;

    public CoordinadorService(ProyectoTitulacionRepository proyectoRepo,
                              DocumentoTitulacionRepository documentoRepo,
                              DocenteRepository docenteRepo,
                              ObservacionAdministrativaRepository observacionRepo,
                              ComisionFormativaRepository comisionRepo,
                              ComisionMiembroRepository miembroRepo,
                              ComisionProyectoRepository comisionProyectoRepo,
                              CarreraRepository carreraRepo) {
        this.proyectoRepo = proyectoRepo;
        this.documentoRepo = documentoRepo;
        this.docenteRepo = docenteRepo;
        this.observacionRepo = observacionRepo;
        this.comisionRepo = comisionRepo;
        this.miembroRepo = miembroRepo;
        this.comisionProyectoRepo = comisionProyectoRepo;
        this.carreraRepo = carreraRepo;
    }

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
                    miembroDto.setDocente(miembro.getDocente().getUsuario().getNombres() + " " + miembro.getDocente().getUsuario().getApellidos());
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

        if (request.getMiembros() == null || request.getMiembros().isEmpty()) {
            return;
        }

        for (ComisionMiembroDto miembroDto : request.getMiembros()) {
            if (miembroDto.getIdDocente() == null || miembroDto.getCargo() == null || miembroDto.getCargo().trim().isEmpty()) {
                continue;
            }

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
        if (estado == null || estado.trim().isEmpty()) {
            return "ACTIVA";
        }
        String estadoNormalizado = estado.trim().toUpperCase();
        return estadoNormalizado.equals("INACTIVA") ? "INACTIVA" : "ACTIVA";
    }

    @Transactional
    public void eliminarComision(Integer idComision) {
        ComisionFormativa comision = comisionRepo.findById(idComision)
                .orElseThrow(() -> new RuntimeException("Comisión no encontrada"));

        List<ComisionProyecto> comisionesProyecto = comisionProyectoRepo.findByComision_IdComision(idComision);
        if (!comisionesProyecto.isEmpty()) {
            comisionProyectoRepo.deleteAll(comisionesProyecto);
        }

        List<ComisionMiembro> miembros = miembroRepo.findByIdcomision_IdComision(idComision);
        if (!miembros.isEmpty()) {
            miembroRepo.deleteAll(miembros);
        }

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
                ? request.getFechaConformacion()
                : LocalDate.now());
        comisionProyecto.setResolucionActa(request.getResolucionActa());
        comisionProyecto.setObservacion(request.getObservacion());
        comisionProyecto.setEstado(request.getEstado() != null
                ? request.getEstado()
                : "COMISION_CONFORMADA");
        comisionProyectoRepo.save(comisionProyecto);
    }

    public Optional<ComisionProyecto> comisionPorProyecto(Integer idProyecto) {
        return comisionProyectoRepo.findByProyecto_IdProyecto(idProyecto);
    }
}
