package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.erwin.backend.repository.ObservacionDocumentoRepository;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.SustentacionRepository;
import com.erwin.backend.repository.TribunalProyectoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoTitulacionService {

    private final DocumentoTitulacionRepository docRepo;
    private final ObservacionDocumentoRepository obsRepo;
    private final EstudianteRepository estudianteRepo;
    private final DocenteRepository docenteRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final SustentacionRepository sustentacionRepo;
    private final EmailService emailService; // ✅ NUEVO

    public DocumentoTitulacionService(DocumentoTitulacionRepository docRepo,
                                      ObservacionDocumentoRepository obsRepo,
                                      EstudianteRepository estudianteRepo,
                                      DocenteRepository docenteRepo,
                                      TribunalProyectoRepository tribunalRepo,
                                      SustentacionRepository sustentacionRepo,
                                      EmailService emailService) { // ✅ NUEVO
        this.docRepo = docRepo;
        this.obsRepo = obsRepo;
        this.estudianteRepo = estudianteRepo;
        this.docenteRepo = docenteRepo;
        this.tribunalRepo = tribunalRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.emailService = emailService; // ✅ NUEVO
    }

    // ====== ESTUDIANTE ======
    public DocumentoTitulacionDto obtenerOMiDocumento(Integer idEstudiante) {
        DocumentoTitulacion doc = docRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseGet(() -> crearDocumentoVacio(idEstudiante));
        return toDto(doc);
    }

    @Transactional
    public DocumentoTitulacionDto guardarCambios(Integer idEstudiante, DocumentoUpdateRequest req) {
        DocumentoTitulacion doc = docRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));

        if (!(doc.getEstado() == EstadoDocumento.BORRADOR || doc.getEstado() == EstadoDocumento.CORRECCION_REQUERIDA)) {
            throw new RuntimeException("No puedes editar en estado: " + doc.getEstado());
        }

        aplicarCambios(doc, req);
        return toDto(docRepo.save(doc));
    }

    @Transactional
    public void enviarRevision(Integer idEstudiante) {
        DocumentoTitulacion doc = docRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));

        if (!(doc.getEstado() == EstadoDocumento.BORRADOR || doc.getEstado() == EstadoDocumento.CORRECCION_REQUERIDA)) {
            throw new RuntimeException("No puedes enviar a revisión en estado: " + doc.getEstado());
        }
        doc.setEstado(EstadoDocumento.EN_REVISION);
        docRepo.save(doc);
    }

    // ====== DIRECTOR ======
    public List<DocumentoTitulacionDto> listarPendientesDirector(Integer idDocente) {
        return docRepo.findByDirector_IdDocente(idDocente)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ObservacionDto agregarObservacion(Integer idDocente, Integer idDocumento, CrearObservacionRequest req) {
        DocumentoTitulacion doc = docRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));

        if (doc.getEstado() != EstadoDocumento.EN_REVISION) {
            throw new RuntimeException("Solo se puede observar si está EN_REVISION");
        }

        Docente autor = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no existe"));

        ObservacionDocumento obs = new ObservacionDocumento();
        obs.setDocumento(doc);
        obs.setSeccion(req.getSeccion());
        obs.setComentario(req.getComentario());
        obs.setEstado(EstadoObservacion.PENDIENTE);
        obs.setAutor(autor);

        obs = obsRepo.save(obs);
        return toObsDto(obs);
    }

    @Transactional
    public void devolverConObservaciones(Integer idDocente, Integer idDocumento) {
        DocumentoTitulacion doc = docRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));

        if (doc.getEstado() != EstadoDocumento.EN_REVISION) {
            throw new RuntimeException("Documento no está EN_REVISION");
        }

        doc.setEstado(EstadoDocumento.CORRECCION_REQUERIDA);
        docRepo.save(doc);
    }

    @Transactional
    public void aprobar(Integer idDocente, Integer idDocumento) {
        DocumentoTitulacion doc = docRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));

        if (doc.getEstado() != EstadoDocumento.EN_REVISION) {
            throw new RuntimeException("Documento no está EN_REVISION");
        }

        doc.setEstado(EstadoDocumento.APROBADO_POR_DIRECTOR);
        docRepo.save(doc);

        // ✅ Notificar al estudiante que su documento fue aprobado
        try {
            Estudiante est = doc.getEstudiante();
            String emailEst = est != null && est.getUsuario() != null
                    ? est.getUsuario().getCorreoInstitucional() : null;
            if (emailEst != null && !emailEst.isBlank()) {
                String nombreEst = (est.getUsuario().getNombres() + " " +
                        est.getUsuario().getApellidos()).trim();
                String nombreDirector = doc.getDirector() != null && doc.getDirector().getUsuario() != null
                        ? (doc.getDirector().getUsuario().getNombres() + " " +
                        doc.getDirector().getUsuario().getApellidos()).trim()
                        : "Tu Director TIC";
                String periodo = doc.getProyecto() != null && doc.getProyecto().getPeriodo() != null
                        ? doc.getProyecto().getPeriodo().getDescripcion() : "";
                emailService.notificarDocumentoAprobado(
                        emailEst,
                        nombreEst,
                        doc.getTitulo(),
                        nombreDirector,
                        periodo
                );
            }
        } catch (Exception e) {
            System.err.println("Error al notificar documento aprobado: " + e.getMessage());
        }
    }

    @Transactional
    public void marcarObservacionAtendida(Integer idEstudiante, Integer idObservacion) {
        ObservacionDocumento obs = obsRepo.findById(idObservacion)
                .orElseThrow(() -> new RuntimeException("Observación no existe"));

        DocumentoTitulacion doc = obs.getDocumento();
        if (doc == null || doc.getEstudiante() == null || !doc.getEstudiante().getIdEstudiante().equals(idEstudiante)) {
            throw new RuntimeException("No autorizado para actualizar esta observación");
        }

        obs.setEstado(EstadoObservacion.ATENDIDA);
        obsRepo.save(obs);
    }

    // ====== OBSERVACIONES (para estudiante/ver) ======
    public List<ObservacionDto> listarObservaciones(Integer idDocumento) {
        return obsRepo.findByDocumento_IdOrderByCreadoEnDesc(idDocumento)
                .stream().map(this::toObsDto).toList();
    }

    // ====== Helpers ======
    private DocumentoTitulacion crearDocumentoVacio(Integer idEstudiante) {
        Estudiante est = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no existe"));

        DocumentoTitulacion doc = new DocumentoTitulacion();
        doc.setEstudiante(est);
        doc.setEstado(EstadoDocumento.BORRADOR);
        doc.setTitulo("");
        return docRepo.save(doc);
    }

    private void aplicarCambios(DocumentoTitulacion doc, DocumentoUpdateRequest req) {
        doc.setTitulo(req.getTitulo());
        doc.setCiudad(req.getCiudad());
        doc.setAnio(req.getAnio());
        doc.setResumen(req.getResumen());
        doc.setAbstractText(req.getAbstractText());
        doc.setIntroduccion(req.getIntroduccion());
        doc.setPlanteamientoProblema(req.getPlanteamientoProblema());
        doc.setObjetivoGeneral(req.getObjetivoGeneral());
        doc.setObjetivosEspecificos(req.getObjetivosEspecificos());
        doc.setJustificacion(req.getJustificacion());
        doc.setMarcoTeorico(req.getMarcoTeorico());
        doc.setMetodologia(req.getMetodologia());
        doc.setResultados(req.getResultados());
        doc.setDiscusion(req.getDiscusion());
        doc.setConclusiones(req.getConclusiones());
        doc.setRecomendaciones(req.getRecomendaciones());
        doc.setBibliografia(req.getBibliografia());
        doc.setAnexos(req.getAnexos());
    }

    private DocumentoTitulacionDto toDto(DocumentoTitulacion d) {
        DocumentoTitulacionDto dto = new DocumentoTitulacionDto();
        dto.setId(d.getId());
        dto.setIdEstudiante(d.getEstudiante() != null ? d.getEstudiante().getIdEstudiante() : null);
        // ✅ NUEVO: Nombre completo del estudiante
        if (d.getEstudiante() != null && d.getEstudiante().getUsuario() != null) {
            String nombres = firstNonBlank(d.getEstudiante().getUsuario().getNombres(), "");
            String apellidos = firstNonBlank(d.getEstudiante().getUsuario().getApellidos(), "");
            dto.setNombreEstudiante((nombres + " " + apellidos).trim());
        }

        // ✅ NUEVO: Carrera del estudiante
        if (d.getEstudiante() != null && d.getEstudiante().getCarrera() != null) {
            dto.setCarreraEstudiante(d.getEstudiante().getCarrera().getNombre());
        }

        dto.setIdDirector(d.getDirector() != null ? d.getDirector().getIdDocente() : null);
        dto.setEstado(d.getEstado());
        dto.setTitulo(d.getTitulo());
        dto.setCiudad(d.getCiudad());
        dto.setAnio(d.getAnio());
        dto.setResumen(d.getResumen());
        dto.setAbstractText(d.getAbstractText());
        dto.setIntroduccion(d.getIntroduccion());

        PropuestaTitulacion propuesta = d.getProyecto() != null ? d.getProyecto().getPropuesta() : null;

        dto.setPlanteamientoProblema(firstNonBlank(
                d.getPlanteamientoProblema(),
                propuesta != null ? propuesta.getPlanteamientoProblema() : null
        ));
        dto.setObjetivoGeneral(firstNonBlank(
                d.getObjetivoGeneral(),
                propuesta != null ? propuesta.getObjetivosGenerales() : null
        ));
        dto.setObjetivosEspecificos(firstNonBlank(
                d.getObjetivosEspecificos(),
                propuesta != null ? propuesta.getObjetivosEspecificos() : null
        ));
        dto.setJustificacion(d.getJustificacion());
        dto.setMarcoTeorico(firstNonBlank(
                d.getMarcoTeorico(),
                propuesta != null ? propuesta.getMarcoTeorico() : null
        ));
        dto.setMetodologia(firstNonBlank(
                d.getMetodologia(),
                propuesta != null ? propuesta.getMetodologia() : null
        ));
        dto.setResultados(firstNonBlank(
                d.getResultados(),
                propuesta != null ? propuesta.getResultadosEsperados() : null
        ));
        dto.setDiscusion(d.getDiscusion());
        dto.setConclusiones(d.getConclusiones());
        dto.setRecomendaciones(d.getRecomendaciones());
        dto.setBibliografia(firstNonBlank(
                d.getBibliografia(),
                propuesta != null ? propuesta.getBibliografia() : null
        ));
        dto.setAnexos(d.getAnexos());

        if (d.getProyecto() != null && d.getProyecto().getIdProyecto() != null) {
            Integer idProyecto = d.getProyecto().getIdProyecto();
            List<TribunalProyecto> miembros = tribunalRepo.findByProyecto_IdProyecto(idProyecto);
            if (!miembros.isEmpty()) {
                String tribunal = miembros.stream()
                        .map(this::formatMiembroTribunal)
                        .toList()
                        .stream()
                        .filter(v -> v != null && !v.isBlank())
                        .reduce((a, b) -> a + " · " + b)
                        .orElse(null);
                dto.setTribunal(tribunal);
            }

            List<Sustentacion> sustentaciones = sustentacionRepo
                    .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto);
            if (!sustentaciones.isEmpty()) {
                Sustentacion ultima = sustentaciones.get(0);
                dto.setFechaSustentacion(ultima.getFecha());
                dto.setHoraSustentacion(ultima.getHora());
                dto.setLugarSustentacion(ultima.getLugar());
            }
        }
        return dto;
    }

    private String formatMiembroTribunal(TribunalProyecto miembro) {
        if (miembro == null || miembro.getDocente() == null || miembro.getDocente().getUsuario() == null) {
            return null;
        }
        String nombres   = firstNonBlank(miembro.getDocente().getUsuario().getNombres(), "");
        String apellidos = firstNonBlank(miembro.getDocente().getUsuario().getApellidos(), "");
        String nombreCompleto = (nombres + " " + apellidos).trim();
        String cargo = miembro.getCargo() != null ? miembro.getCargo().trim() : "";

        if (cargo.isEmpty())         return nombreCompleto.isEmpty() ? null : nombreCompleto;
        if (nombreCompleto.isEmpty()) return cargo;
        return cargo + ": " + nombreCompleto;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    private ObservacionDto toObsDto(ObservacionDocumento o) {
        ObservacionDto dto = new ObservacionDto();
        dto.setId(o.getId());
        dto.setIdDocumento(o.getDocumento() != null ? o.getDocumento().getId() : null);
        dto.setSeccion(o.getSeccion());
        dto.setComentario(o.getComentario());
        dto.setEstado(o.getEstado());
        dto.setCreadoEn(o.getCreadoEn());

        if (o.getAutor() != null && o.getAutor().getUsuario() != null) {
            dto.setIdAutor(o.getAutor().getIdDocente());
            dto.setAutorNombre(o.getAutor().getUsuario().getNombres() + " " +
                    o.getAutor().getUsuario().getApellidos());
        }
        return dto;
    }

    public DocumentoTitulacionDto obtenerPorIdDocumento(Integer idDocumento) {
        DocumentoTitulacion doc = docRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));
        return toDto(doc);
    }
}