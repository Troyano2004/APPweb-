package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal de Titulación II — 5 módulos:
 * M1: Configuración inicial (asignar DT2, Director, Tribunal)
 * M2: Seguimiento de avances (asesorías y actas de corte)
 * M3: Certificación antiplagio
 * M4: Predefensa (60% DT2 + 40% Tribunal)
 * M5: Sustentación final (4 criterios, nota de grado)
 *
 * ✅ REFACTOR: Las validaciones de módulos M3, M4 y M5 ahora se basan
 *    en el estado del DOCUMENTO (no del proyecto). El proyecto puede
 *    cambiar de estado como efecto secundario pero no es la fuente de verdad.
 *
 * Flujo de estados del documento:
 *   BORRADOR → EN_REVISION → CORRECCION_REQUERIDA → APROBADO_POR_DIRECTOR
 *   → ANTIPLAGIO_APROBADO → EN_PREDEFENSA → LISTO_SUSTENTACION
 */
@Service
public class Dt2Service {

    private static final int MIN_ASESORIAS_CORTE = 5;
    private static final int MIN_MIEMBROS_TRIBUNAL = 3;
    private static final BigDecimal UMBRAL_ANTIPLAGIO = new BigDecimal("10.00");
    private static final BigDecimal NOTA_APROBATORIA_SUSTENTACION = new BigDecimal("7.00");
    private static final int DIAS_SEGUNDA_OPORTUNIDAD = 15;

    private final ProyectoTitulacionRepository proyectoRepo;
    private final AnteproyectoTitulacionRepository anteproyectoRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final DocenteRepository docenteRepo;
    private final UsuarioRepository usuarioRepo;
    private final Dt2AsignacionRepository dt2AsignacionRepo;
    private final BitacoraAsignacionRepository bitacoraRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final AsesoriaDirectorRepository asesoriaRepo;
    private final ActaCorteRepository actaCorteRepo;
    private final AntiplacioIntentoRepository antiplacioRepo;
    private final SustentacionRepository sustentacionRepo;
    private final EvaluacionSustentacionRepository evaluacionRepo;
    private final DocumentoPrevioSustentacionRepository docPrevioRepo;
    private final ActaGradoRepository actaGradoRepo;
    private final DocumentStorageService storageService;
    private final EmailService emailService;

    public Dt2Service(ProyectoTitulacionRepository proyectoRepo,
                      AnteproyectoTitulacionRepository anteproyectoRepo,
                      DocumentoTitulacionRepository documentoRepo,
                      DocenteRepository docenteRepo,
                      UsuarioRepository usuarioRepo,
                      Dt2AsignacionRepository dt2AsignacionRepo,
                      BitacoraAsignacionRepository bitacoraRepo,
                      TribunalProyectoRepository tribunalRepo,
                      AsesoriaDirectorRepository asesoriaRepo,
                      ActaCorteRepository actaCorteRepo,
                      AntiplacioIntentoRepository antiplacioRepo,
                      SustentacionRepository sustentacionRepo,
                      EvaluacionSustentacionRepository evaluacionRepo,
                      DocumentoPrevioSustentacionRepository docPrevioRepo,
                      ActaGradoRepository actaGradoRepo,
                      DocumentStorageService storageService,
                      EmailService emailService) {
        this.proyectoRepo = proyectoRepo;
        this.anteproyectoRepo = anteproyectoRepo;
        this.documentoRepo = documentoRepo;
        this.docenteRepo = docenteRepo;
        this.usuarioRepo = usuarioRepo;
        this.dt2AsignacionRepo = dt2AsignacionRepo;
        this.bitacoraRepo = bitacoraRepo;
        this.tribunalRepo = tribunalRepo;
        this.asesoriaRepo = asesoriaRepo;
        this.actaCorteRepo = actaCorteRepo;
        this.antiplacioRepo = antiplacioRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.evaluacionRepo = evaluacionRepo;
        this.docPrevioRepo = docPrevioRepo;
        this.actaGradoRepo = actaGradoRepo;
        this.storageService = storageService;
        this.emailService = emailService;
    }

    // =========================================================
    // MÓDULO 1 — Configuración inicial de Titulación II
    // =========================================================

    /**
     * Lista proyectos con anteproyecto APROBADO que aún no tienen configuración DT2 completa.
     * ✅ FIX: incluye ANTEPROYECTO, BORRADOR y DESARROLLO
     */
    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosPendientesConfiguracion() {
        return proyectoRepo.findAll().stream()
                .filter(p -> "ANTEPROYECTO".equalsIgnoreCase(p.getEstado())
                        || "BORRADOR".equalsIgnoreCase(p.getEstado())
                        || "DESARROLLO".equalsIgnoreCase(p.getEstado()))
                .filter(p -> {
                    AnteproyectoTitulacion ante = anteproyectoRepo
                            .findByPropuesta_IdPropuesta(p.getPropuesta().getIdPropuesta())
                            .orElse(null);
                    // Mostrar si tiene anteproyecto aprobado O si ya está en DESARROLLO
                    return (ante != null && "APROBADO".equalsIgnoreCase(ante.getEstado()))
                            || "DESARROLLO".equalsIgnoreCase(p.getEstado());
                })
                .filter(p -> {
                    boolean tieneDt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(p.getIdProyecto()).isPresent();
                    boolean tieneDirector = p.getDirector() != null;
                    boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(p.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;
                    return !(tieneDt2 && tieneDirector && tieneTribunal);
                })
                .map(p -> {
                    boolean tieneDt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(p.getIdProyecto()).isPresent();
                    boolean tieneDirector = p.getDirector() != null;
                    boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(p.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;

                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                    dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
                    dto.setPeriodo(p.getPeriodo().getDescripcion());
                    dto.setEstadoProyecto(p.getEstado());
                    dto.setTieneDocenteDt2(tieneDt2);
                    dto.setTieneDirector(tieneDirector);
                    dto.setTieneTribunal(tieneTribunal);
                    dto.setConfiguracionCompleta(tieneDt2 && tieneDirector && tieneTribunal);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "Dt2Asignacion", accion = "CREATE")
    @Transactional
    public Dt2Dtos.MensajeDto asignarDocenteDt2(Dt2Dtos.AsignarDocenteDt2Request req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        Docente docente = getDocente(req.getIdDocenteDt2());
        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());

        dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(req.getIdProyecto())
                .ifPresent(a -> {
                    a.setActivo(false);
                    dt2AsignacionRepo.save(a);
                });

        Dt2Asignacion asignacion = new Dt2Asignacion();
        asignacion.setProyecto(proyecto);
        asignacion.setDocenteDt2(docente);
        asignacion.setAsignadoPor(realizadoPor);
        asignacion.setPeriodo(req.getPeriodo());
        asignacion.setObservacion(req.getObservacion());
        asignacion.setActivo(true);
        dt2AsignacionRepo.save(asignacion);

        registrarBitacora(proyecto, "DOCENTE_DT2", docente.getIdDocente(),
                fullName(docente.getUsuario()), null, realizadoPor,
                req.getPeriodo(), req.getObservacion());

        intentarTransicionDesarrollo(proyecto);

        // ✅ Notificación por correo al docente DT2
        String emailDocente = docente.getUsuario() != null ? docente.getUsuario().getCorreoInstitucional() : null;
        if (emailDocente != null && !emailDocente.isBlank()) {
            String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
            String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
            emailService.notificarAsignacionDocenteDt2(
                    emailDocente,
                    fullName(docente.getUsuario()),
                    proyecto.getTitulo(),
                    estudiante,
                    periodo
            );
        }

        return new Dt2Dtos.MensajeDto("Docente DT2 asignado correctamente", proyecto.getEstado(), true);
    }

    @Transactional
    public Dt2Dtos.MensajeDto asignarDirector(Dt2Dtos.AsignarDirectorRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        // ✅ FIX: solo pide motivo si YA tiene director asignado (campo no nulo)
        if (proyecto.getDirector() != null
                && (req.getMotivo() == null || req.getMotivo().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cambiar el Director debe ingresar un motivo justificado");
        }

        Docente director = getDocente(req.getIdDirector());
        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());

        proyecto.setDirector(director);
        proyectoRepo.save(proyecto);

        documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())
                .ifPresent(doc -> {
                    doc.setDirector(director);
                    documentoRepo.save(doc);
                });

        registrarBitacora(proyecto, "DIRECTOR", director.getIdDocente(),
                fullName(director.getUsuario()), null, realizadoPor,
                req.getPeriodo(), req.getMotivo());

        intentarTransicionDesarrollo(proyecto);

        // ✅ Notificación por correo al director
        String emailDirector = director.getUsuario() != null ? director.getUsuario().getCorreoInstitucional() : null;
        if (emailDirector != null && !emailDirector.isBlank()) {
            String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
            String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
            emailService.notificarAsignacionDirector(
                    emailDirector,
                    fullName(director.getUsuario()),
                    proyecto.getTitulo(),
                    estudiante,
                    periodo
            );
        }

        return new Dt2Dtos.MensajeDto("Director asignado correctamente", proyecto.getEstado(), true);
    }

    @Auditable(entidad = "TribunalProyecto", accion = "CREATE")
    @Transactional
    public Dt2Dtos.MensajeDto asignarTribunal(Dt2Dtos.AsignarTribunalDt2Request req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        if (req.getMiembros() == null || req.getMiembros().size() < MIN_MIEMBROS_TRIBUNAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren al menos " + MIN_MIEMBROS_TRIBUNAL + " miembros del tribunal");
        }

        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());
        tribunalRepo.deleteByProyecto_IdProyecto(proyecto.getIdProyecto());

        for (Dt2Dtos.MiembroTribunalRequest m : req.getMiembros()) {
            Docente docente = getDocente(m.getIdDocente());
            TribunalProyecto tp = new TribunalProyecto();
            tp.setProyecto(proyecto);
            tp.setDocente(docente);
            tp.setCargo(m.getCargo() != null ? m.getCargo().toUpperCase() : "VOCAL");
            tribunalRepo.save(tp);

            registrarBitacora(proyecto, "TRIBUNAL", docente.getIdDocente(),
                    fullName(docente.getUsuario()), tp.getCargo(), realizadoPor,
                    req.getPeriodo(), null);

            // ✅ Notificación por correo a cada miembro del tribunal
            String emailTribunal = docente.getUsuario() != null ? docente.getUsuario().getCorreoInstitucional() : null;
            if (emailTribunal != null && !emailTribunal.isBlank()) {
                String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
                String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
                emailService.notificarAsignacionTribunal(
                        emailTribunal,
                        fullName(docente.getUsuario()),
                        tp.getCargo(),
                        proyecto.getTitulo(),
                        estudiante,
                        periodo
                );
            }
        }

        intentarTransicionDesarrollo(proyecto);
        return new Dt2Dtos.MensajeDto("Tribunal registrado correctamente", proyecto.getEstado(), true);
    }

    @Transactional
    public Dt2Dtos.ConfiguracionProyectoDto getConfiguracion(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        Dt2Dtos.ConfiguracionProyectoDto dto = new Dt2Dtos.ConfiguracionProyectoDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstadoProyecto(proyecto.getEstado());

        dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(idProyecto).ifPresent(a -> {
            dto.setDocenteDt2(fullName(a.getDocenteDt2().getUsuario()));
            dto.setIdDocenteDt2(a.getDocenteDt2().getIdDocente());
        });

        DocumentoTitulacion doc = documentoRepo.findByProyecto_IdProyecto(idProyecto).orElse(null);
        Docente director = doc != null && doc.getDirector() != null ? doc.getDirector() : proyecto.getDirector();
        if (director != null) {
            dto.setDirector(fullName(director.getUsuario()));
            dto.setIdDirector(director.getIdDocente());
        }

        List<Dt2Dtos.MiembroTribunalDto> miembros = tribunalRepo.findByProyecto_IdProyecto(idProyecto)
                .stream().map(tp -> {
                    Dt2Dtos.MiembroTribunalDto m = new Dt2Dtos.MiembroTribunalDto();
                    m.setIdTribunal(tp.getIdTribunal());
                    m.setIdDocente(tp.getDocente().getIdDocente());
                    m.setNombre(fullName(tp.getDocente().getUsuario()));
                    m.setCargo(tp.getCargo());
                    return m;
                }).collect(Collectors.toList());
        dto.setTribunal(miembros);

        boolean completa = dto.getDocenteDt2() != null
                && dto.getDirector() != null
                && miembros.size() >= MIN_MIEMBROS_TRIBUNAL;
        dto.setConfiguracionCompleta(completa);

        List<Dt2Dtos.BitacoraDto> bitacora = bitacoraRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().map(b -> {
                    Dt2Dtos.BitacoraDto bd = new Dt2Dtos.BitacoraDto();
                    bd.setTipo(b.getTipoAsignacion());
                    bd.setNombreAsignado(b.getNombreAsignado());
                    bd.setCargo(b.getCargo());
                    bd.setRealizadoPor(fullName(b.getRealizadoPor()));
                    bd.setFecha(b.getFecha());
                    bd.setObservacion(b.getObservacion());
                    return bd;
                }).collect(Collectors.toList());
        dto.setBitacora(bitacora);

        return dto;
    }

    // =========================================================
    // MÓDULO 2 — Seguimiento de avances
    // =========================================================


    /**
     * ✅ NUEVO: Lista todos los proyectos en estado PREDEFENSA.
     * Usado por el coordinador en la vista de predefensa.
     */
    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosEnPredefensa() {
        return proyectoRepo.findAll().stream()
                .filter(p -> "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstadoProyecto(p.getEstado());
                    if (p.getPropuesta() != null) {
                        if (p.getPropuesta().getEstudiante() != null)
                            dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                        if (p.getPropuesta().getCarrera() != null)
                            dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
                    }
                    if (p.getPeriodo() != null)
                        dto.setPeriodo(p.getPeriodo().getDescripcion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosDirector(Integer idDirector) {
        return proyectoRepo.findByDirector_IdDocente(idDirector).stream()
                .filter(p -> "DESARROLLO".equalsIgnoreCase(p.getEstado())
                        || "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                    dto.setEstadoProyecto(p.getEstado());
                    return dto;
                }).collect(Collectors.toList());
    }


    /**
     * ✅ NUEVO: proyectos donde el docente está asignado como DT2
     * y el documento está en APROBADO_POR_DIRECTOR (listos para subir antiplagio).
     */
    /**
     * ✅ NUEVO: Lista proyectos en PREDEFENSA donde el docente es miembro del tribunal.
     */
    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosTribunal(Integer idDocente) {
        return tribunalRepo.findByDocente_IdDocente(idDocente).stream()
                .map(tp -> tp.getProyecto())
                .filter(p -> "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .distinct()
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstadoProyecto(p.getEstado());
                    if (p.getPropuesta() != null) {
                        if (p.getPropuesta().getEstudiante() != null)
                            dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                        if (p.getPropuesta().getCarrera() != null)
                            dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
                    }
                    if (p.getPeriodo() != null)
                        dto.setPeriodo(p.getPeriodo().getDescripcion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosDocenteDt2(Integer idDocenteDt2) {
        List<Dt2Asignacion> asignaciones = dt2AsignacionRepo.findByDocenteDt2_IdDocenteAndActivoTrue(idDocenteDt2);
        List<Dt2Dtos.ProyectoPendienteConfiguracionDto> resultado = new java.util.ArrayList<>();

        for (Dt2Asignacion asignacion : asignaciones) {
            ProyectoTitulacion proyecto = asignacion.getProyecto();
            if (proyecto == null) continue;

            // Proyectos con documento en APROBADO_POR_DIRECTOR o estados posteriores
            boolean listo = documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())
                    .map(doc -> doc.getEstado() == EstadoDocumento.APROBADO_POR_DIRECTOR
                            || doc.getEstado() == EstadoDocumento.ANTIPLAGIO_APROBADO
                            || doc.getEstado() == EstadoDocumento.EN_PREDEFENSA
                            || doc.getEstado() == EstadoDocumento.LISTO_SUSTENTACION)
                    .orElse(false);
            if (!listo) continue;

            Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
            dto.setIdProyecto(proyecto.getIdProyecto());
            dto.setTitulo(proyecto.getTitulo());
            dto.setEstadoProyecto(proyecto.getEstado());

            if (proyecto.getPropuesta() != null) {
                if (proyecto.getPropuesta().getEstudiante() != null) {
                    dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
                }
                if (proyecto.getPropuesta().getCarrera() != null) {
                    dto.setCarrera(proyecto.getPropuesta().getCarrera().getNombre());
                }
            }
            if (proyecto.getPeriodo() != null) {
                dto.setPeriodo(proyecto.getPeriodo().getDescripcion());
            }

            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional
    public Dt2Dtos.AsesoriaDto registrarAsesoria(Dt2Dtos.RegistrarAsesoriaRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "DESARROLLO", "PREDEFENSA");

        Docente director = getDocente(req.getIdDirector());
        if (proyecto.getDirector() == null
                || !proyecto.getDirector().getIdDocente().equals(req.getIdDirector())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el Director asignado puede registrar asesorías");
        }
        if (req.getNumeroCorte() == null || (req.getNumeroCorte() != 1 && req.getNumeroCorte() != 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de corte debe ser 1 o 2");
        }

        AsesoriaDirector asesoria = new AsesoriaDirector();
        asesoria.setProyecto(proyecto);
        asesoria.setDirector(director);
        asesoria.setFecha(req.getFecha() != null ? req.getFecha() : LocalDateTime.now());
        asesoria.setObservaciones(req.getObservaciones());
        asesoria.setEvidenciaUrl(req.getEvidenciaUrl());
        asesoria.setPorcentajeAvance(req.getPorcentajeAvance());
        asesoria.setNumeroCorte(req.getNumeroCorte());
        asesoria.setCalificacion(req.getCalificacion());
        asesoriaRepo.save(asesoria);

        return mapAsesoria(asesoria);
    }

    @Transactional
    public List<Dt2Dtos.AsesoriaDto> listarAsesorias(Integer idProyecto) {
        return asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().map(this::mapAsesoria).collect(Collectors.toList());
    }

    @Transactional
    public Dt2Dtos.ActaCorteDto cerrarCorte(Dt2Dtos.CerrarCorteRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "DESARROLLO");

        if (actaCorteRepo.existsByProyecto_IdProyectoAndNumeroCorte(req.getIdProyecto(), req.getNumeroCorte())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un acta para el corte " + req.getNumeroCorte());
        }

        Docente director = getDocente(req.getIdDirector());
        long totalAsesorias = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(
                req.getIdProyecto(), req.getNumeroCorte());
        boolean suficientes = totalAsesorias >= MIN_ASESORIAS_CORTE;

        ActaCorte acta = new ActaCorte();
        acta.setProyecto(proyecto);
        acta.setNumeroCorte(req.getNumeroCorte());
        acta.setDirector(director);
        acta.setTotalAsesorias((int) totalAsesorias);
        acta.setAsesoriasSuficientes(suficientes);
        acta.setNotaCorte(req.getNotaCorte());
        acta.setObservaciones(req.getObservaciones());
        actaCorteRepo.save(acta);

        Dt2Dtos.ActaCorteDto dto = mapActaCorte(acta);
        if (!suficientes) {
            dto.setAdvertencia("ADVERTENCIA: Solo se registraron " + totalAsesorias
                    + " asesorías. El mínimo requerido por corte es " + MIN_ASESORIAS_CORTE);
        }
        return dto;
    }

    @Transactional
    public Dt2Dtos.SeguimientoDto getSeguimiento(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        long corte1 = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1);
        long corte2 = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 2);

        BigDecimal ultimoAvance = asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().filter(a -> a.getPorcentajeAvance() != null)
                .findFirst().map(AsesoriaDirector::getPorcentajeAvance).orElse(BigDecimal.ZERO);

        List<Dt2Dtos.ActaCorteDto> actas = actaCorteRepo.findByProyecto_IdProyectoOrderByNumeroCorteAsc(idProyecto)
                .stream().map(this::mapActaCorte).collect(Collectors.toList());

        Dt2Dtos.SeguimientoDto dto = new Dt2Dtos.SeguimientoDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
        dto.setEstadoProyecto(proyecto.getEstado());
        dto.setTotalAsesorias((int) (corte1 + corte2));
        dto.setAsesoriaCorte1((int) corte1);
        dto.setAsesoriaCorte2((int) corte2);
        dto.setUltimoAvance(ultimoAvance);
        dto.setActas(actas);
        return dto;
    }

    // =========================================================
    // MÓDULO 3 — Certificación antiplagio
    // =========================================================

    /**
     * Sube el informe COMPILATIO y registra el intento.
     *
     * ✅ REFACTOR: ahora valida documento.estado = APROBADO_POR_DIRECTOR
     *    (antes validaba estado del proyecto).
     * ✅ REFACTOR: quien sube el antiplagio es el Docente DT2 (no el director).
     * ✅ REFACTOR: si favorable → documento pasa a ANTIPLAGIO_APROBADO.
     *    El proyecto cambia a PREDEFENSA como efecto secundario.
     */
    @Transactional
    public Dt2Dtos.CertificadoAntiplacioDto registrarAntiplagio(
            Integer idProyecto, Integer idDocenteDt2,
            BigDecimal porcentajeCoincidencia, String observaciones,
            MultipartFile archivoPdf) {

        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        // ✅ Validar estado del DOCUMENTO (fuente de verdad)
        DocumentoTitulacion documento = documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para este proyecto"));

        if (documento.getEstado() != EstadoDocumento.APROBADO_POR_DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en estado APROBADO_POR_DIRECTOR para registrar el antiplagio. " +
                            "Estado actual: " + documento.getEstado());
        }

        // ✅ Quien sube es el Docente DT2
        Dt2Asignacion dt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay docente DT2 asignado al proyecto"));

        if (!dt2.getDocenteDt2().getIdDocente().equals(idDocenteDt2)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el Docente DT2 asignado puede subir el informe antiplagio");
        }

        Docente docenteDt2 = dt2.getDocenteDt2();
        String urlInforme = storageService.storeDocument(archivoPdf);
        boolean favorable = porcentajeCoincidencia.compareTo(UMBRAL_ANTIPLAGIO) < 0;

        AntiplacioIntento intento = new AntiplacioIntento();
        intento.setProyecto(proyecto);
        intento.setDirector(docenteDt2);  // campo director reutilizado para DT2
        intento.setPorcentajeCoincidencia(porcentajeCoincidencia);
        intento.setUrlInforme(urlInforme);
        intento.setFavorable(favorable);
        intento.setObservaciones(observaciones);
        antiplacioRepo.save(intento);

        // Actualizar campos del proyecto (solo informativos)
        proyecto.setPorcentajeAntiplagio(porcentajeCoincidencia);
        proyecto.setFechaVerificacionAntiplagio(LocalDate.now());
        proyecto.setUrlInformeAntiplagio(urlInforme);

        if (favorable) {
            // ✅ Estado del DOCUMENTO avanza a ANTIPLAGIO_APROBADO
            documento.setEstado(EstadoDocumento.ANTIPLAGIO_APROBADO);
            documentoRepo.save(documento);

            // Estado del proyecto como efecto secundario
            proyecto.setEstado("PREDEFENSA");
        }
        proyectoRepo.save(proyecto);

        return buildCertificadoDto(idProyecto);
    }

    @Transactional
    public Dt2Dtos.CertificadoAntiplacioDto getCertificadoAntiplagio(Integer idProyecto) {
        getProyecto(idProyecto);
        return buildCertificadoDto(idProyecto);
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    /**
     * Programa la fecha de predefensa.
     *
     * ✅ REFACTOR: valida documento.estado = ANTIPLAGIO_APROBADO
     *    (antes validaba estado del proyecto = PREDEFENSA).
     * ✅ El Coordinador es quien programa la fecha.
     */
    @Transactional
    public Dt2Dtos.MensajeDto programarPredefensa(Dt2Dtos.ProgramarPredefensaRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        // ✅ Validar estado del DOCUMENTO
        DocumentoTitulacion documento = documentoRepo.findByProyecto_IdProyecto(req.getIdProyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para este proyecto"));

        if (documento.getEstado() != EstadoDocumento.ANTIPLAGIO_APROBADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe tener antiplagio aprobado para programar la predefensa. " +
                            "Estado actual: " + documento.getEstado());
        }

        List<Sustentacion> existentes = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(req.getIdProyecto())
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .collect(Collectors.toList());

        Sustentacion sustentacion = existentes.isEmpty() ? new Sustentacion() : existentes.get(0);
        sustentacion.setProyecto(proyecto);
        sustentacion.setTipo("PREDEFENSA");
        sustentacion.setFecha(req.getFecha());
        sustentacion.setHora(req.getHora());
        sustentacion.setLugar(req.getLugar());
        sustentacion.setObservaciones(req.getObservaciones());
        sustentacionRepo.save(sustentacion);

        // ✅ Documento avanza a EN_PREDEFENSA
        documento.setEstado(EstadoDocumento.EN_PREDEFENSA);
        documentoRepo.save(documento);

        return new Dt2Dtos.MensajeDto("Predefensa programada para el " + req.getFecha(), proyecto.getEstado(), true);
    }

    /**
     * El docente DT2 registra su calificación de predefensa (60%).
     * ✅ REFACTOR: valida documento.estado = EN_PREDEFENSA
     */
    @Transactional
    public Dt2Dtos.PredefensaDto calificarPredefensaDocente(Dt2Dtos.CalificarPredefensaDocenteRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        validarEstadoDocumento(documento, EstadoDocumento.EN_PREDEFENSA);

        Dt2Asignacion dt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(req.getIdProyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay docente DT2 asignado al proyecto"));

        if (!dt2.getDocenteDt2().getIdDocente().equals(req.getIdDocenteDt2())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el docente DT2 asignado puede registrar esta calificación");
        }

        validarNota(req.getNota(), "nota del docente DT2");

        Sustentacion sus = getPredefensa(req.getIdProyecto());
        Docente docente = getDocente(req.getIdDocenteDt2());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocenteDt2(), "PREDEFENSA_DOCENTE")
                .orElse(new EvaluacionSustentacion());

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("PREDEFENSA_DOCENTE");
        eval.setNotaFinal(req.getNota());
        eval.setObservaciones(req.getObservaciones());
        evaluacionRepo.save(eval);

        return buildPredefensaDto(req.getIdProyecto(), sus);
    }

    /**
     * Un miembro del tribunal registra su calificación de predefensa (40%).
     * ✅ REFACTOR: valida documento.estado = EN_PREDEFENSA
     * ✅ Si solicita correcciones → documento regresa a CORRECCION_REQUERIDA
     */
    @Transactional
    public Dt2Dtos.PredefensaDto calificarPredefensaTribunal(Dt2Dtos.CalificarPredefensaTribunalRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        validarEstadoDocumento(documento, EstadoDocumento.EN_PREDEFENSA);

        if (!tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(
                req.getIdProyecto(), req.getIdDocente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El docente no pertenece al tribunal de este proyecto");
        }

        validarNota(req.getNota(), "nota del tribunal");

        Sustentacion sus = getPredefensa(req.getIdProyecto());
        Docente docente = getDocente(req.getIdDocente());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocente(), "PREDEFENSA_TRIBUNAL")
                .orElse(new EvaluacionSustentacion());

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("PREDEFENSA_TRIBUNAL");
        eval.setNotaFinal(req.getNota());
        eval.setObservaciones(req.getObservaciones());
        evaluacionRepo.save(eval);

        Dt2Dtos.PredefensaDto dto = buildPredefensaDto(req.getIdProyecto(), sus);

        // ✅ Si pide correcciones → documento regresa a CORRECCION_REQUERIDA
        if (Boolean.TRUE.equals(req.getSolicitudCorrecciones())) {
            documento.setEstado(EstadoDocumento.CORRECCION_REQUERIDA);
            documentoRepo.save(documento);
            dto.setSolicitudCorrecciones(true);
        }

        return dto;
    }

    @Transactional
    public Dt2Dtos.PredefensaDto getPredefensaDto(Integer idProyecto) {
        getProyecto(idProyecto);
        Sustentacion sus = getPredefensaSafe(idProyecto);
        if (sus == null) return new Dt2Dtos.PredefensaDto();
        return buildPredefensaDto(idProyecto, sus);
    }

    // =========================================================
    // MÓDULO 5 — Sustentación final
    // =========================================================

    /**
     * Registra/actualiza el checklist de documentos previos.
     * ✅ REFACTOR: valida documento.estado = EN_PREDEFENSA o LISTO_SUSTENTACION
     *    Si completo → documento avanza a LISTO_SUSTENTACION
     */
    @Transactional
    public Dt2Dtos.DocumentosPreviosDto registrarDocumentosPrevios(Dt2Dtos.DocumentosPreviosRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.EN_PREDEFENSA
                && documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en EN_PREDEFENSA o LISTO_SUSTENTACION. " +
                            "Estado actual: " + documento.getEstado());
        }

        Usuario registradoPor = getUsuario(req.getIdRealizadoPor());

        DocumentoPrevioSustentacion docPrevio = docPrevioRepo.findByProyecto_IdProyecto(req.getIdProyecto())
                .orElse(new DocumentoPrevioSustentacion());

        docPrevio.setProyecto(proyecto);
        docPrevio.setEjemplarImpreso(Boolean.TRUE.equals(req.getEjemplarImpreso()));
        docPrevio.setCopiaDigitalBiblioteca(Boolean.TRUE.equals(req.getCopiaDigitalBiblioteca()));
        docPrevio.setCopiasDigitalesTribunal(Boolean.TRUE.equals(req.getCopiasDigitalesTribunal()));
        docPrevio.setInformeCompilatioFirmado(Boolean.TRUE.equals(req.getInformeCompilatioFirmado()));
        docPrevio.setObservaciones(req.getObservaciones());
        docPrevio.setRegistradoPor(registradoPor);
        docPrevioRepo.save(docPrevio);

        // ✅ Si todos los documentos están completos → documento avanza a LISTO_SUSTENTACION
        if (docPrevio.getCompleto() && documento.getEstado() == EstadoDocumento.EN_PREDEFENSA) {
            documento.setEstado(EstadoDocumento.LISTO_SUSTENTACION);
            documentoRepo.save(documento);

            // Proyecto como efecto secundario
            proyecto.setEstado("SUSTENTACION");
            proyectoRepo.save(proyecto);
        }

        return mapDocumentosPrevios(docPrevio);
    }

    @Transactional
    public Dt2Dtos.DocumentosPreviosDto getDocumentosPrevios(Integer idProyecto) {
        return docPrevioRepo.findByProyecto_IdProyecto(idProyecto)
                .map(this::mapDocumentosPrevios)
                .orElse(new Dt2Dtos.DocumentosPreviosDto(false, false, false, false, false, null));
    }

    /**
     * Programa la fecha de sustentación final.
     * ✅ REFACTOR: valida documento.estado = LISTO_SUSTENTACION
     */
    @Transactional
    public Dt2Dtos.MensajeDto programarSustentacion(Dt2Dtos.ProgramarSustentacionRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION para programar la sustentación. " +
                            "Estado actual: " + documento.getEstado());
        }

        if (!predefensaAprobada(req.getIdProyecto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La predefensa debe estar aprobada antes de programar la sustentación final");
        }

        List<Sustentacion> existentes = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(req.getIdProyecto())
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                .collect(Collectors.toList());

        Sustentacion sus = existentes.isEmpty() ? new Sustentacion() : existentes.get(0);
        sus.setProyecto(proyecto);
        sus.setTipo("DEFENSA_FINAL");
        sus.setFecha(req.getFecha());
        sus.setHora(req.getHora());
        sus.setLugar(req.getLugar());
        sus.setObservaciones(req.getObservaciones());
        sustentacionRepo.save(sus);

        return new Dt2Dtos.MensajeDto("Sustentación final programada para el " + req.getFecha(), proyecto.getEstado(), true);
    }

    @Transactional
    public Dt2Dtos.ResultadoSustentacionDto calificarSustentacion(Dt2Dtos.CalificarSustentacionRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION para calificar la sustentación. " +
                            "Estado actual: " + documento.getEstado());
        }

        if (!tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(
                req.getIdProyecto(), req.getIdDocente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El docente no pertenece al tribunal de este proyecto");
        }

        validarNota(req.getCalidadTrabajo(), "calidad del trabajo");
        validarNota(req.getOriginalidad(), "originalidad");
        validarNota(req.getDominioTema(), "dominio del tema");
        validarNota(req.getPreguntas(), "pertinencia de respuestas");

        BigDecimal notaFinal = req.getCalidadTrabajo().multiply(new BigDecimal("0.20"))
                .add(req.getOriginalidad().multiply(new BigDecimal("0.20")))
                .add(req.getDominioTema().multiply(new BigDecimal("0.30")))
                .add(req.getPreguntas().multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        Sustentacion sus = getDefensaFinal(req.getIdProyecto());
        Docente docente = getDocente(req.getIdDocente());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocente(), "SUSTENTACION")
                .orElse(new EvaluacionSustentacion());

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("SUSTENTACION");
        eval.setCalidadTrabajo(req.getCalidadTrabajo());
        eval.setOriginalidad(req.getOriginalidad());
        eval.setDominioTema(req.getDominioTema());
        eval.setPreguntas(req.getPreguntas());
        eval.setNotaFinal(notaFinal);
        eval.setObservaciones(req.getObservaciones());
        evaluacionRepo.save(eval);

        return buildResultadoSustentacion(req.getIdProyecto(), sus, proyecto);
    }

    @Transactional
    public Dt2Dtos.ResultadoSustentacionDto consolidarResultado(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        DocumentoTitulacion documento = getDocumentoRequerido(idProyecto);
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION para consolidar el resultado.");
        }

        Sustentacion sus = getDefensaFinal(idProyecto);
        long totalMiembros = tribunalRepo.countByProyecto_IdProyecto(idProyecto);
        long miembrosCalificaron = evaluacionRepo.countBySustentacion_IdSustentacionAndTipo(
                sus.getIdSustentacion(), "SUSTENTACION");

        if (miembrosCalificaron < totalMiembros) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan " + (totalMiembros - miembrosCalificaron) + " miembros del tribunal por calificar");
        }

        Dt2Dtos.ResultadoSustentacionDto resultado = buildResultadoSustentacion(idProyecto, sus, proyecto);

        BigDecimal notaSustentacion = resultado.getPromedioTribunal();
        boolean aprobado = notaSustentacion.compareTo(NOTA_APROBATORIA_SUSTENTACION) >= 0;

        Estudiante estudiante = proyecto.getPropuesta().getEstudiante();
        BigDecimal promedioRecord = estudiante.getPromedioRecord80() != null
                ? estudiante.getPromedioRecord80() : BigDecimal.ZERO;
        BigDecimal notaGrado = promedioRecord.multiply(new BigDecimal("0.80"))
                .add(notaSustentacion.multiply(new BigDecimal("0.20")))
                .setScale(2, RoundingMode.HALF_UP);

        ActaGrado acta = actaGradoRepo.findByEstudiante_IdEstudiante(estudiante.getIdEstudiante())
                .orElse(new ActaGrado());
        acta.setEstudiante(estudiante);
        acta.setTipoTitulacion("TRABAJO_INTEGRACION_CURRICULAR");
        acta.setIdOrigen(proyecto.getIdProyecto());
        acta.setNotaRecord(promedioRecord);
        acta.setNotaTitulacion(notaSustentacion);
        acta.setNotaFinal(notaGrado);
        actaGradoRepo.save(acta);

        // ✅ Estado del proyecto como efecto secundario
        proyecto.setEstado(aprobado ? "FINALIZADO" : "REPROBADO");
        proyectoRepo.save(proyecto);

        // ✅ Estado del documento al final del proceso
        documento.setEstado(aprobado
                ? EstadoDocumento.CERRADO_APROBADO
                : EstadoDocumento.CERRADO_REPROBADO);
        documentoRepo.save(documento);

        resultado.setNotaGradoFinal(notaGrado);
        resultado.setPromedioRecordAcademico(promedioRecord);
        resultado.setResultado(aprobado ? "APROBADO" : "REPROBADO");
        resultado.setHabilitadoSegundaOportunidad(!aprobado);

        if (!aprobado) {
            resultado.setFechaLimiteSegundaOportunidad(
                    sus.getFecha().plusDays(DIAS_SEGUNDA_OPORTUNIDAD));
        }

        return resultado;
    }

    @Transactional
    public Dt2Dtos.MensajeDto habilitarSegundaOportunidad(Dt2Dtos.SegundaOportunidadRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "REPROBADO");

        Sustentacion sus2 = new Sustentacion();
        sus2.setProyecto(proyecto);
        sus2.setTipo("DEFENSA_FINAL");
        sus2.setFecha(req.getFechaSustentacion());
        sus2.setHora(req.getHora());
        sus2.setLugar(req.getLugar());
        sus2.setObservaciones("SEGUNDA OPORTUNIDAD — " + (req.getObservaciones() != null ? req.getObservaciones() : ""));
        sustentacionRepo.save(sus2);

        // ✅ Documento regresa a LISTO_SUSTENTACION para segunda oportunidad
        documentoRepo.findByProyecto_IdProyecto(req.getIdProyecto()).ifPresent(doc -> {
            doc.setEstado(EstadoDocumento.LISTO_SUSTENTACION);
            documentoRepo.save(doc);
        });

        proyecto.setEstado("SUSTENTACION");
        proyectoRepo.save(proyecto);

        return new Dt2Dtos.MensajeDto(
                "Segunda oportunidad programada para el " + req.getFechaSustentacion(),
                proyecto.getEstado(), true);
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    private ProyectoTitulacion getProyecto(Integer idProyecto) {
        return proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proyecto no encontrado: " + idProyecto));
    }

    private Docente getDocente(Integer idDocente) {
        return docenteRepo.findById(idDocente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Docente no encontrado: " + idDocente));
    }

    private Usuario getUsuario(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado: " + idUsuario));
    }

    /** Obtiene el documento de titulación o lanza excepción si no existe. */
    private DocumentoTitulacion getDocumentoRequerido(Integer idProyecto) {
        return documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para el proyecto: " + idProyecto));
    }

    /** Valida que el documento esté en el estado esperado. */
    private void validarEstadoDocumento(DocumentoTitulacion documento, EstadoDocumento estadoEsperado) {
        if (documento.getEstado() != estadoEsperado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en estado " + estadoEsperado +
                            ". Estado actual: " + documento.getEstado());
        }
    }

    private void validarEstadoProyecto(ProyectoTitulacion p, String... estadosValidos) {
        for (String estado : estadosValidos) {
            if (estado.equalsIgnoreCase(p.getEstado())) return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El proyecto está en estado '" + p.getEstado() + "'. Estados válidos: "
                        + String.join(", ", estadosValidos));
    }

    private void validarAnteproyectoAprobado(ProyectoTitulacion proyecto) {
        // Si ya está en DESARROLLO, el anteproyecto ya fue aprobado en su momento
        if ("DESARROLLO".equalsIgnoreCase(proyecto.getEstado())) return;

        AnteproyectoTitulacion ante = anteproyectoRepo
                .findByPropuesta_IdPropuesta(proyecto.getPropuesta().getIdPropuesta())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proyecto no tiene anteproyecto registrado"));
        if (!"APROBADO".equalsIgnoreCase(ante.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El anteproyecto debe estar APROBADO para configurar DT2. Estado actual: " + ante.getEstado());
        }
    }

    private void intentarTransicionDesarrollo(ProyectoTitulacion proyecto) {
        if (!"ANTEPROYECTO".equalsIgnoreCase(proyecto.getEstado())) return;

        boolean tieneDt2 = dt2AsignacionRepo.existsByProyecto_IdProyecto(proyecto.getIdProyecto());
        boolean tieneDirector = proyecto.getDirector() != null;
        boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(proyecto.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;

        if (tieneDt2 && tieneDirector && tieneTribunal) {
            proyecto.setEstado("DESARROLLO");
            proyectoRepo.save(proyecto);

            documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto()).orElseGet(() -> {
                DocumentoTitulacion doc = new DocumentoTitulacion();
                doc.setProyecto(proyecto);
                doc.setEstudiante(proyecto.getPropuesta().getEstudiante());
                doc.setDirector(proyecto.getDirector());
                doc.setEstado(EstadoDocumento.BORRADOR);
                doc.setTitulo(proyecto.getTitulo());
                return documentoRepo.save(doc);
            });

            // ✅ Notificar al estudiante que su equipo quedó completo
            try {
                Estudiante est = proyecto.getPropuesta().getEstudiante();
                String emailEst = est != null && est.getUsuario() != null
                        ? est.getUsuario().getCorreoInstitucional() : null;
                if (emailEst != null && !emailEst.isBlank()) {
                    String nombreEst = fullName(est.getUsuario());
                    String periodo = proyecto.getPeriodo() != null
                            ? proyecto.getPeriodo().getDescripcion() : "";
                    String nombreDirector = proyecto.getDirector() != null
                            ? fullName(proyecto.getDirector().getUsuario()) : null;
                    String nombreDt2 = dt2AsignacionRepo
                            .findByProyecto_IdProyectoAndActivoTrue(proyecto.getIdProyecto())
                            .map(a -> fullName(a.getDocenteDt2().getUsuario()))
                            .orElse(null);
                    emailService.notificarAsignacionEquipo(
                            emailEst, nombreEst, proyecto.getTitulo(),
                            nombreDirector, nombreDt2, periodo
                    );
                }
            } catch (Exception e) {
                System.err.println("Error al notificar asignacion equipo: " + e.getMessage());
            }
        }
    }

    private void registrarBitacora(ProyectoTitulacion proyecto, String tipo, Integer idAsignado,
                                   String nombre, String cargo, Usuario realizadoPor,
                                   String periodo, String observacion) {
        BitacoraAsignacion b = new BitacoraAsignacion();
        b.setProyecto(proyecto);
        b.setTipoAsignacion(tipo);
        b.setIdAsignado(idAsignado);
        b.setNombreAsignado(nombre);
        b.setCargo(cargo);
        b.setRealizadoPor(realizadoPor);
        b.setPeriodo(periodo);
        b.setObservacion(observacion);
        bitacoraRepo.save(b);
    }

    private String fullName(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " "
                + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private void validarNota(BigDecimal nota, String campo) {
        if (nota == null || nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(BigDecimal.TEN) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La " + campo + " debe estar entre 0 y 10");
        }
    }

    private Sustentacion getPredefensa(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay predefensa programada para este proyecto"));
    }

    private Sustentacion getPredefensaSafe(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .findFirst().orElse(null);
    }

    private Sustentacion getDefensaFinal(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay sustentación final programada para este proyecto"));
    }

    private boolean predefensaAprobada(Integer idProyecto) {
        Sustentacion sus = getPredefensaSafe(idProyecto);
        if (sus == null) return false;
        Dt2Dtos.PredefensaDto dto = buildPredefensaDto(idProyecto, sus);
        return "APROBADA".equalsIgnoreCase(dto.getEstado());
    }

    private Dt2Dtos.PredefensaDto buildPredefensaDto(Integer idProyecto, Sustentacion sus) {
        Dt2Dtos.PredefensaDto dto = new Dt2Dtos.PredefensaDto();
        dto.setIdSustentacion(sus.getIdSustentacion());
        dto.setFecha(sus.getFecha());
        dto.setHora(sus.getHora());
        dto.setLugar(sus.getLugar());

        evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_DOCENTE")
                .stream().findFirst().ifPresent(e -> dto.setNotaDocenteDt2(e.getNotaFinal()));

        List<EvaluacionSustentacion> evalsTribunal = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_TRIBUNAL");

        List<Dt2Dtos.EvaluacionMiembroDto> evalsDto = evalsTribunal.stream().map(e -> {
            Dt2Dtos.EvaluacionMiembroDto m = new Dt2Dtos.EvaluacionMiembroDto();
            m.setIdDocente(e.getDocente() != null ? e.getDocente().getIdDocente() : null);
            m.setNombreDocente(e.getDocente() != null ? fullName(e.getDocente().getUsuario()) : "");
            m.setNota(e.getNotaFinal());
            m.setObservaciones(e.getObservaciones());
            return m;
        }).collect(Collectors.toList());
        dto.setEvaluacionesTribunal(evalsDto);

        long totalMiembros = tribunalRepo.countByProyecto_IdProyecto(idProyecto);
        dto.setTotalMiembrosTribunal((int) totalMiembros);
        dto.setMiembrosQueCalificaron(evalsDto.size());

        if (!evalsTribunal.isEmpty()) {
            BigDecimal sumaT = evalsTribunal.stream()
                    .map(e -> e.getNotaFinal() != null ? e.getNotaFinal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promT = sumaT.divide(BigDecimal.valueOf(evalsTribunal.size()), 2, RoundingMode.HALF_UP);
            dto.setPromedioTribunal(promT);

            if (dto.getNotaDocenteDt2() != null) {
                BigDecimal ponderada = dto.getNotaDocenteDt2().multiply(new BigDecimal("0.60"))
                        .add(promT.multiply(new BigDecimal("0.40")))
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setNotaFinalPonderada(ponderada);

                boolean todasCalificaron = evalsDto.size() >= totalMiembros;
                boolean hayCorrecciones = evalsDto.stream()
                        .anyMatch(e -> Boolean.TRUE.equals(e.getSolicitudCorrecciones()));

                if (hayCorrecciones) {
                    dto.setEstado("CON_OBSERVACIONES");
                    dto.setSolicitudCorrecciones(true);
                } else if (todasCalificaron) {
                    dto.setEstado(ponderada.compareTo(new BigDecimal("5.00")) >= 0 ? "APROBADA" : "CON_OBSERVACIONES");
                } else {
                    dto.setEstado("EN_PROCESO");
                }
            } else {
                dto.setEstado("EN_PROCESO");
            }
        } else {
            dto.setEstado(dto.getNotaDocenteDt2() != null ? "EN_PROCESO" : "PENDIENTE");
        }

        return dto;
    }

    private Dt2Dtos.ResultadoSustentacionDto buildResultadoSustentacion(
            Integer idProyecto, Sustentacion sus, ProyectoTitulacion proyecto) {

        List<EvaluacionSustentacion> evals = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "SUSTENTACION");

        List<Dt2Dtos.EvaluacionMiembroDto> evalsDto = evals.stream().map(e -> {
            Dt2Dtos.EvaluacionMiembroDto m = new Dt2Dtos.EvaluacionMiembroDto();
            m.setIdDocente(e.getDocente() != null ? e.getDocente().getIdDocente() : null);
            m.setNombreDocente(e.getDocente() != null ? fullName(e.getDocente().getUsuario()) : "");
            m.setNota(e.getNotaFinal());
            m.setObservaciones(e.getObservaciones());
            return m;
        }).collect(Collectors.toList());

        BigDecimal promedio = BigDecimal.ZERO;
        if (!evals.isEmpty()) {
            BigDecimal suma = evals.stream()
                    .map(e -> e.getNotaFinal() != null ? e.getNotaFinal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            promedio = suma.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
        }

        Dt2Dtos.ResultadoSustentacionDto dto = new Dt2Dtos.ResultadoSustentacionDto();
        dto.setIdProyecto(idProyecto);
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
        dto.setFechaSustentacion(sus.getFecha());
        dto.setPromedioTribunal(promedio);
        dto.setEvaluaciones(evalsDto);
        dto.setResultado("PENDIENTE_CONSOLIDACION");

        return dto;
    }

    private Dt2Dtos.CertificadoAntiplacioDto buildCertificadoDto(Integer idProyecto) {
        List<AntiplacioIntento> historial = antiplacioRepo
                .findByProyecto_IdProyectoOrderByFechaIntentoDesc(idProyecto);

        boolean favorable = historial.stream().anyMatch(AntiplacioIntento::getFavorable);
        AntiplacioIntento ultimo = historial.isEmpty() ? null : historial.get(0);

        List<Dt2Dtos.AntiplacioIntentoDto> historialDto = historial.stream().map(i -> {
            Dt2Dtos.AntiplacioIntentoDto d = new Dt2Dtos.AntiplacioIntentoDto();
            d.setIdIntento(i.getIdIntento());
            d.setFechaIntento(i.getFechaIntento());
            d.setPorcentajeCoincidencia(i.getPorcentajeCoincidencia());
            d.setUrlInforme(i.getUrlInforme());
            d.setFavorable(i.getFavorable());
            d.setObservaciones(i.getObservaciones());
            d.setResultado(Boolean.TRUE.equals(i.getFavorable())
                    ? "APROBADO (" + i.getPorcentajeCoincidencia() + "% < 10%)"
                    : "RECHAZADO (" + i.getPorcentajeCoincidencia() + "% >= 10%)");
            return d;
        }).collect(Collectors.toList());

        Dt2Dtos.CertificadoAntiplacioDto dto = new Dt2Dtos.CertificadoAntiplacioDto();
        dto.setIdProyecto(idProyecto);
        dto.setCertificadoFavorable(favorable);
        dto.setTotalIntentos(historial.size());
        dto.setHistorial(historialDto);
        if (ultimo != null) {
            dto.setUltimoPorcentaje(ultimo.getPorcentajeCoincidencia());
            dto.setUrlUltimoInforme(ultimo.getUrlInforme());
            dto.setFechaUltimoIntento(ultimo.getFechaIntento());
        }
        return dto;
    }

    private Dt2Dtos.AsesoriaDto mapAsesoria(AsesoriaDirector a) {
        Dt2Dtos.AsesoriaDto dto = new Dt2Dtos.AsesoriaDto();
        dto.setIdAsesoria(a.getIdAsesoria());
        dto.setFecha(a.getFecha());
        dto.setObservaciones(a.getObservaciones());
        dto.setEvidenciaUrl(a.getEvidenciaUrl());
        dto.setPorcentajeAvance(a.getPorcentajeAvance());
        dto.setNumeroCorte(a.getNumeroCorte());
        dto.setCalificacion(a.getCalificacion());
        return dto;
    }

    private Dt2Dtos.ActaCorteDto mapActaCorte(ActaCorte a) {
        Dt2Dtos.ActaCorteDto dto = new Dt2Dtos.ActaCorteDto();
        dto.setIdActaCorte(a.getIdActaCorte());
        dto.setNumeroCorte(a.getNumeroCorte());
        dto.setFechaGeneracion(a.getFechaGeneracion());
        dto.setTotalAsesorias(a.getTotalAsesorias());
        dto.setAsesoriasSuficientes(a.getAsesoriasSuficientes());
        dto.setNotaCorte(a.getNotaCorte());
        dto.setObservaciones(a.getObservaciones());
        dto.setUrlActaPdf(a.getUrlActaPdf());
        return dto;
    }

    private Dt2Dtos.DocumentosPreviosDto mapDocumentosPrevios(DocumentoPrevioSustentacion d) {
        return new Dt2Dtos.DocumentosPreviosDto(
                d.getEjemplarImpreso(), d.getCopiaDigitalBiblioteca(),
                d.getCopiasDigitalesTribunal(), d.getInformeCompilatioFirmado(),
                d.getCompleto(), d.getObservaciones());
    }
}