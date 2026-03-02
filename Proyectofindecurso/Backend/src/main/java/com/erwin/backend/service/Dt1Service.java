package com.erwin.backend.service;

import com.erwin.backend.dtos.Dt1DetalleResponse;
import com.erwin.backend.dtos.Dt1EnviadoResponse;
import com.erwin.backend.dtos.Dt1RevisionRequest;
import com.erwin.backend.entities.AnteproyectoTitulacion;
import com.erwin.backend.entities.Anteproyectotitulacionversion;
import com.erwin.backend.entities.Dt1TutorEstudiante;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.AnteproyectoTitulacionRepository;
import com.erwin.backend.repository.AnteproyectoVersionRepository;
import com.erwin.backend.repository.Dt1TutorEstudianteRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class Dt1Service {

    private final AnteproyectoTitulacionRepository anteRepo;
    private final AnteproyectoVersionRepository verRepo;
    private final Dt1TutorEstudianteRepository dt1TutorRepo; // ✅ CAMBIO REAL
    private final Dt1PdfService pdf;
    private final PeriodoTitulacionRepository periodoRepo;

    // Mantienes SP para revisar/pdf
    private final JdbcTemplate jdbc;

    public Dt1Service(
            AnteproyectoTitulacionRepository anteRepo,
            AnteproyectoVersionRepository verRepo,
            Dt1TutorEstudianteRepository dt1TutorRepo,
            JdbcTemplate jdbc,
            Dt1PdfService pdf, PeriodoTitulacionRepository periodoRepo
            ) {
        this.anteRepo = anteRepo;
        this.verRepo = verRepo;
        this.dt1TutorRepo = dt1TutorRepo;
        this.jdbc = jdbc;
        this.pdf = pdf;
        this.periodoRepo = periodoRepo;
    }

    // =============================
    // ✅ ENVIADOS (SOLO MIS TUTORADOS)
    // =============================
    @Transactional(readOnly = true)
    public List<Dt1EnviadoResponse> enviados(Integer idDocente) {

        if (idDocente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");
        }

        PeriodoTitulacion periodoActual = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        Integer idPeriodoActual = periodoActual.getIdPeriodo();

        // SOLO tutorados del PERIODO ACTUAL
        List<Dt1TutorEstudiante> tutorados =
                dt1TutorRepo.findByDocente_IdDocenteAndPeriodo_IdPeriodoAndActivoTrue(idDocente, idPeriodoActual);

        if (tutorados.isEmpty()) return List.of();

        Map<Integer, Dt1EnviadoResponse> unicos = new LinkedHashMap<>();

        for (Dt1TutorEstudiante te : tutorados) {

            Integer idEstudiante = te.getEstudiante().getIdEstudiante();

            List<AnteproyectoTitulacion> anteproyectos =
                    anteRepo.findByEstudiante_IdEstudianteAndEleccion_Periodo_IdPeriodoAndEstadoIgnoreCase(
                            idEstudiante, idPeriodoActual, "ENVIADO"
                    );

            for (AnteproyectoTitulacion ap : anteproyectos) {

                Optional<Anteproyectotitulacionversion> ov =
                        verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(ap.getIdAnteproyecto());

                if (ov.isEmpty()) continue;

                Anteproyectotitulacionversion v = ov.get();
                if (!"ENVIADO".equalsIgnoreCase(safe(v.getEstadoVersion()))) continue;

                Dt1EnviadoResponse dto = new Dt1EnviadoResponse();
                dto.setIdAnteproyecto(ap.getIdAnteproyecto());
                dto.setIdEstudiante(ap.getEstudiante().getIdEstudiante());

                String nombres = safe(ap.getEstudiante().getUsuario().getNombres());
                String apellidos = safe(ap.getEstudiante().getUsuario().getApellidos());
                dto.setEstudiante((nombres + " " + apellidos).trim());

                dto.setTitulo(safe(ap.getPropuesta().getTitulo()));
                dto.setEstado(safe(ap.getEstado()));
                dto.setVersion(v.getNumeroVersion());
                dto.setFechaEnvio(v.getFechaEnvio());

                unicos.put(ap.getIdAnteproyecto(), dto);
            }
        }

        List<Dt1EnviadoResponse> lista = new ArrayList<>(unicos.values());
        lista.sort(Comparator.comparing(
                Dt1EnviadoResponse::getFechaEnvio,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return lista;
    }

    // =============================
    // ✅ DETALLE (SOLO SI SOY TUTOR DEL ESTUDIANTE)
    // =============================
    @Transactional(readOnly = true)
    public Dt1DetalleResponse detalle(Integer idAnteproyecto, Integer idDocente) {

        if (idAnteproyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (idDocente == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        AnteproyectoTitulacion ap = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDocenteEsTutorDelAnteproyecto(ap, idDocente);

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

        String nombres = safe(ap.getEstudiante().getUsuario().getNombres());
        String apellidos = safe(ap.getEstudiante().getUsuario().getApellidos());
        d.setEstudiante((nombres + " " + apellidos).trim());

        d.setPeriodo(safe(ap.getEleccion().getPeriodo().getDescripcion()));

        d.titulo = safe(v.getTitulo());
        d.temaInvestigacion = safe(v.getTemaInvestigacion());
        d.planteamientoProblema = safe(v.getPlanteamientoProblema());
        d.objetivosGenerales = safe(v.getObjetivosGenerales());
        d.objetivosEspecificos = safe(v.getObjetivosEspecificos());
        d.marcoTeorico = safe(v.getMarcoTeorico());
        d.metodologia = safe(v.getMetodologia());
        d.resultadosEsperados = safe(v.getResultadosEsperados());
        d.bibliografia = safe(v.getBibliografia());

        return d;
    }

    // =============================
    // ✅ REVISAR (SP) - SOLO SI SOY TUTOR
    // =============================
    @Transactional
    public void revisar(Dt1RevisionRequest req) {

        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        if (req.getIdAnteproyecto() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (req.getIdDocente() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");
        if (safe(req.getDecision()).isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DECISION_REQUERIDA");

        AnteproyectoTitulacion ap = anteRepo.findById(req.getIdAnteproyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDocenteEsTutorDelAnteproyecto(ap, req.getIdDocente());

        jdbc.update(
                "call sp_dt1_revisar(?,?,?,?)",
                req.getIdAnteproyecto(),
                req.getIdDocente(),
                req.getDecision(),
                req.getObservacion()
        );
    }

    // =============================
    // ✅ PDF (SP) - SOLO SI SOY TUTOR
    // =============================
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idAnteproyecto, Integer idDocente) {

        if (idAnteproyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (idDocente == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        AnteproyectoTitulacion ap = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDocenteEsTutorDelAnteproyecto(ap, idDocente);

        Map<String, Object> d;
        try {
            d = jdbc.queryForMap("select * from fn_dt1_detalle(?,?)", idAnteproyecto, idDocente);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE");
        }

        String html = pdf.leerHtml("dt1pdf.html");

        html = html.replace("{{ESTUDIANTE}}", pdf.seguro(d.get("estudiante")))
                .replace("{{PERIODO}}", pdf.seguro(d.get("periodo")))
                .replace("{{ESTADO}}", pdf.seguro(d.get("estado_anteproyecto")))
                .replace("{{VERSION}}", pdf.seguro(d.get("numero_version")))
                .replace("{{FECHA_ENVIO}}", pdf.seguro(d.get("fecha_envio")))
                .replace("{{ID_ANTEPROYECTO}}", pdf.seguro(d.get("id_anteproyecto")))
                .replace("{{TITULO}}", pdf.seguro(d.get("titulo")))
                .replace("{{TEMA}}", pdf.seguro(d.get("tema_investigacion")))
                .replace("{{PROBLEMA}}", pdf.seguro(d.get("planteamiento_problema")))
                .replace("{{OBJ_GEN}}", pdf.seguro(d.get("objetivos_generales")))
                .replace("{{OBJ_ESP}}", pdf.seguro(d.get("objetivos_especificos")))
                .replace("{{MARCO}}", pdf.seguro(d.get("marco_teorico")))
                .replace("{{METODO}}", pdf.seguro(d.get("metodologia")))
                .replace("{{RESULTADOS}}", pdf.seguro(d.get("resultados_esperados")))
                .replace("{{BIBLIO}}", pdf.seguro(d.get("bibliografia")));

        return pdf.aPdf(html);
    }

    // ==========================================================
    // ✅ VALIDACIÓN CENTRAL
    // ==========================================================
    private void validarDocenteEsTutorDelAnteproyecto(AnteproyectoTitulacion ante, Integer idDocente) {

        if (ante.getEstudiante() == null || ante.getEleccion() == null || ante.getEleccion().getPeriodo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANTEPROYECTO_SIN_DATOS_PARA_VALIDAR");
        }

        Integer idEstudiante = ante.getEstudiante().getIdEstudiante();
        Integer idPeriodo = ante.getEleccion().getPeriodo().getIdPeriodo();

        boolean esTutor = dt1TutorRepo.existsByDocente_IdDocenteAndEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                idDocente, idEstudiante, idPeriodo
        );

        if (!esTutor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ES_TUTOR_DEL_ESTUDIANTE");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}