package com.erwin.backend.service;

import com.erwin.backend.dtos.TitulacionWorkflowDtos;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TitulacionWorkflowService {

    private static final int MIN_MIEMBROS_TRIBUNAL = 3;

    private final DocumentoTitulacionRepository documentoRepo;
    private final ObservacionDocumentoRepository observacionRepo;
    private final AvalDirectorRepository avalRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final DocenteRepository docenteRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final SustentacionRepository sustentacionRepo;
    private final SustentacionReprogramacionRepository reprogramacionRepo;
    private final CierreTitulacionRepository cierreRepo;

    public TitulacionWorkflowService(DocumentoTitulacionRepository documentoRepo,
                                     ObservacionDocumentoRepository observacionRepo,
                                     AvalDirectorRepository avalRepo,
                                     ProyectoTitulacionRepository proyectoRepo,
                                     DocenteRepository docenteRepo,
                                     TribunalProyectoRepository tribunalRepo,
                                     SustentacionRepository sustentacionRepo,
                                     SustentacionReprogramacionRepository reprogramacionRepo,
                                     CierreTitulacionRepository cierreRepo) {
        this.documentoRepo = documentoRepo;
        this.observacionRepo = observacionRepo;
        this.avalRepo = avalRepo;
        this.proyectoRepo = proyectoRepo;
        this.docenteRepo = docenteRepo;
        this.tribunalRepo = tribunalRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.reprogramacionRepo = reprogramacionRepo;
        this.cierreRepo = cierreRepo;
    }

    @Transactional
    public TitulacionWorkflowDtos.WorkflowResumenDto prepararParaTribunal(Integer idDocumento,
                                                                          TitulacionWorkflowDtos.PrepararTribunalRequest req) {
        DocumentoTitulacion doc = getDocumento(idDocumento);
        validarEstado(doc, EstadoDocumento.APROBADO_POR_DIRECTOR);
        validarSinObservacionesPendientes(doc.getId());
        validarDocumentoFinalListo(doc);

        if (isBlank(req.getAvalUrlPdf())) {
            throw new RuntimeException("Debe cargar el aval/informe del director");
        }

        BigDecimal umbral = req.getUmbralAntiplagio() != null ? req.getUmbralAntiplagio() : new BigDecimal("20.00");
        if (req.getPorcentajeAntiplagio() == null) {
            throw new RuntimeException("Debe registrar el porcentaje de antiplagio");
        }
        if (req.getPorcentajeAntiplagio().compareTo(umbral) > 0) {
            throw new RuntimeException("Antiplagio fuera del umbral permitido");
        }

        AvalDirector aval = avalRepo.findByDocumento_Id(doc.getId()).orElse(new AvalDirector());
        aval.setDocumento(doc);
        aval.setUrlPdf(req.getAvalUrlPdf());
        aval.setComentario(req.getAvalComentario());
        avalRepo.save(aval);

        ProyectoTitulacion proyecto = doc.getProyecto();
        if (proyecto != null) {
            proyecto.setPorcentajeAntiplagio(req.getPorcentajeAntiplagio());
            proyecto.setUrlInformeAntiplagio(req.getUrlInformeAntiplagio());
            proyectoRepo.save(proyecto);
        }

        doc.setEstado(EstadoDocumento.LISTO_PARA_TRIBUNAL);
        documentoRepo.save(doc);

        TitulacionWorkflowDtos.WorkflowResumenDto dto = new TitulacionWorkflowDtos.WorkflowResumenDto();
        dto.setIdDocumento(doc.getId());
        dto.setEstado(doc.getEstado().name());
        dto.setMensaje("Expediente listo para asignar tribunal");
        return dto;
    }

    @Transactional
    public TitulacionWorkflowDtos.WorkflowResumenDto asignarTribunal(Integer idDocumento,
                                                                     TitulacionWorkflowDtos.AsignarTribunalRequest req) {
        DocumentoTitulacion doc = getDocumento(idDocumento);
        validarEstado(doc, EstadoDocumento.LISTO_PARA_TRIBUNAL);

        if (req.getMiembros() == null || req.getMiembros().size() < MIN_MIEMBROS_TRIBUNAL) {
            throw new RuntimeException("Debe asignar al menos " + MIN_MIEMBROS_TRIBUNAL + " miembros");
        }

        Set<Integer> ids = new HashSet<>();
        tribunalRepo.deleteByProyecto_IdProyecto(doc.getProyecto().getIdProyecto());

        for (TitulacionWorkflowDtos.MiembroTribunalRequest miembro : req.getMiembros()) {
            if (miembro.getIdDocente() == null || isBlank(miembro.getCargo())) {
                throw new RuntimeException("Cada miembro debe tener docente y cargo");
            }
            if (!ids.add(miembro.getIdDocente())) {
                throw new RuntimeException("No se puede repetir un docente en el tribunal");
            }

            Docente docente = docenteRepo.findById(miembro.getIdDocente())
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + miembro.getIdDocente()));

            TribunalProyecto tp = new TribunalProyecto();
            tp.setProyecto(doc.getProyecto());
            tp.setDocente(docente);
            tp.setCargo(miembro.getCargo().toUpperCase());
            tribunalRepo.save(tp);
        }

        doc.setEstado(EstadoDocumento.TRIBUNAL_ASIGNADO);
        documentoRepo.save(doc);

        TitulacionWorkflowDtos.WorkflowResumenDto dto = new TitulacionWorkflowDtos.WorkflowResumenDto();
        dto.setIdDocumento(doc.getId());
        dto.setEstado(doc.getEstado().name());
        dto.setMensaje("Tribunal asignado correctamente");
        return dto;
    }

    @Transactional
    public TitulacionWorkflowDtos.WorkflowResumenDto agendarSustentacion(Integer idDocumento,
                                                                         TitulacionWorkflowDtos.AgendarSustentacionRequest req) {
        DocumentoTitulacion doc = getDocumento(idDocumento);
        validarEstado(doc, EstadoDocumento.TRIBUNAL_ASIGNADO);

        if (tribunalRepo.countByProyecto_IdProyecto(doc.getProyecto().getIdProyecto()) < MIN_MIEMBROS_TRIBUNAL) {
            throw new RuntimeException("El tribunal no cumple el mínimo de miembros");
        }
        if (req.getFecha() == null || req.getHora() == null || isBlank(req.getLugar())) {
            throw new RuntimeException("Fecha, hora y lugar son obligatorios");
        }

        List<Sustentacion> existentes = sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(doc.getProyecto().getIdProyecto());
        Sustentacion sustentacion;
        if (existentes.isEmpty()) {
            sustentacion = new Sustentacion();
            sustentacion.setProyecto(doc.getProyecto());
            sustentacion.setTipo("DEFENSA_FINAL");
        } else {
            sustentacion = existentes.get(0);
            SustentacionReprogramacion rep = new SustentacionReprogramacion();
            rep.setSustentacion(sustentacion);
            rep.setFechaAnterior(Objects.toString(sustentacion.getFecha(), ""));
            rep.setHoraAnterior(Objects.toString(sustentacion.getHora(), ""));
            rep.setLugarAnterior(sustentacion.getLugar());
            rep.setMotivo(req.getMotivoReprogramacion());
            reprogramacionRepo.save(rep);
        }

        sustentacion.setFecha(req.getFecha());
        sustentacion.setHora(req.getHora());
        sustentacion.setLugar(req.getLugar());
        sustentacion.setObservaciones(req.getObservaciones());
        sustentacionRepo.save(sustentacion);

        doc.setEstado(EstadoDocumento.SUSTENTACION_AGENDADA);
        documentoRepo.save(doc);

        TitulacionWorkflowDtos.WorkflowResumenDto dto = new TitulacionWorkflowDtos.WorkflowResumenDto();
        dto.setIdDocumento(doc.getId());
        dto.setEstado(doc.getEstado().name());
        dto.setMensaje("Sustentación agendada");
        return dto;
    }

    @Transactional
    public TitulacionWorkflowDtos.WorkflowResumenDto registrarResultado(Integer idDocumento,
                                                                        TitulacionWorkflowDtos.RegistrarResultadoRequest req) {
        DocumentoTitulacion doc = getDocumento(idDocumento);
        validarEstado(doc, EstadoDocumento.SUSTENTACION_AGENDADA);

        Sustentacion sustentacion = getUltimaSustentacion(doc.getProyecto().getIdProyecto());

        if (isBlank(req.getActaUrl())) {
            throw new RuntimeException("Debe subir el acta consolidada");
        }
        if (req.getNotaDocente() == null || req.getNotasTribunal() == null || req.getNotasTribunal().isEmpty()) {
            throw new RuntimeException("Debe registrar nota docente y notas de tribunal");
        }

        BigDecimal suma = BigDecimal.ZERO;
        for (TitulacionWorkflowDtos.NotaTribunalRequest nota : req.getNotasTribunal()) {
            if (nota.getNota() == null) {
                throw new RuntimeException("Todas las notas del tribunal son obligatorias");
            }
            suma = suma.add(nota.getNota());
        }
        BigDecimal promedioTribunal = suma.divide(BigDecimal.valueOf(req.getNotasTribunal().size()), 2, RoundingMode.HALF_UP);
        BigDecimal notaFinal = req.getNotaDocente().multiply(new BigDecimal("0.50"))
                .add(promedioTribunal.multiply(new BigDecimal("0.50")))
                .setScale(2, RoundingMode.HALF_UP);

        CierreTitulacion cierre = cierreRepo.findByDocumento_Id(doc.getId()).orElse(new CierreTitulacion());
        cierre.setDocumento(doc);
        cierre.setSustentacion(sustentacion);
        cierre.setNotaDocente(req.getNotaDocente());
        cierre.setNotaTribunal(promedioTribunal);
        cierre.setNotaFinal(notaFinal);
        cierre.setActaUrl(req.getActaUrl());
        cierre.setActaFirmadaUrl(req.getActaFirmadaUrl());
        cierre.setObservaciones(req.getObservaciones());
        cierre.setResultado(isBlank(req.getResultado()) ? "PENDIENTE" : req.getResultado().toUpperCase());
        cierre.setCerrado(false);
        cierreRepo.save(cierre);

        doc.setEstado(EstadoDocumento.SUSTENTADO);
        documentoRepo.save(doc);

        TitulacionWorkflowDtos.WorkflowResumenDto dto = new TitulacionWorkflowDtos.WorkflowResumenDto();
        dto.setIdDocumento(doc.getId());
        dto.setEstado(doc.getEstado().name());
        dto.setMensaje("Resultado de sustentación registrado");
        dto.setNotaDocente(cierre.getNotaDocente());
        dto.setNotaTribunal(cierre.getNotaTribunal());
        dto.setNotaFinal(cierre.getNotaFinal());
        return dto;
    }

    @Transactional
    public TitulacionWorkflowDtos.WorkflowResumenDto cerrarExpediente(Integer idDocumento,
                                                                      TitulacionWorkflowDtos.CerrarExpedienteRequest req) {
        DocumentoTitulacion doc = getDocumento(idDocumento);
        validarEstado(doc, EstadoDocumento.SUSTENTADO);

        CierreTitulacion cierre = cierreRepo.findByDocumento_Id(doc.getId())
                .orElseThrow(() -> new RuntimeException("Debe registrar acta y notas antes de cerrar"));

        if (isBlank(cierre.getActaUrl()) || cierre.getNotaFinal() == null) {
            throw new RuntimeException("Debe existir acta y notas antes de cerrar");
        }

        String resultadoFinal = !isBlank(req.getResultadoFinal()) ? req.getResultadoFinal().toUpperCase() : cierre.getResultado();
        cierre.setResultado(resultadoFinal);
        cierre.setObservaciones(req.getObservacionesFinales());
        cierre.setCerrado(true);
        cierreRepo.save(cierre);

        if ("APROBADO".equals(resultadoFinal)) {
            doc.setEstado(EstadoDocumento.CERRADO_APROBADO);
        } else {
            doc.setEstado(EstadoDocumento.CERRADO_REPROBADO);
        }
        documentoRepo.save(doc);

        TitulacionWorkflowDtos.WorkflowResumenDto dto = new TitulacionWorkflowDtos.WorkflowResumenDto();
        dto.setIdDocumento(doc.getId());
        dto.setEstado(doc.getEstado().name());
        dto.setMensaje("Expediente cerrado");
        dto.setNotaDocente(cierre.getNotaDocente());
        dto.setNotaTribunal(cierre.getNotaTribunal());
        dto.setNotaFinal(cierre.getNotaFinal());
        return dto;
    }

    private DocumentoTitulacion getDocumento(Integer idDocumento) {
        return documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
    }

    private Sustentacion getUltimaSustentacion(Integer idProyecto) {
        List<Sustentacion> sustentaciones = sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto);
        if (sustentaciones.isEmpty()) {
            throw new RuntimeException("No existe agenda de sustentación");
        }
        return sustentaciones.get(0);
    }

    private void validarEstado(DocumentoTitulacion doc, EstadoDocumento esperado) {
        if (doc.getEstado() != esperado) {
            throw new RuntimeException("Estado inválido. Se esperaba " + esperado + " y se encontró " + doc.getEstado());
        }
    }

    private void validarSinObservacionesPendientes(Integer idDocumento) {
        List<ObservacionDocumento> observaciones = observacionRepo.findByDocumento_IdOrderByCreadoEnDesc(idDocumento);
        boolean existePendiente = observaciones.stream().anyMatch(o -> o.getEstado() == EstadoObservacion.PENDIENTE);
        if (existePendiente) {
            throw new RuntimeException("Todas las observaciones deben estar resueltas");
        }
    }

    private void validarDocumentoFinalListo(DocumentoTitulacion doc) {
        if (isBlank(doc.getTitulo()) || isBlank(doc.getIntroduccion()) || isBlank(doc.getMetodologia())
                || isBlank(doc.getResultados()) || isBlank(doc.getConclusiones()) || isBlank(doc.getBibliografia())) {
            throw new RuntimeException("Documento final incompleto. Faltan secciones obligatorias");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
