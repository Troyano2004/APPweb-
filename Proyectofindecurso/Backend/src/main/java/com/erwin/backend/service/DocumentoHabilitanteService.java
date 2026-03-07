
package com.erwin.backend.service;

import com.erwin.backend.dtos.DocumentoHabilitanteDtos;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Gestión de documentos habilitantes para la sustentación en DT2.
 * Arts. 10, 11, 57 num.2 y 59 del Reglamento UIC-UTEQ (sept-2024).
 */
@Service
public class DocumentoHabilitanteService {

    /** Catálogo de tipos con metadatos (orden, etiqueta, descripción, obligatorio) */
    private static final List<TipoMeta> CATALOGO = List.of(
            new TipoMeta("INFORME_DIRECTOR",       1, "Informe del Director",
                    "Art. 59a: Informe favorable emitido por el/la Director(a) del TIC una vez finalizado el trabajo.", true),
            new TipoMeta("CERTIFICADO_ANTIPLAGIO",  2, "Certificado Anti-plagio (COMPILATIO)",
                    "Art. 57 num.2: Reporte de COMPILATIO con coincidencia máxima del 10%. El Director certifica que el trabajo supera la prueba.", true),
            new TipoMeta("TRABAJO_FINAL_PDF",       3, "Trabajo Final (PDF para Biblioteca)",
                    "Art. 59a: Copia digital del trabajo final empastado que será remitida a la biblioteca.", true),
            new TipoMeta("CERTIFICADO_PENSUM",      4, "Certificado Pensum Completo (SGA)",
                    "Art. 10: Certificado emitido por el SGA que acredita haber aprobado la totalidad del pensum.", true),
            new TipoMeta("CERTIFICADO_DEUDAS",      5, "Certificado Sin Deudas Administrativas (SGA)",
                    "Art. 11: Certificado del SGA de no tener deudas con ninguna instancia administrativa o académica de la UTEQ.", true),
            new TipoMeta("CERTIFICADO_IDIOMA",      6, "Certificado Idioma Extranjero",
                    "Art. 18a: Certificado de haber aprobado el idioma extranjero definido en la oferta académica.", true),
            new TipoMeta("CERTIFICADO_PRACTICAS",   7, "Certificado Prácticas y Servicio Comunitario",
                    "Art. 18b: Certificado de horas de Prácticas Laborales y Servicio Comunitario aprobadas.", true)
    );

    private final DocumentoHabilitanteRepository   habilitanteRepo;
    private final ProyectoTitulacionRepository     proyectoRepo;
    private final EstudianteRepository             estudianteRepo;
    private final DocenteRepository                docenteRepo;

    public DocumentoHabilitanteService(
            DocumentoHabilitanteRepository habilitanteRepo,
            ProyectoTitulacionRepository proyectoRepo,
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo) {
        this.habilitanteRepo = habilitanteRepo;
        this.proyectoRepo    = proyectoRepo;
        this.estudianteRepo  = estudianteRepo;
        this.docenteRepo     = docenteRepo;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ESTUDIANTE: obtener resumen de sus habilitantes
    // ────────────────────────────────────────────────────────────────────────────

    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorEstudiante(Integer idEstudiante) {
        List<DocumentoHabilitante> existentes =
                habilitanteRepo.findByEstudiante_IdEstudiante(idEstudiante);

        // Buscar el proyecto activo del estudiante
        ProyectoTitulacion proyecto = proyectoRepo
                .findAll()
                .stream()
                .filter(p -> p.getEleccion() != null
                        && p.getEleccion().getEstudiante() != null
                        && p.getEleccion().getEstudiante().getIdEstudiante().equals(idEstudiante))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró proyecto activo para el estudiante"));

        return construirResumen(proyecto, existentes);
    }

    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorProyecto(Integer idProyecto) {
        ProyectoTitulacion proyecto = proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + idProyecto));
        List<DocumentoHabilitante> existentes =
                habilitanteRepo.findByProyecto_IdProyecto(idProyecto);
        return construirResumen(proyecto, existentes);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ESTUDIANTE: subir / actualizar un documento
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumento(
            Integer idEstudiante,
            DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {

        validarTipo(req.getTipoDocumento());

        ProyectoTitulacion proyecto = proyectoRepo.findById(req.getIdProyecto())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        // Upsert: si ya existe ese tipo en ese proyecto, actualizamos
        DocumentoHabilitante doc = habilitanteRepo
                .findByProyecto_IdProyectoAndTipoDocumento(req.getIdProyecto(), req.getTipoDocumento())
                .orElse(new DocumentoHabilitante());

        doc.setProyecto(proyecto);
        doc.setEstudiante(estudiante);
        doc.setTipoDocumento(req.getTipoDocumento());
        doc.setNombreArchivo(req.getNombreArchivo());
        doc.setUrlArchivo(req.getUrlArchivo());
        doc.setFormato("PDF");
        doc.setEstado("ENVIADO");
        doc.setComentarioValidacion(null);
        doc.setFechaValidacion(null);
        doc.setValidadoPor(null);

        // Lógica antiplagio (solo para CERTIFICADO_ANTIPLAGIO)
        if ("CERTIFICADO_ANTIPLAGIO".equals(req.getTipoDocumento())) {
            if (req.getPorcentajeCoincidencia() == null) {
                throw new RuntimeException("Debe ingresar el porcentaje de coincidencia del reporte COMPILATIO");
            }
            BigDecimal umbral = req.getUmbralPermitido() != null
                    ? req.getUmbralPermitido()
                    : new BigDecimal("10.00");

            doc.setPorcentajeCoincidencia(req.getPorcentajeCoincidencia());
            doc.setUmbralPermitido(umbral);

            // Determinamos resultado automáticamente según el reglamento (Art. 57 num.2)
            boolean aprobado = req.getPorcentajeCoincidencia().compareTo(umbral) <= 0;
            doc.setResultadoAntiplagio(aprobado ? "APROBADO" : "RECHAZADO");

            // Si supera el umbral, el estado queda RECHAZADO directamente
            if (!aprobado) {
                doc.setEstado("RECHAZADO");
                doc.setComentarioValidacion(
                        "Porcentaje de coincidencia (" + req.getPorcentajeCoincidencia()
                                + "%) supera el umbral permitido del " + umbral + "% (Art. 57 num.2)."
                );
            }
        }

        habilitanteRepo.save(doc);
        return toDto(doc);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // DIRECTOR / COORDINADOR: validar un documento
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto validarDocumento(
            Integer idDocente,
            Integer idHabilitante,
            DocumentoHabilitanteDtos.ValidarHabilitanteRequest req) {

        DocumentoHabilitante doc = habilitanteRepo.findById(idHabilitante)
                .orElseThrow(() -> new RuntimeException("Documento habilitante no encontrado: " + idHabilitante));

        if (!"ENVIADO".equals(doc.getEstado())) {
            throw new RuntimeException("Solo se pueden validar documentos en estado ENVIADO");
        }

        Docente validador = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        String decision = req.getDecision();
        if (!"APROBADO".equals(decision) && !"RECHAZADO".equals(decision)) {
            throw new RuntimeException("La decisión debe ser APROBADO o RECHAZADO");
        }

        doc.setEstado(decision);
        doc.setValidadoPor(validador);
        doc.setComentarioValidacion(req.getComentario());
        doc.setFechaValidacion(LocalDateTime.now());

        habilitanteRepo.save(doc);
        return toDto(doc);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // DIRECTOR: listar pendientes por validar
    // ────────────────────────────────────────────────────────────────────────────

    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesPorDirector(Integer idDocente) {
        return habilitanteRepo
                .findByValidadoPor_IdDocenteAndEstado(idDocente, "ENVIADO")
                .stream().map(this::toDto).toList();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ────────────────────────────────────────────────────────────────────────────

    private DocumentoHabilitanteDtos.ResumenHabilitacionDto construirResumen(
            ProyectoTitulacion proyecto,
            List<DocumentoHabilitante> existentes) {

        Map<String, DocumentoHabilitante> porTipo = new HashMap<>();
        existentes.forEach(d -> porTipo.put(d.getTipoDocumento(), d));

        List<DocumentoHabilitanteDtos.HabilitanteDto> docs = new ArrayList<>();

        for (TipoMeta meta : CATALOGO) {
            DocumentoHabilitante entidad = porTipo.get(meta.tipo);
            if (entidad != null) {
                docs.add(toDto(entidad));
            } else {
                // Crear DTO vacío (pendiente de subir)
                DocumentoHabilitanteDtos.HabilitanteDto placeholder =
                        new DocumentoHabilitanteDtos.HabilitanteDto();
                placeholder.setIdProyecto(proyecto.getIdProyecto());
                placeholder.setTipoDocumento(meta.tipo);
                placeholder.setEtiquetaTipo(meta.etiqueta);
                placeholder.setDescripcionTipo(meta.descripcion);
                placeholder.setObligatorio(meta.obligatorio);
                placeholder.setEstado("PENDIENTE");
                docs.add(placeholder);
            }
        }

        long aprobados  = docs.stream().filter(d -> "APROBADO".equals(d.getEstado())).count();
        long pendientes = docs.stream().filter(d -> "PENDIENTE".equals(d.getEstado())).count();
        long rechazados = docs.stream().filter(d -> "RECHAZADO".equals(d.getEstado())).count();

        DocumentoHabilitanteDtos.ResumenHabilitacionDto resumen =
                new DocumentoHabilitanteDtos.ResumenHabilitacionDto();
        resumen.setIdProyecto(proyecto.getIdProyecto());
        resumen.setTituloProyecto(proyecto.getTitulo());
        resumen.setDocumentos(docs);
        resumen.setTotalDocumentos(docs.size());
        resumen.setAprobados((int) aprobados);
        resumen.setPendientes((int) pendientes);
        resumen.setRechazados((int) rechazados);
        resumen.setHabilitadoParaSustentacion(aprobados == CATALOGO.size());
        return resumen;
    }

    private DocumentoHabilitanteDtos.HabilitanteDto toDto(DocumentoHabilitante e) {
        TipoMeta meta = CATALOGO.stream()
                .filter(m -> m.tipo.equals(e.getTipoDocumento()))
                .findFirst()
                .orElse(new TipoMeta(e.getTipoDocumento(), 99, e.getTipoDocumento(), "", false));

        DocumentoHabilitanteDtos.HabilitanteDto dto = new DocumentoHabilitanteDtos.HabilitanteDto();
        dto.setId(e.getId());
        dto.setIdProyecto(e.getProyecto() != null ? e.getProyecto().getIdProyecto() : null);
        dto.setIdEstudiante(e.getEstudiante() != null ? e.getEstudiante().getIdEstudiante() : null);
        if (e.getEstudiante() != null && e.getEstudiante().getUsuario() != null) {
            dto.setNombreEstudiante(
                    e.getEstudiante().getUsuario().getNombres() + " " +
                            e.getEstudiante().getUsuario().getApellidos()
            );
        }
        dto.setTipoDocumento(e.getTipoDocumento());
        dto.setEtiquetaTipo(meta.etiqueta);
        dto.setDescripcionTipo(meta.descripcion);
        dto.setObligatorio(meta.obligatorio);
        dto.setNombreArchivo(e.getNombreArchivo());
        dto.setUrlArchivo(e.getUrlArchivo());
        dto.setFormato(e.getFormato());
        dto.setPorcentajeCoincidencia(e.getPorcentajeCoincidencia());
        dto.setUmbralPermitido(e.getUmbralPermitido());
        dto.setResultadoAntiplagio(e.getResultadoAntiplagio());
        dto.setEstado(e.getEstado());
        dto.setComentarioValidacion(e.getComentarioValidacion());
        if (e.getValidadoPor() != null && e.getValidadoPor().getUsuario() != null) {
            dto.setValidadoPorNombre(
                    e.getValidadoPor().getUsuario().getNombres() + " " +
                            e.getValidadoPor().getUsuario().getApellidos()
            );
        }
        dto.setFechaValidacion(e.getFechaValidacion());
        dto.setFechaSubida(e.getFechaSubida());
        dto.setActualizadoEn(e.getActualizadoEn());
        return dto;
    }

    private void validarTipo(String tipo) {
        boolean valido = CATALOGO.stream().anyMatch(m -> m.tipo.equals(tipo));
        if (!valido) {
            throw new RuntimeException("Tipo de documento no reconocido: " + tipo);
        }
    }

    /** Registro de metadatos del catálogo de tipos */
    private record TipoMeta(String tipo, int orden, String etiqueta, String descripcion, boolean obligatorio) {}
}