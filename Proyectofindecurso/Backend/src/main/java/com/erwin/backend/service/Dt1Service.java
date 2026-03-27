package com.erwin.backend.service;

import com.erwin.backend.dtos.Dt1DetalleResponse;
import com.erwin.backend.dtos.Dt1EnviadoResponse;
import com.erwin.backend.dtos.Dt1RevisionRequest;
import com.erwin.backend.dtos.Dt1UltimaRevisionResponse;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class Dt1Service {

    private final AnteproyectoTitulacionRepository anteRepo;
    private final AnteproyectoVersionRepository    verRepo;
    private final Dt1AsignacionRepository          dt1AsignacionRepo;
    private final PeriodoTitulacionRepository      periodoRepo;
    private final Dt1PdfService                    pdf;
    private final JdbcTemplate                     jdbcTemplate;
    private final Dt1RevisionRepository           dt1RevisionRepo;
    private final IaEjemploRepository              iaEjemploRepo;

    // revisionRepo y docenteRepo eliminados: el SP maneja el INSERT en dt1_revision

    public Dt1Service(
            AnteproyectoTitulacionRepository anteRepo,
            AnteproyectoVersionRepository    verRepo,
            Dt1AsignacionRepository          dt1AsignacionRepo,
            PeriodoTitulacionRepository      periodoRepo,
            Dt1PdfService                    pdf,
            JdbcTemplate                     jdbcTemplate,
            Dt1RevisionRepository           dt1RevisionRepo,
            IaEjemploRepository             iaEjemploRepo
    ) {
        this.anteRepo          = anteRepo;
        this.verRepo           = verRepo;
        this.dt1AsignacionRepo = dt1AsignacionRepo;
        this.periodoRepo       = periodoRepo;
        this.pdf               = pdf;
        this.jdbcTemplate      = jdbcTemplate;
        this.dt1RevisionRepo   = dt1RevisionRepo;
        this.iaEjemploRepo  = iaEjemploRepo;
    }

    // =========================================================
    // LISTA: anteproyectos EN_REVISION/ENVIADO de mi carrera
    // =========================================================
    @Transactional(readOnly = true)
    public List<Dt1EnviadoResponse> enviados(Integer idDocente) {

        if (idDocente == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        Integer idPeriodo = periodo.getIdPeriodo();

        List<Dt1Asignacion> asignaciones = dt1AsignacionRepo
                .findByDocente_IdDocenteAndPeriodo_IdPeriodoAndActivoTrue(idDocente, idPeriodo);

        if (asignaciones.isEmpty()) return List.of();

        Map<Integer, Dt1EnviadoResponse> unicos = new LinkedHashMap<>();

        for (Dt1Asignacion asg : asignaciones) {

            Integer idCarrera = asg.getCarrera().getIdCarrera();

            List<AnteproyectoTitulacion> lista = anteRepo
                    .findByCarrera_IdCarreraAndEleccion_Periodo_IdPeriodoAndEstadoInIgnoreCase(
                            idCarrera, idPeriodo, List.of("EN_REVISION", "ENVIADO")
                    );

            for (AnteproyectoTitulacion ap : lista) {

                Optional<Anteproyectotitulacionversion> ov =
                        verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(
                                ap.getIdAnteproyecto());

                if (ov.isEmpty()) continue;

                Anteproyectotitulacionversion v = ov.get();
                if (!"ENVIADO".equalsIgnoreCase(safe(v.getEstadoVersion()))) continue;

                Dt1EnviadoResponse dto = new Dt1EnviadoResponse();
                dto.setIdAnteproyecto(ap.getIdAnteproyecto());
                dto.setIdEstudiante(ap.getEstudiante().getIdEstudiante());

                String nombres   = safe(ap.getEstudiante().getUsuario().getNombres());
                String apellidos = safe(ap.getEstudiante().getUsuario().getApellidos());
                dto.setEstudiante((nombres + " " + apellidos).trim());

                dto.setTitulo(safe(ap.getPropuesta().getTitulo()));
                dto.setEstado(safe(ap.getEstado()));
                dto.setVersion(v.getNumeroVersion());
                dto.setFechaEnvio(v.getFechaEnvio());

                unicos.put(ap.getIdAnteproyecto(), dto);
            }
        }

        List<Dt1EnviadoResponse> result = new ArrayList<>(unicos.values());
        result.sort(Comparator.comparing(
                Dt1EnviadoResponse::getFechaEnvio,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    // =========================================================
    // DETALLE: solo si soy DT1 asignado a carrera+periodo
    // =========================================================
    @Transactional(readOnly = true)
    public Dt1DetalleResponse detalle(Integer idAnteproyecto, Integer idDocente) {

        if (idAnteproyecto == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (idDocente == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        AnteproyectoTitulacion ap = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDt1(ap, idDocente);

        Anteproyectotitulacionversion v = verRepo
                .findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "SIN_VERSION"));

        Dt1DetalleResponse d = new Dt1DetalleResponse();
        d.setIdAnteproyecto(ap.getIdAnteproyecto());
        d.setEstadoAnteproyecto(safe(ap.getEstado()));
        d.setIdVersion(v.getIdVersion());
        d.setNumeroVersion(v.getNumeroVersion());
        d.setEstadoVersion(safe(v.getEstadoVersion()));
        d.setFechaEnvio(v.getFechaEnvio());

        String nombres   = safe(ap.getEstudiante().getUsuario().getNombres());
        String apellidos = safe(ap.getEstudiante().getUsuario().getApellidos());
        d.setEstudiante((nombres + " " + apellidos).trim());
        d.setPeriodo(safe(ap.getEleccion().getPeriodo().getDescripcion()));

        d.titulo                = safe(v.getTitulo());
        d.temaInvestigacion     = safe(v.getTemaInvestigacion());
        d.planteamientoProblema = safe(v.getPlanteamientoProblema());
        d.objetivosGenerales    = safe(v.getObjetivosGenerales());
        d.objetivosEspecificos  = safe(v.getObjetivosEspecificos());
        d.marcoTeorico          = safe(v.getMarcoTeorico());
        d.metodologia           = safe(v.getMetodologia());
        d.resultadosEsperados   = safe(v.getResultadosEsperados());
        d.bibliografia          = safe(v.getBibliografia());

        return d;
    }

    // =========================================================
    // REVISAR: usa sp_revisar_anteproyecto_dt1
    //
    // FLUJO DE ESTADOS:
    //   APROBADO  → anteproyecto=APROBADO,  version=APROBADO  (bloqueado definitivamente)
    //   RECHAZADO → anteproyecto=BORRADOR,  version=RECHAZADO (estudiante corrige y reenvía)
    // =========================================================
    @Transactional
    public void revisar(Dt1RevisionRequest req) {

        if (req == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        if (req.getIdAnteproyecto() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (req.getIdDocente() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT * FROM public.sp_revisar_anteproyecto_dt1(?, ?, ?, ?)",
                req.getIdAnteproyecto(),
                req.getIdDocente(),
                safe(req.getDecision()).toUpperCase(),
                req.getObservacion()
        );

        int    codigo = toInt(result.get("p_codigo"));
        String msg    = safe(result.get("p_mensaje"));

        if (codigo != 1) {
            HttpStatus status = switch (msg) {
                case "NO_EXISTE"          -> HttpStatus.NOT_FOUND;
                case "NO_ES_DT1_ASIGNADO" -> HttpStatus.FORBIDDEN;
                default                   -> HttpStatus.BAD_REQUEST;
            };
            throw new ResponseStatusException(status, msg);
        }
        try{
            IaEjemplo ejemplo = new IaEjemplo();
            AnteproyectoTitulacion ant = anteRepo.findById(req.getIdAnteproyecto()).orElse(null);
            if(ant != null)
            {
                Anteproyectotitulacionversion v = verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(ant.getIdAnteproyecto()).orElse(null);
                if(v != null)
                {
                    ejemplo.setIdEstudiante(ant.getEstudiante().getIdEstudiante());
                    ejemplo.setSeccion("revision_final");
                    ejemplo.setObservacion(req.getObservacion());
                    ejemplo.setFuente("REVISION_FINAL");
                    ejemplo.setDecision(safe(req.getDecision()).toUpperCase());
                    ejemplo.setContenido(  "TÍTULO: " + safe(v.getTitulo()) +
                            "\nOBJETIVO GENERAL: " + safe(v.getObjetivosGenerales()) +
                            "\nPROBLEMA: " + safe(v.getPlanteamientoProblema()) +
                            "\nMETODOLOGÍA: " + safe(v.getMetodologia()));

                }
                iaEjemploRepo.save(ejemplo);
            }


        }catch (Exception ignored){

        }
    }
    @Transactional(readOnly = true)
    public Dt1UltimaRevisionResponse ultimaRevision(Integer idAnteproyecto) {
        return dt1RevisionRepo
                .findTopByAnteproyecto_IdAnteproyectoOrderByFechaRevisionDesc(idAnteproyecto)
                .map(r ->
                        new Dt1UltimaRevisionResponse(
                        r.getDecision(),
                        r.getObservacion(),
                        r.getFechaRevision()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIN REVISION"));
    }

    // =========================================================
    // PDF
    // =========================================================
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idAnteproyecto, Integer idDocente) {

        Dt1DetalleResponse d = detalle(idAnteproyecto, idDocente);

        String html = pdf.leerHtml("dt1pdf.html");


        html = html
                .replace("{{ESTUDIANTE}}",      pdf.seguro(d.getEstudiante()))
                .replace("{{PERIODO}}",          pdf.seguro(d.getPeriodo()))
                .replace("{{ESTADO}}",           pdf.seguro(d.getEstadoAnteproyecto()))
                .replace("{{VERSION}}",          pdf.seguro(d.getNumeroVersion()))
                .replace("{{FECHA_ENVIO}}",      pdf.seguro(d.getFechaEnvio()))
                .replace("{{ID_ANTEPROYECTO}}", pdf.seguro(d.getIdAnteproyecto()))
                .replace("{{TITULO}}",           pdf.seguro(d.titulo))
                .replace("{{TEMA}}",             pdf.seguro(d.temaInvestigacion))
                .replace("{{PROBLEMA}}",         pdf.seguro(d.planteamientoProblema))
                .replace("{{OBJ_GEN}}",          pdf.seguro(d.objetivosGenerales))
                .replace("{{OBJ_ESP}}",          pdf.seguro(d.objetivosEspecificos))
                .replace("{{MARCO}}",            pdf.seguro(d.marcoTeorico))
                .replace("{{METODO}}",           pdf.seguro(d.metodologia))
                .replace("{{RESULTADOS}}",       pdf.seguro(d.resultadosEsperados))
                .replace("{{BIBLIO}}",           pdf.seguro(d.bibliografia));

        return pdf.aPdf(html);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Valida que el docente sea DT1 activo asignado a la carrera y período del anteproyecto.
     * Lanza 403 si no cumple.
     */
    private void validarDt1(AnteproyectoTitulacion ap, Integer idDocente) {

        if (ap.getCarrera() == null
                || ap.getEleccion() == null
                || ap.getEleccion().getPeriodo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ANTEPROYECTO_SIN_DATOS_PARA_VALIDAR");
        }

        Integer idCarrera = ap.getCarrera().getIdCarrera();
        Integer idPeriodo = ap.getEleccion().getPeriodo().getIdPeriodo();

        boolean esDt1 = dt1AsignacionRepo
                .existsByDocente_IdDocenteAndCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(
                        idDocente, idCarrera, idPeriodo);

        if (!esDt1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ES_DT1_ASIGNADO");
    }

    private int toInt(Object val) {
        if (val == null) return -1;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return -1; }
    }

    private String safe(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}