package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AnteproyectoService {

    private final AnteproyectoVersionRepository verRepo;
    private final PropuestaRepository propRepo;
    private final AnteproyectoTitulacionRepository anteRepo;

    public AnteproyectoService(AnteproyectoVersionRepository verRepo,
                               PropuestaRepository propRepo,
                               AnteproyectoTitulacionRepository anteRepo) {
        this.verRepo = verRepo;
        this.propRepo = propRepo;
        this.anteRepo = anteRepo;
    }

    /**
     * Equivalente a fn_mi_anteproyecto(idEstudiante)
     * - Busca propuesta aprobada
     * - Busca o crea anteproyecto
     * - Retorna snapshot de propuesta + ultimaVersion (si existe) o plantilla
     */
    @Transactional
    public AnteproyectoResponse cargarMiAnteproyecto(Integer idEstudiante) {

        PropuestaTitulacion p = propRepo
                .findFirstByEstudiante_IdEstudianteAndEstadoInOrderByIdPropuestaAsc(
                        idEstudiante,
                        List.of("APROBADO", "APROBADA")
                )
                .orElse(null);

        AnteproyectoResponse resp = new AnteproyectoResponse();

        if (p == null) {
            resp.setEstado("NO_DISPONIBLE");
            resp.setIdEstudiante(idEstudiante);
            resp.setMensaje("Aún no tienes una propuesta APROBADA. Cuando esté aprobada podrás editar el anteproyecto.");
            return resp;
        }

        // Buscar o crear anteproyecto
        AnteproyectoTitulacion a = anteRepo.findByPropuesta_IdPropuesta(p.getIdPropuesta())
                .orElseGet(() -> {
                    AnteproyectoTitulacion nuevo = new AnteproyectoTitulacion();
                    nuevo.setPropuesta(p);
                    nuevo.setEleccion(p.getEleccion());
                    nuevo.setEstudiante(p.getEstudiante());
                    nuevo.setCarrera(p.getCarrera());
                    nuevo.setEstado("BORRADOR");
                    return anteRepo.save(nuevo);
                });

        resp.setIdAnteproyecto(a.getIdAnteproyecto());
        resp.setEstado(a.getEstado());
        resp.setIdEstudiante(a.getEstudiante().getIdEstudiante());

        // Nombre del estudiante (si la relación existe)
        try {
            Usuario u = a.getEstudiante().getUsuario();
            if (u != null) {
                resp.setNombresEstudiante(n(u.getNombres()));
                resp.setApellidosEstudiante(n(u.getApellidos()));
            }
        } catch (Exception ignored) {}

        // Snapshot de propuesta
        AnteproyectoResponse.PropuestaSnapshot snap = new AnteproyectoResponse.PropuestaSnapshot();
        snap.idPropuesta = p.getIdPropuesta();

        if (p.getTema() != null) {
            snap.idTema = p.getTema().getIdTema();
            snap.tituloTema = n(p.getTema().getTitulo());
        }

        // OJO: aquí usa los getters reales de tu entidad (yo dejé los de tu ejemplo)
        snap.titulo = n(p.getTitulo());

        snap.planteamientoProblema = n(p.getPlanteamientoProblema());
        snap.objetivoGeneral = n(p.getObjetivosGenerales());
        snap.objetivosEspecificos = n(p.getObjetivosEspecificos());
        snap.metodologia = n(p.getMetodologia());
        snap.bibliografia = n(p.getBibliografia());

        resp.setPropuesta(snap);

        // Última versión (equivalente a fn_ultima_version)
        var lastOpt = verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(a.getIdAnteproyecto());

        if (lastOpt.isPresent()) {
            resp.setUltimaVersion(toDto(lastOpt.get()));
            return resp;
        }

        // Plantilla si no hay versiones
        AnteproyectoVersionResponse plantilla = new AnteproyectoVersionResponse();
        plantilla.setNumeroVersion(0);
        plantilla.setEstadoVersion("PLANTILLA");
        plantilla.setTitulo(n(snap.titulo));
        plantilla.setTemaInvestigacion(n(snap.temaInvestigacion));
        plantilla.setPlanteamientoProblema(n(snap.planteamientoProblema));
        plantilla.setObjetivosGenerales(n(snap.objetivoGeneral));
        plantilla.setObjetivosEspecificos(n(snap.objetivosEspecificos));
        plantilla.setMarcoTeorico("");
        plantilla.setMetodologia(n(snap.metodologia));
        plantilla.setResultadosEsperados("");
        plantilla.setBibliografia(n(snap.bibliografia));

        resp.setUltimaVersion(plantilla);
        resp.setMensaje("Se precargaron campos desde tu propuesta aprobada. Completa y guarda el anteproyecto.");
        return resp;
    }

    public List<AnteproyectoVersionResponse> versiones(Integer idAnteproyecto) {
        return verRepo.findByAnteproyecto_IdAnteproyectoOrderByNumeroVersionAsc(idAnteproyecto)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public AnteproyectoVersionResponse ultimaVersion(Integer idAnteproyecto) {
        Anteproyectotitulacionversion v = verRepo
                .findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIN_VERSIONES"));
        return toDto(v);
    }

    /**
     * Equivalente a sp_guardar_borrador(...)
     * - valida bloqueado
     * - crea nueva version BORRADOR
     * - actualiza estado anteproyecto BORRADOR
     */
    @Auditable(entidad = "AnteproyectoVersion", accion = "BORRADOR", capturarArgs = false)
    @Transactional
    public AnteproyectoVersionResponse guardarBorrador(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        AnteproyectoTitulacion a = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANTEPROYECTO_NO_EXISTE"));

        if (estaBloqueado(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ANTEPROYECTO_BLOQUEADO");
        }

        Anteproyectotitulacionversion v = crearVersion(a, req);
        v.setEstadoVersion("BORRADOR");

        // estado anteproyecto
        a.setEstado("BORRADOR");
        anteRepo.save(a);

        return toDto(verRepo.save(v));
    }

    /**
     * Equivalente a sp_enviar_revision(...)
     * - valida bloqueado
     * - crea nueva version ENVIADO
     * - actualiza estado anteproyecto EN_REVISION
     */
    @Auditable(entidad = "AnteproyectoVersion", accion = "ENVIAR_REVISION", capturarArgs = false)
    @Transactional
    public AnteproyectoVersionResponse enviarRevision(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        AnteproyectoTitulacion a = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANTEPROYECTO_NO_EXISTE"));

        if (estaBloqueado(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ANTEPROYECTO_BLOQUEADO");
        }

        Anteproyectotitulacionversion v = crearVersion(a, req);
        v.setEstadoVersion("ENVIADO");
        Anteproyectotitulacionversion guardada = verRepo.save(v);

        a.setEstado("EN_REVISION");
        anteRepo.save(a);

        return toDto(guardada);
    }

    // ===================== helpers =====================

    private boolean estaBloqueado(String estado) {
        if (estado == null) return false;
        String e = estado.toUpperCase();
        // Ajusta según tu regla real
        return e.equals("APROBADA") || e.equals("RECHAZADA") || e.equals("EN_REVISION");
    }

    private Anteproyectotitulacionversion crearVersion(AnteproyectoTitulacion a, AnteproyectoVersionRequest req) {
        int next = verRepo.maxNumeroVersion(a.getIdAnteproyecto()) + 1;

        Anteproyectotitulacionversion v = new Anteproyectotitulacionversion();
        v.setAnteproyecto(a);
        v.setNumeroVersion(next);

        v.setTitulo(n(req.titulo));
        v.setTemaInvestigacion(n(req.temaInvestigacion));
        v.setPlanteamientoProblema(n(req.planteamientoProblema));
        v.setObjetivosGenerales(n(req.objetivosGenerales));
        v.setObjetivosEspecificos(n(req.objetivosEspecificos));
        v.setMarcoTeorico(n(req.marcoTeorico));
        v.setMetodologia(n(req.metodologia));
        v.setResultadosEsperados(n(req.resultadosEsperados));
        v.setBibliografia(n(req.bibliografia));
        v.setComentarioCambio(n(req.comentarioCambio));

        // si manejas fecha en entidad:
        // v.setFechaEnvio(LocalDate.now());

        return v;
    }

    private AnteproyectoVersionResponse toDto(Anteproyectotitulacionversion v) {
        AnteproyectoVersionResponse r = new AnteproyectoVersionResponse();
        r.setIdVersion(v.getIdVersion());
        r.setNumeroVersion(v.getNumeroVersion());
        r.setEstadoVersion(v.getEstadoVersion());
        r.setFechaCreacion(v.getFechaEnvio());   // ajusta si tu campo se llama distinto
        r.setComentarioCambio(v.getComentarioCambio());

        r.setTitulo(v.getTitulo());
        r.setTemaInvestigacion(v.getTemaInvestigacion());
        r.setPlanteamientoProblema(v.getPlanteamientoProblema());
        r.setObjetivosGenerales(v.getObjetivosGenerales());
        r.setObjetivosEspecificos(v.getObjetivosEspecificos());
        r.setMarcoTeorico(v.getMarcoTeorico());
        r.setMetodologia(v.getMetodologia());
        r.setResultadosEsperados(v.getResultadosEsperados());
        r.setBibliografia(v.getBibliografia());
        return r;
    }

    private String n(String s) { return (s == null) ? "" : s; }
}