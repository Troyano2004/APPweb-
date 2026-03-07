package com.erwin.backend.service;

import com.erwin.backend.dtos.Dt1DetalleResponse;
import com.erwin.backend.dtos.Dt1EnviadoResponse;
import com.erwin.backend.dtos.Dt1RevisionRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

//Revision anteproyecto final

@Service
public class Dt1Service {

    private final AnteproyectoTitulacionRepository anteRepo;
    private final AnteproyectoVersionRepository verRepo;
    private final Dt1AsignacionRepository dt1AsignacionRepo;
    private final Dt1RevisionRepository revisionRepo;
    private final DocenteRepository docenteRepo;
    private final PeriodoTitulacionRepository periodoRepo;

    private final Dt1PdfService pdf;

    public Dt1Service(
            AnteproyectoTitulacionRepository anteRepo,
            AnteproyectoVersionRepository verRepo,
            Dt1AsignacionRepository dt1AsignacionRepo,
            Dt1RevisionRepository revisionRepo,
            DocenteRepository docenteRepo,
            PeriodoTitulacionRepository periodoRepo,
            Dt1PdfService pdf
    ) {
        this.anteRepo = anteRepo;
        this.verRepo = verRepo;
        this.dt1AsignacionRepo = dt1AsignacionRepo;
        this.revisionRepo = revisionRepo;
        this.docenteRepo = docenteRepo;
        this.periodoRepo = periodoRepo;
        this.pdf = pdf;
    }

    // =============================
    // ✅ ENVIADOS (EN MI CARRERA/PERIODO COMO DT1)
    // =============================
    @Transactional(readOnly = true)
    public List<Dt1EnviadoResponse> enviados(Integer idDocente) {

        if (idDocente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");
        }

        PeriodoTitulacion periodoActual = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        Integer idPeriodoActual = periodoActual.getIdPeriodo();

        List<Dt1Asignacion> asignaciones = dt1AsignacionRepo
                .findByDocente_IdDocenteAndPeriodo_IdPeriodoAndActivoTrue(idDocente, idPeriodoActual);

        if (asignaciones.isEmpty()) return List.of();

        Map<Integer, Dt1EnviadoResponse> unicos = new LinkedHashMap<>();

        for (Dt1Asignacion asg : asignaciones) {
            Integer idCarrera = asg.getCarrera().getIdCarrera();

            List<AnteproyectoTitulacion> anteproyectos = anteRepo
                    .findByCarrera_IdCarreraAndEleccion_Periodo_IdPeriodoAndEstadoInIgnoreCase(
                            idCarrera, idPeriodoActual, List.of("EN_REVISION", "ENVIADO")
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
    // ✅ DETALLE (SOLO SI SOY DT1 ASIGNADO A ESA CARRERA/PERIODO)
    // =============================
    @Transactional(readOnly = true)
    public Dt1DetalleResponse detalle(Integer idAnteproyecto, Integer idDocente) {

        if (idAnteproyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (idDocente == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        AnteproyectoTitulacion ap = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDocenteEsDt1Asignado(ap, idDocente);

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
    // ✅ REVISAR (JPA) - Solo si:
    // 1) docente es DT1 asignado a carrera+periodo del anteproyecto
    // 2) anteproyecto.estado == ENVIADO
    // 3) ultima version.estadoVersion == ENVIADO
    // =============================
    @Transactional
    public void revisar(Dt1RevisionRequest req) {

        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DATOS_REQUERIDOS");
        if (req.getIdAnteproyecto() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_ANTEPROYECTO_REQUERIDO");
        if (req.getIdDocente() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID_DOCENTE_REQUERIDO");

        String decision = safe(req.getDecision()).toUpperCase();
        if (!(decision.equals("APROBADO") || decision.equals("RECHAZADO"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DECISION_INVALIDA");
        }

        AnteproyectoTitulacion ap = anteRepo.findById(req.getIdAnteproyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_EXISTE"));

        validarDocenteEsDt1Asignado(ap, req.getIdDocente());

        // ✅ validar estado anteproyecto
        String estadoAnte = safe(ap.getEstado()).toUpperCase();
        if (estadoAnte.equals("APROBADO") || estadoAnte.equals("RECHAZADO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YA_REVISADO");
        }
        if (!estadoAnte.equals("ENVIADO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_ENVIADO");
        }

        // ✅ ultima version
        Anteproyectotitulacionversion v = verRepo
                .findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(ap.getIdAnteproyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "SIN_VERSION"));

        String estadoVer = safe(v.getEstadoVersion()).toUpperCase();
        if (!estadoVer.equals("ENVIADO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VERSION_NO_ENVIADA");
        }

        // ✅ actualizar estados
        ap.setEstado(decision);               // APROBADO / RECHAZADO
        v.setEstadoVersion(decision);         // APROBADO / RECHAZADO
        // si tu entity tiene fechaRevision como columna DB default, puedes no setearla aquí

        anteRepo.save(ap);
        verRepo.save(v);

        // ✅ guardar historial dt1_revision
        Docente docente = docenteRepo.findById(req.getIdDocente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE_NO_EXISTE"));

        Dt1Revision r = new Dt1Revision();
        r.setAnteproyecto(ap);
        r.setVersion(v);
        r.setDocente(docente);
        r.setDecision(decision);
        r.setObservacion(safe(req.getObservacion()));

        revisionRepo.save(r);
    }

    // =============================
    // ✅ PDF (SIN FN) - usando detalle() (JPA)
    // =============================
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idAnteproyecto, Integer idDocente) {

        Dt1DetalleResponse d = detalle(idAnteproyecto, idDocente);

        String html = pdf.leerHtml("dt1pdf.html");

        html = html.replace("{{ESTUDIANTE}}", pdf.seguro(d.getEstudiante()))
                .replace("{{PERIODO}}", pdf.seguro(d.getPeriodo()))
                .replace("{{ESTADO}}", pdf.seguro(d.getEstadoAnteproyecto()))
                .replace("{{VERSION}}", pdf.seguro(d.getNumeroVersion()))
                .replace("{{FECHA_ENVIO}}", pdf.seguro(d.getFechaEnvio()))
                .replace("{{ID_ANTEPROYECTO}}", pdf.seguro(d.getIdAnteproyecto()))
                .replace("{{TITULO}}", pdf.seguro(d.titulo))
                .replace("{{TEMA}}", pdf.seguro(d.temaInvestigacion))
                .replace("{{PROBLEMA}}", pdf.seguro(d.planteamientoProblema))
                .replace("{{OBJ_GEN}}", pdf.seguro(d.objetivosGenerales))
                .replace("{{OBJ_ESP}}", pdf.seguro(d.objetivosEspecificos))
                .replace("{{MARCO}}", pdf.seguro(d.marcoTeorico))
                .replace("{{METODO}}", pdf.seguro(d.metodologia))
                .replace("{{RESULTADOS}}", pdf.seguro(d.resultadosEsperados))
                .replace("{{BIBLIO}}", pdf.seguro(d.bibliografia));

        return pdf.aPdf(html);
    }

    // ==========================================================
    // ✅ VALIDACIÓN: docente debe ser DT1 asignado a carrera+periodo del anteproyecto
    // ==========================================================
    private void validarDocenteEsDt1Asignado(AnteproyectoTitulacion ante, Integer idDocente) {

        if (ante.getCarrera() == null || ante.getEleccion() == null || ante.getEleccion().getPeriodo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ANTEPROYECTO_SIN_DATOS_PARA_VALIDAR");
        }

        Integer idCarrera = ante.getCarrera().getIdCarrera();
        Integer idPeriodo = ante.getEleccion().getPeriodo().getIdPeriodo();

        boolean esDt1 = dt1AsignacionRepo.existsByDocente_IdDocenteAndCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(
                idDocente, idCarrera, idPeriodo
        );

        if (!esDt1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ES_DT1_ASIGNADO");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}