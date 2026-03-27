
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

    /**
     * Catálogo ESTUDIANTE TIC (6 documentos — Arts. 10, 11, 59).
     * CERTIFICADO_ANTIPLAGIO se eliminó del flujo del estudiante:
     * el Director lo emite y sube directamente (Art. 57 num.2).
     */
    private static final List<TipoMeta> CATALOGO = List.of(
            new TipoMeta("INFORME_DIRECTOR", 1, "Informe del Director",
                    "Art. 59a: Informe favorable emitido por el/la Director(a) del TIC una vez finalizado el trabajo.", true),
            new TipoMeta("TRABAJO_FINAL_PDF", 2, "Trabajo Final (PDF para Biblioteca)",
                    "Art. 59a: Copia digital del trabajo final empastado que será remitida a la biblioteca.", true),
            new TipoMeta("CERTIFICADO_PENSUM", 3, "Certificado Pensum Completo (SGA)",
                    "Art. 10: Certificado del SGA que acredita haber aprobado la totalidad del pensum.", true),
            new TipoMeta("CERTIFICADO_DEUDAS", 4, "Certificado Sin Deudas Administrativas (SGA)",
                    "Art. 11: Certificado del SGA de no tener deudas con ninguna instancia de la UTEQ.", true),
            new TipoMeta("CERTIFICADO_IDIOMA", 5, "Certificado Idioma Extranjero",
                    "Art. 18a: Certificado de haber aprobado el idioma extranjero.", true),
            new TipoMeta("CERTIFICADO_PRACTICAS", 6, "Certificado Prácticas y Servicio Comunitario",
                    "Art. 18b: Certificado de horas de Prácticas Laborales y Servicio Comunitario.", true)
    );
    /** Catálogo COMPLEXIVO (4 documentos — Arts. 10, 11, 18a, 18b)
     *  El informe práctico ya se gestiona en ComplexivoInformePractico.
     *  No aplica COMPILATIO, Informe Director ni Trabajo Final PDF (son del TIC). */
    private static final List<TipoMeta> CATALOGO_COMPLEXIVO = List.of(
            new TipoMeta("CERTIFICADO_PENSUM", 1, "Certificado Pensum Completo (SGA)",
                    "Art. 10: Certificado del SGA que acredita haber aprobado la totalidad del pensum académico.", true),
            new TipoMeta("CERTIFICADO_DEUDAS", 2, "Certificado Sin Deudas Administrativas (SGA)",
                    "Art. 11: Certificado del SGA de no tener deudas con ninguna instancia administrativa o académica de la UTEQ.", true),
            new TipoMeta("CERTIFICADO_IDIOMA", 3, "Certificado Idioma Extranjero",
                    "Art. 18a: Certificado de haber aprobado el idioma extranjero definido en la oferta académica.", true),
            new TipoMeta("CERTIFICADO_PRACTICAS", 4, "Certificado Prácticas y Servicio Comunitario",
                    "Art. 18b: Certificado de horas de Prácticas Laborales y Servicio Comunitario aprobadas.", true)
    );

    private final DocumentoHabilitanteRepository habilitanteRepo;
    private final ProyectoTitulacionRepository   proyectoRepo;
    private final EstudianteRepository           estudianteRepo;
    private final DocenteRepository              docenteRepo;
    private final ComplexivoTitulacionRepository complexivoRepo;
    private final PeriodoTitulacionRepository    periodoRepo;

    public DocumentoHabilitanteService(
            DocumentoHabilitanteRepository habilitanteRepo,
            ProyectoTitulacionRepository proyectoRepo,
            EstudianteRepository estudianteRepo,
            DocenteRepository docenteRepo,
            ComplexivoTitulacionRepository complexivoRepo,
            PeriodoTitulacionRepository periodoRepo) {
        this.habilitanteRepo = habilitanteRepo;
        this.proyectoRepo    = proyectoRepo;
        this.estudianteRepo  = estudianteRepo;
        this.docenteRepo     = docenteRepo;
        this.complexivoRepo  = complexivoRepo;
        this.periodoRepo     = periodoRepo;
    }

    // ══════════════════════════════════════════════════════════
    // TIC — resumen por estudiante
    // ══════════════════════════════════════════════════════════
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorEstudiante(
            Integer idEstudiante) {

        List<DocumentoHabilitante> existentes =
                habilitanteRepo.findByEstudiante_IdEstudiante(idEstudiante)
                        .stream()
                        .filter(d -> d.getProyecto() != null)
                        .toList();

        ProyectoTitulacion proyecto = proyectoRepo.findAll().stream()
                .filter(p -> p.getEleccion() != null
                        && p.getEleccion().getEstudiante() != null
                        && p.getEleccion().getEstudiante().getIdEstudiante().equals(idEstudiante))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró proyecto activo para el estudiante"));

        return construirResumen(proyecto, existentes);
    }

    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenPorProyecto(
            Integer idProyecto) {
        ProyectoTitulacion proyecto = proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + idProyecto));
        List<DocumentoHabilitante> existentes =
                habilitanteRepo.findByProyecto_IdProyecto(idProyecto);
        return construirResumen(proyecto, existentes);
    }

    // ══════════════════════════════════════════════════════════
    // COMPLEXIVO — resumen por estudiante
    // ══════════════════════════════════════════════════════════
    public DocumentoHabilitanteDtos.ResumenHabilitacionDto obtenerResumenComplexivo(
            Integer idEstudiante) {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new RuntimeException("No hay periodo activo"));

        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró registro Complexivo activo para el estudiante"));

        List<DocumentoHabilitante> existentes =
                habilitanteRepo.findByComplexivo_IdComplexivo(ct.getIdComplexivo());

        return construirResumenComplexivo(ct, existentes);
    }

    // ══════════════════════════════════════════════════════════
    // TIC — subir documento
    // ══════════════════════════════════════════════════════════
    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumento(
            Integer idEstudiante,
            DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {

        validarTipo(req.getTipoDocumento());

        ProyectoTitulacion proyecto = proyectoRepo.findById(req.getIdProyecto())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        DocumentoHabilitante doc = habilitanteRepo
                .findByProyecto_IdProyectoAndTipoDocumento(
                        req.getIdProyecto(), req.getTipoDocumento())
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

        // El porcentaje de antiplagio lo registra el Director al validar, no al subir
        habilitanteRepo.save(doc);
        return toDto(doc);
    }

    // ══════════════════════════════════════════════════════════
    // COMPLEXIVO — subir documento
    // ══════════════════════════════════════════════════════════
    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto subirDocumentoComplexivo(
            Integer idEstudiante,
            DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {

        validarTipoComplexivo(req.getTipoDocumento());

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new RuntimeException("No hay periodo activo"));

        ComplexivoTitulacion ct = complexivoRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
                        idEstudiante, periodo.getIdPeriodo())
                .orElseThrow(() -> new RuntimeException("Sin registro Complexivo activo"));

        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        DocumentoHabilitante doc = habilitanteRepo
                .findByComplexivo_IdComplexivoAndTipoDocumento(
                        ct.getIdComplexivo(), req.getTipoDocumento())
                .orElse(new DocumentoHabilitante());

        doc.setComplexivo(ct);
        doc.setProyecto(null);
        doc.setEstudiante(estudiante);
        doc.setTipoDocumento(req.getTipoDocumento());
        doc.setNombreArchivo(req.getNombreArchivo());
        doc.setUrlArchivo(req.getUrlArchivo());
        doc.setFormato("PDF");
        doc.setEstado("ENVIADO");
        doc.setComentarioValidacion(null);
        doc.setFechaValidacion(null);
        doc.setValidadoPor(null);

        habilitanteRepo.save(doc);
        return toDto(doc);
    }

    // ══════════════════════════════════════════════════════════
    // DIRECTOR — validar documento
    // ══════════════════════════════════════════════════════════
    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto validarDocumento(
            Integer idDocente,
            Integer idHabilitante,
            DocumentoHabilitanteDtos.ValidarHabilitanteRequest req) {

        DocumentoHabilitante doc = habilitanteRepo.findById(idHabilitante)
                .orElseThrow(() -> new RuntimeException(
                        "Documento habilitante no encontrado: " + idHabilitante));

        if (!"ENVIADO".equals(doc.getEstado()))
            throw new RuntimeException("Solo se pueden validar documentos en estado ENVIADO");

        Docente validador = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        String decision = req.getDecision();
        if (!"APROBADO".equals(decision) && !"RECHAZADO".equals(decision))
            throw new RuntimeException("La decisión debe ser APROBADO o RECHAZADO");

        // Si es antiplagio, el director registra el porcentaje real
        if ("CERTIFICADO_ANTIPLAGIO".equals(doc.getTipoDocumento())) {
            if (req.getPorcentajeCoincidencia() == null)
                throw new RuntimeException("Debe ingresar el porcentaje de coincidencia de COMPILATIO");
            BigDecimal umbral = new BigDecimal("10.00");
            doc.setPorcentajeCoincidencia(req.getPorcentajeCoincidencia());
            doc.setUmbralPermitido(umbral);
            boolean dentroUmbral = req.getPorcentajeCoincidencia().compareTo(umbral) <= 0;
            doc.setResultadoAntiplagio(dentroUmbral ? "APROBADO" : "RECHAZADO");
            // Si supera el umbral, se fuerza RECHAZADO sin importar lo que diga el director
            if (!dentroUmbral) {
                decision = "RECHAZADO";
                doc.setComentarioValidacion("Porcentaje (" + req.getPorcentajeCoincidencia()
                        + "%) supera el umbral del 10% (Art. 57 num.2). " +
                        (req.getComentario() != null ? req.getComentario() : ""));
            }
        }

        doc.setEstado(decision);
        doc.setValidadoPor(validador);
        if (doc.getComentarioValidacion() == null) {
            doc.setComentarioValidacion(req.getComentario());
        }
        doc.setFechaValidacion(LocalDateTime.now());

        habilitanteRepo.save(doc);
        return toDto(doc);
    }







    // ══════════════════════════════════════════════════════════
// DIRECTOR — subir certificado antiplagio (Art. 57 num.2)
// El Director corre COMPILATIO, emite el certificado firmado
// y lo sube directamente al sistema. El estudiante no interviene.
// ══════════════════════════════════════════════════════════
    @Transactional
    public DocumentoHabilitanteDtos.HabilitanteDto subirCertificadoAntiplagio(
            Integer idDocente,
            Integer idProyecto,
            DocumentoHabilitanteDtos.SubirAntiplagioPorDirectorRequest req) {

        ProyectoTitulacion proyecto = proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado: " + idProyecto));

        // Verificar que el docente es el director de este proyecto
        if (proyecto.getDirector() == null ||
                !proyecto.getDirector().getIdDocente().equals(idDocente))
            throw new RuntimeException(
                    "Solo el Director del proyecto puede subir el certificado antiplagio");

        Docente director = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        // Buscar el estudiante del proyecto
        Estudiante estudiante = proyecto.getEleccion().getEstudiante();

        // Upsert: si ya existe lo reemplazamos
        DocumentoHabilitante doc = habilitanteRepo
                .findByProyecto_IdProyectoAndTipoDocumento(idProyecto, "CERTIFICADO_ANTIPLAGIO")
                .orElse(new DocumentoHabilitante());

        if (req.getPorcentajeCoincidencia() == null)
            throw new RuntimeException(
                    "Debe ingresar el porcentaje de coincidencia del reporte COMPILATIO");

        BigDecimal umbral = new BigDecimal("10.00");
        boolean dentroUmbral = req.getPorcentajeCoincidencia().compareTo(umbral) <= 0;

        doc.setProyecto(proyecto);
        doc.setEstudiante(estudiante);
        doc.setTipoDocumento("CERTIFICADO_ANTIPLAGIO");
        doc.setNombreArchivo(req.getNombreArchivo());
        doc.setUrlArchivo(req.getUrlArchivo());
        doc.setFormato("PDF");
        doc.setPorcentajeCoincidencia(req.getPorcentajeCoincidencia());
        doc.setUmbralPermitido(umbral);
        doc.setResultadoAntiplagio(dentroUmbral ? "APROBADO" : "RECHAZADO");
        doc.setValidadoPor(director);
        doc.setFechaValidacion(LocalDateTime.now());

        if (dentroUmbral) {
            doc.setEstado("APROBADO");
            doc.setComentarioValidacion(
                    "Certificado emitido por el Director. Coincidencia: "
                            + req.getPorcentajeCoincidencia() + "% (dentro del umbral del 10%, Art. 57 num.2).");
        } else {
            doc.setEstado("RECHAZADO");
            doc.setComentarioValidacion(
                    "Porcentaje (" + req.getPorcentajeCoincidencia()
                            + "%) supera el umbral del 10% (Art. 57 num.2). El trabajo debe ser corregido.");
        }

        habilitanteRepo.save(doc);
        return toDto(doc);
    }








    // ══════════════════════════════════════════════════════════
    // DIRECTOR — pendientes
    // ══════════════════════════════════════════════════════════
    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesPorDirector(
            Integer idDocente) {
        return habilitanteRepo.findPendientesPorDirector(idDocente)
                .stream().map(this::toDto).toList();
    }


    public List<DocumentoHabilitanteDtos.HabilitanteDto> pendientesPorDirectorYComplexivo(
            Integer idDocente) {
        // TIC: documentos de proyectos donde el docente es director
        List<DocumentoHabilitanteDtos.HabilitanteDto> tic =
                habilitanteRepo.findPendientesPorDirector(idDocente)
                        .stream().map(this::toDto).toList();

        // Complexivo: documentos de estudiantes donde el docente es DT2 asignado
        List<DocumentoHabilitanteDtos.HabilitanteDto> complexivo =
                habilitanteRepo.findPendientesComplexivoPorDocente(idDocente)
                        .stream().map(this::toDto).toList();

        List<DocumentoHabilitanteDtos.HabilitanteDto> todos = new ArrayList<>(tic);
        todos.addAll(complexivo);
        return todos;
    }



    // ══════════════════════════════════════════════════════════
    // Helpers privados
    // ══════════════════════════════════════════════════════════
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
                var ph = new DocumentoHabilitanteDtos.HabilitanteDto();
                ph.setIdProyecto(proyecto.getIdProyecto());
                ph.setTipoDocumento(meta.tipo);
                ph.setEtiquetaTipo(meta.etiqueta);
                ph.setDescripcionTipo(meta.descripcion);
                ph.setObligatorio(meta.obligatorio);
                ph.setEstado("PENDIENTE");
                docs.add(ph);
            }
        }

        return armarResumen(proyecto.getIdProyecto(), proyecto.getTitulo(), docs, CATALOGO.size());
    }

    private DocumentoHabilitanteDtos.ResumenHabilitacionDto construirResumenComplexivo(
            ComplexivoTitulacion ct,
            List<DocumentoHabilitante> existentes) {

        Map<String, DocumentoHabilitante> porTipo = new HashMap<>();
        existentes.forEach(d -> porTipo.put(d.getTipoDocumento(), d));

        List<DocumentoHabilitanteDtos.HabilitanteDto> docs = new ArrayList<>();
        for (TipoMeta meta : CATALOGO_COMPLEXIVO) {
            DocumentoHabilitante entidad = porTipo.get(meta.tipo);
            if (entidad != null) {
                docs.add(toDto(entidad));
            } else {
                var ph = new DocumentoHabilitanteDtos.HabilitanteDto();
                ph.setIdProyecto(ct.getIdComplexivo());
                ph.setIdEstudiante(ct.getEstudiante().getIdEstudiante());
                ph.setTipoDocumento(meta.tipo);
                ph.setEtiquetaTipo(meta.etiqueta);
                ph.setDescripcionTipo(meta.descripcion);
                ph.setObligatorio(meta.obligatorio);
                ph.setEstado("PENDIENTE");
                docs.add(ph);
            }
        }

        return armarResumen(ct.getIdComplexivo(),
                "Documentos Habilitantes — Examen Complexivo",
                docs, CATALOGO_COMPLEXIVO.size());
    }

    private DocumentoHabilitanteDtos.ResumenHabilitacionDto armarResumen(
            Integer idRef, String titulo,
            List<DocumentoHabilitanteDtos.HabilitanteDto> docs,
            int totalCatalogo) {

        long aprobados  = docs.stream().filter(d -> "APROBADO".equals(d.getEstado())).count();
        long pendientes = docs.stream().filter(d -> "PENDIENTE".equals(d.getEstado())).count();
        long rechazados = docs.stream().filter(d -> "RECHAZADO".equals(d.getEstado())).count();

        var r = new DocumentoHabilitanteDtos.ResumenHabilitacionDto();
        r.setIdProyecto(idRef);
        r.setTituloProyecto(titulo);
        r.setDocumentos(docs);
        r.setTotalDocumentos(docs.size());
        r.setAprobados((int) aprobados);
        r.setPendientes((int) pendientes);
        r.setRechazados((int) rechazados);
        r.setHabilitadoParaSustentacion(aprobados == totalCatalogo);
        return r;
    }

    private void aplicarAntiplagio(DocumentoHabilitante doc,
                                   DocumentoHabilitanteDtos.SubirHabilitanteRequest req) {
        if (!"CERTIFICADO_ANTIPLAGIO".equals(req.getTipoDocumento())) return;
        if (req.getPorcentajeCoincidencia() == null)
            throw new RuntimeException(
                    "Debe ingresar el porcentaje de coincidencia del reporte COMPILATIO");

        BigDecimal umbral = req.getUmbralPermitido() != null
                ? req.getUmbralPermitido() : new BigDecimal("10.00");
        doc.setPorcentajeCoincidencia(req.getPorcentajeCoincidencia());
        doc.setUmbralPermitido(umbral);

        boolean ok = req.getPorcentajeCoincidencia().compareTo(umbral) <= 0;
        doc.setResultadoAntiplagio(ok ? "APROBADO" : "RECHAZADO");
        if (!ok) {
            doc.setEstado("RECHAZADO");
            doc.setComentarioValidacion(
                    "Porcentaje (" + req.getPorcentajeCoincidencia()
                            + "%) supera el umbral del " + umbral + "% (Art. 57 num.2).");
        }
    }

    private DocumentoHabilitanteDtos.HabilitanteDto toDto(DocumentoHabilitante e) {
        // Buscar en ambos catálogos
        TipoMeta meta = CATALOGO.stream()
                .filter(m -> m.tipo.equals(e.getTipoDocumento()))
                .findFirst()
                .orElseGet(() -> CATALOGO_COMPLEXIVO.stream()
                        .filter(m -> m.tipo.equals(e.getTipoDocumento()))
                        .findFirst()
                        .orElse(new TipoMeta(e.getTipoDocumento(), 99,
                                e.getTipoDocumento(), "", false)));

        var dto = new DocumentoHabilitanteDtos.HabilitanteDto();
        dto.setId(e.getId());
        dto.setIdProyecto(e.getProyecto() != null
                ? e.getProyecto().getIdProyecto()
                : (e.getComplexivo() != null ? e.getComplexivo().getIdComplexivo() : null));
        dto.setIdEstudiante(e.getEstudiante() != null
                ? e.getEstudiante().getIdEstudiante() : null);
        if (e.getEstudiante() != null && e.getEstudiante().getUsuario() != null) {
            dto.setNombreEstudiante(
                    e.getEstudiante().getUsuario().getNombres() + " "
                            + e.getEstudiante().getUsuario().getApellidos());
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
                    e.getValidadoPor().getUsuario().getNombres() + " "
                            + e.getValidadoPor().getUsuario().getApellidos());
        }
        dto.setFechaValidacion(e.getFechaValidacion());
        dto.setFechaSubida(e.getFechaSubida());
        dto.setActualizadoEn(e.getActualizadoEn());
        return dto;
    }

    private void validarTipo(String tipo) {
        if (CATALOGO.stream().noneMatch(m -> m.tipo.equals(tipo)))
            throw new RuntimeException("Tipo de documento no reconocido: " + tipo);
    }

    private void validarTipoComplexivo(String tipo) {
        if (CATALOGO_COMPLEXIVO.stream().noneMatch(m -> m.tipo.equals(tipo)))
            throw new RuntimeException(
                    "Tipo no válido para Examen Complexivo: " + tipo +
                            ". Permitidos: CERTIFICADO_PENSUM, CERTIFICADO_DEUDAS, " +
                            "CERTIFICADO_IDIOMA, CERTIFICADO_PRACTICAS");
    }

    private record TipoMeta(String tipo, int orden, String etiqueta,
                            String descripcion, boolean obligatorio) {}
}