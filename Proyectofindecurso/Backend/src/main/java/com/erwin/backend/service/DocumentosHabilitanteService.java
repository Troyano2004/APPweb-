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
 * Arts. 10, 11, 18, 20, 57 y 59 del Reglamento UIC-UTEQ (sept-2024).
 *
 * Catálogo dinámico según modalidad del estudiante:
 * - Examen Complexivo:              4 comunes + 1 exclusivo = 5 documentos
 * - Trabajo de Integración Curricular: 4 comunes + 3 exclusivos = 7 documentos
 */
@Service
public class DocumentosHabilitanteService {

    // ── Documentos comunes a AMBAS modalidades (Arts. 10, 11, 18) ────────────
    private static final List<TipoMeta> DOCS_COMUNES = List.of(
            new TipoMeta("CERTIFICADO_PENSUM",    1, "Certificado Pensum Completo (SGA)",
                    "Art. 10: Certificado del SGA que acredita haber aprobado la totalidad del pensum académico.", true),
            new TipoMeta("CERTIFICADO_DEUDAS",    2, "Certificado Sin Deudas Administrativas (SGA)",
                    "Art. 11: Certificado del SGA de no tener deudas con ninguna instancia administrativa o académica de la UTEQ.", true),
            new TipoMeta("CERTIFICADO_IDIOMA",    3, "Certificado Idioma Extranjero",
                    "Art. 18a: Certificado de haber aprobado el idioma extranjero definido en la oferta académica.", true),
            new TipoMeta("CERTIFICADO_PRACTICAS", 4, "Certificado Prácticas y Servicio Comunitario",
                    "Art. 18b: Certificado de horas de Prácticas Laborales y Servicio Comunitario aprobadas.", true)
    );

    // ── Exclusivos para TRABAJO DE INTEGRACIÓN CURRICULAR (Arts. 57 y 59) ────
    private static final List<TipoMeta> DOCS_TIC = List.of(
            new TipoMeta("CERTIFICADO_ANTIPLAGIO", 5, "Certificado Anti-plagio (COMPILATIO)",
                    "Art. 57 num.2: Reporte COMPILATIO con coincidencia máxima del 10%. El Director certifica que el trabajo supera la prueba.", true),
            new TipoMeta("INFORME_DIRECTOR",       6, "Informe del Director",
                    "Art. 59a: Informe favorable emitido por el/la Director(a) del TIC una vez finalizado el trabajo.", true),
            new TipoMeta("TRABAJO_FINAL_PDF",      7, "Trabajo Final (PDF para Biblioteca)",
                    "Art. 59a: Copia digital del trabajo final empastado que será remitida a la biblioteca.", true)
    );

    // ── Exclusivos para EXAMEN COMPLEXIVO (Art. 20) ───────────────────────────
    private static final List<TipoMeta> DOCS_COMPLEXIVO = List.of(
            new TipoMeta("INFORME_PRACTICO_COMPLEXIVO", 5, "Informe del Trabajo Práctico (Complexivo)",
                    "Art. 20: Trabajo Investigativo-Práctico o Artículo de Revisión aprobado por el Consejo Directivo de Facultad.", true)
    );

    private final DocumentosHabilitanteRepository habilitanteRepo;
    private final ProyectoTitulacionRepository    proyectoRepo;
    private final EstudianteRepository            estudianteRepo;
    private final DocenteRepository               docenteRepo;
    private final EleccionTitulacionRepository    eleccionRepo;

    public DocumentosHabilitanteService(
            DocumentosHabilitanteRepository habilitanteRepo,
            ProyectoTitulacionRepository proyectoRepo,
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo,
            EleccionTitulacionRepository eleccionRepo) {
        this.habilitanteRepo = habilitanteRepo;
        this.proyectoRepo    = proyectoRepo;
        this.estudianteRepo  = estudianteRepo;
        this.docenteRepo     = docenteRepo;
        this.eleccionRepo    = eleccionRepo;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Catálogo dinámico según modalidad
    // ────────────────────────────────────────────────────────────────────────

    private List<TipoMeta> catalogoPorEstudiante(Integer idEstudiante) {
        EleccionTitulacion eleccion = eleccionRepo
                .findByEstudiante_IdEstudianteAndEstado(idEstudiante, "ACTIVA")
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró elección ACTIVA de titulación para el estudiante " + idEstudiante));
        return catalogoPorModalidad(eleccion.getModalidad().getNombre());
    }

    private List<TipoMeta> catalogoPorModalidad(String nombreModalidad) {
        List<TipoMeta> catalogo = new ArrayList<>(DOCS_COMUNES);
        if (nombreModalidad != null && nombreModalidad.toUpperCase().contains("COMPLEXIVO")) {
            catalogo.addAll(DOCS_COMPLEXIVO);
        } else {
            catalogo.addAll(DOCS_TIC);
        }
        return Collections.unmodifiableList(catalogo);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ESTUDIANTE: resumen de habilitantes
    // ────────────────────────────────────────────────────────────────────────

    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorEstudiante(Integer idEstudiante) {
        List<DocumentosHabilitantes> existentes =
                habilitanteRepo.findByEstudiante_IdEstudiante(idEstudiante);

        ProyectoTitulacion proyecto = proyectoRepo.findAll().stream()
                .filter(p -> p.getEleccion() != null
                        && p.getEleccion().getEstudiante() != null
                        && p.getEleccion().getEstudiante().getIdEstudiante().equals(idEstudiante))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró proyecto activo para el estudiante " + idEstudiante));

        List<TipoMeta> catalogo = catalogoPorEstudiante(idEstudiante);
        return construirResumen(proyecto, existentes, catalogo);
    }

    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorProyecto(Integer idProyecto) {
        ProyectoTitulacion proyecto = proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + idProyecto));

        List<DocumentosHabilitantes> existentes =
                habilitanteRepo.findByProyecto_IdProyecto(idProyecto);

        Integer idEstudiante = proyecto.getEleccion().getEstudiante().getIdEstudiante();
        List<TipoMeta> catalogo = catalogoPorEstudiante(idEstudiante);
        return construirResumen(proyecto, existentes, catalogo);
    }

    // ────────────────────────────────────────────────────────────────────────
    // ESTUDIANTE: subir / actualizar un documento
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumento(
            Integer idEstudiante,
            DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {

        List<TipoMeta> catalogo = catalogoPorEstudiante(idEstudiante);
        validarTipo(req.getTipoDocumento(), catalogo);

        ProyectoTitulacion proyecto = proyectoRepo.findById(req.getIdProyecto())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        DocumentosHabilitantes doc = habilitanteRepo
                .findByProyecto_IdProyectoAndTipoDocumento(req.getIdProyecto(), req.getTipoDocumento())
                .orElse(new DocumentosHabilitantes());

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

        if ("CERTIFICADO_ANTIPLAGIO".equals(req.getTipoDocumento())) {
            if (req.getPorcentajeCoincidencia() == null) {
                throw new RuntimeException(
                        "Debe ingresar el porcentaje de coincidencia del reporte COMPILATIO");
            }
            BigDecimal umbral = req.getUmbralPermitido() != null
                    ? req.getUmbralPermitido()
                    : new BigDecimal("10.00");

            doc.setPorcentajeCoincidencia(req.getPorcentajeCoincidencia());
            doc.setUmbralPermitido(umbral);

            boolean aprobado = req.getPorcentajeCoincidencia().compareTo(umbral) <= 0;
            doc.setResultadoAntiplagio(aprobado ? "APROBADO" : "RECHAZADO");

            if (!aprobado) {
                doc.setEstado("RECHAZADO");
                doc.setComentarioValidacion(
                        "Porcentaje de coincidencia (" + req.getPorcentajeCoincidencia()
                                + "%) supera el umbral permitido del " + umbral + "% (Art. 57 num.2).");
            }
        }

        habilitanteRepo.save(doc);
        return toDto(doc, catalogo);
    }

    // ────────────────────────────────────────────────────────────────────────
    // DIRECTOR / COORDINADOR: validar un documento
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto validarDocumento(
            Integer idDocente,
            Integer idHabilitante,
            DocumentoHabilitanteDtos.ValidarHabilitanteRequest req) {

        DocumentosHabilitantes doc = habilitanteRepo.findById(idHabilitante)
                .orElseThrow(() -> new RuntimeException(
                        "Documento habilitante no encontrado: " + idHabilitante));

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

        Integer idEstudiante = doc.getEstudiante().getIdEstudiante();
        List<TipoMeta> catalogo = catalogoPorEstudiante(idEstudiante);
        return toDto(doc, catalogo);
    }

    // ────────────────────────────────────────────────────────────────────────
    // DIRECTOR: pendientes por validar
    // ────────────────────────────────────────────────────────────────────────

    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesPorDirector(Integer idDocente) {
        return habilitanteRepo.findPendientesPorDirector(idDocente)
                .stream()
                .map(doc -> {
                    Integer idEstudiante = doc.getEstudiante().getIdEstudiante();
                    List<TipoMeta> catalogo = catalogoPorEstudiante(idEstudiante);
                    return toDto(doc, catalogo);
                })
                .toList();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ────────────────────────────────────────────────────────────────────────

    private DocumentoHabilitanteDtos.ResumenHabilitacionDto construirResumen(
            ProyectoTitulacion proyecto,
            List<DocumentosHabilitantes> existentes,
            List<TipoMeta> catalogo) {

        Map<String, DocumentosHabilitantes> porTipo = new HashMap<>();
        existentes.forEach(d -> porTipo.put(d.getTipoDocumento(), d));

        List<DocumentoHabilitanteDtos.HabilitanteDto> docs = new ArrayList<>();

        for (TipoMeta meta : catalogo) {
            DocumentosHabilitantes entidad = porTipo.get(meta.tipo);
            if (entidad != null) {
                docs.add(toDto(entidad, catalogo));
            } else {
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
        resumen.setHabilitadoParaSustentacion(aprobados == catalogo.size());
        return resumen;
    }

    private DocumentoHabilitanteDtos.HabilitanteDto toDto(
            DocumentosHabilitantes e, List<TipoMeta> catalogo) {

        TipoMeta meta = catalogo.stream()
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
                            e.getEstudiante().getUsuario().getApellidos());
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
                            e.getValidadoPor().getUsuario().getApellidos());
        }
        dto.setFechaValidacion(e.getFechaValidacion());
        dto.setFechaSubida(e.getFechaSubida());
        dto.setActualizadoEn(e.getActualizadoEn());
        return dto;
    }

    private void validarTipo(String tipo, List<TipoMeta> catalogo) {
        boolean valido = catalogo.stream().anyMatch(m -> m.tipo.equals(tipo));
        if (!valido) {
            throw new RuntimeException(
                    "Tipo de documento '" + tipo + "' no corresponde a tu modalidad de titulación.");
        }
    }

    private record TipoMeta(
            String tipo, int orden, String etiqueta, String descripcion, boolean obligatorio) {}
}