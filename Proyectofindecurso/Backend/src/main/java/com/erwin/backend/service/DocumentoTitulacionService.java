package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.erwin.backend.repository.ObservacionDocumentoRepository;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.EstudianteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoTitulacionService {

    private final DocumentoTitulacionRepository docRepo;
    private final ObservacionDocumentoRepository obsRepo;
    private final EstudianteRepository estudianteRepo;
    private final DocenteRepository docenteRepo;

    public DocumentoTitulacionService(DocumentoTitulacionRepository docRepo,
                                      ObservacionDocumentoRepository obsRepo,
                                      EstudianteRepository estudianteRepo,
                                      DocenteRepository docenteRepo) {
        this.docRepo = docRepo;
        this.obsRepo = obsRepo;
        this.estudianteRepo = estudianteRepo;
        this.docenteRepo = docenteRepo;
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

        // Bloqueo por estado
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
        return docRepo.findByDirector_IdDocenteAndEstado(idDocente, EstadoDocumento.EN_REVISION)
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

        // (Opcional) validar que existan observaciones antes de devolver
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
        doc.setTitulo("");// para que no falle nullable=false si lo pusiste así
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
        dto.setIdDirector(d.getDirector() != null ? d.getDirector().getIdDocente() : null);
        dto.setEstado(d.getEstado());

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
            dto.setAutorNombre(o.getAutor().getUsuario().getNombres() + " " + o.getAutor().getUsuario().getApellidos());
        }
        return dto;
    }
    public DocumentoTitulacionDto obtenerPorIdDocumento(Integer idDocumento) {
        DocumentoTitulacion doc = docRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no existe"));
        return toDto(doc);
    }

}
