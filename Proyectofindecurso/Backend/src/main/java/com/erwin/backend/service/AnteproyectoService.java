package com.erwin.backend.service;

import com.erwin.backend.dtos.AnteproyectoResponse;
import com.erwin.backend.dtos.AnteproyectoVersionRequest;
import com.erwin.backend.dtos.AnteproyectoVersionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
public class AnteproyectoService {

    private final JdbcTemplate jdbc;

    public AnteproyectoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Convierte filas (ResultSet) en AnteproyectoVersionResponse
    private final RowMapper<AnteproyectoVersionResponse> mapearVersion = (rs, numeroFila) -> {
        AnteproyectoVersionResponse r = new AnteproyectoVersionResponse();
        r.setIdVersion(rs.getInt("id_version"));
        r.setNumeroVersion(rs.getInt("numero_version"));
        r.setEstadoVersion(rs.getString("estado_version"));

        Date fecha = rs.getDate("fecha_envio");
        r.setFechaCreacion(fecha != null ? fecha.toLocalDate() : null);

        r.setComentarioCambio(rs.getString("comentario_cambio"));
        r.setTitulo(rs.getString("titulo"));
        r.setTemaInvestigacion(rs.getString("tema_investigacion"));
        r.setPlanteamientoProblema(rs.getString("planteamiento_problema"));
        r.setObjetivosGenerales(rs.getString("objetivos_generales"));
        r.setObjetivosEspecificos(rs.getString("objetivos_especificos"));
        r.setMarcoTeorico(rs.getString("marco_teorico"));
        r.setMetodologia(rs.getString("metodologia"));
        r.setResultadosEsperados(rs.getString("resultados_esperados"));
        r.setBibliografia(rs.getString("bibliografia"));
        return r;
    };

    // ==========================================================
    // ✅ 1) Controller llama: service.cargarMiAnteproyecto(id)
    // ==========================================================
    public AnteproyectoResponse cargarMiAnteproyecto(Integer idEstudiante) {

        List<Map<String, Object>> filas = jdbc.queryForList(
                "select * from fn_mi_anteproyecto(?)",
                idEstudiante
        );

        if (filas.isEmpty()) {
            AnteproyectoResponse resp = new AnteproyectoResponse();
            resp.setEstado("NO_DISPONIBLE");
            resp.setIdEstudiante(idEstudiante);
            resp.setMensaje("No disponible.");
            return resp;
        }

        Map<String, Object> fila = filas.get(0);

        AnteproyectoResponse resp = new AnteproyectoResponse();
        resp.setIdAnteproyecto((Integer) fila.get("id_anteproyecto"));
        resp.setEstado((String) fila.get("estado"));
        resp.setIdEstudiante((Integer) fila.get("id_estudiante"));
        resp.setNombresEstudiante((String) fila.get("nombres_estudiante"));
        resp.setApellidosEstudiante((String) fila.get("apellidos_estudiante"));

        String mensaje = (String) fila.get("mensaje");
        if (mensaje != null && !"OK".equalsIgnoreCase(mensaje)) {
            resp.setMensaje(mensaje);
        }

        // snapshot propuesta
        AnteproyectoResponse.PropuestaSnapshot snap = new AnteproyectoResponse.PropuestaSnapshot();
        snap.idPropuesta = (Integer) fila.get("id_propuesta");
        snap.idTema = (Integer) fila.get("id_tema");
        snap.tituloTema = (String) fila.get("titulo_tema");

        snap.titulo = (String) fila.get("titulo");
        snap.temaInvestigacion = (String) fila.get("tema_investigacion");
        snap.planteamientoProblema = (String) fila.get("planteamiento_problema");
        snap.objetivoGeneral = (String) fila.get("objetivo_general");
        snap.objetivosEspecificos = (String) fila.get("objetivos_especificos");
        snap.metodologia = (String) fila.get("metodologia");
        snap.bibliografia = (String) fila.get("bibliografia");

        resp.setPropuesta(snap);

        // cargar última versión (si existe)
        Integer idAnte = resp.getIdAnteproyecto();
        if (idAnte != null) {
            List<AnteproyectoVersionResponse> ult = jdbc.query(
                    "select * from fn_ultima_version(?)",
                    mapearVersion,
                    idAnte
            );

            if (!ult.isEmpty()) {
                resp.setUltimaVersion(ult.get(0));
            } else {
                // plantilla (como tu lógica anterior)
                AnteproyectoVersionResponse plantilla = new AnteproyectoVersionResponse();
                plantilla.setNumeroVersion(0);
                plantilla.setEstadoVersion("PLANTILLA");
                plantilla.setTitulo(textoSeguro(snap.titulo));
                plantilla.setTemaInvestigacion(textoSeguro(snap.temaInvestigacion));
                plantilla.setPlanteamientoProblema(textoSeguro(snap.planteamientoProblema));
                plantilla.setObjetivosGenerales(textoSeguro(snap.objetivoGeneral));
                plantilla.setObjetivosEspecificos(textoSeguro(snap.objetivosEspecificos));
                plantilla.setMarcoTeorico("");
                plantilla.setMetodologia(textoSeguro(snap.metodologia));
                plantilla.setResultadosEsperados("");
                plantilla.setBibliografia(textoSeguro(snap.bibliografia));

                resp.setUltimaVersion(plantilla);

                if (resp.getMensaje() == null) {
                    resp.setMensaje("Se precargaron campos desde tu propuesta aprobada. Completa y guarda el anteproyecto.");
                }
            }
        }

        return resp;
    }

    // ==========================================================
    // ✅ 2) Controller llama: service.versiones(idAnteproyecto)
    // ==========================================================
    public List<AnteproyectoVersionResponse> versiones(Integer idAnteproyecto) {
        return jdbc.query(
                "select * from fn_listar_versiones(?)",
                mapearVersion,
                idAnteproyecto
        );
    }

    // ==========================================================
    // ✅ 3) Controller llama: service.ultimaVersion(idAnteproyecto)
    // ==========================================================
    public AnteproyectoVersionResponse ultimaVersion(Integer idAnteproyecto) {
        List<AnteproyectoVersionResponse> lista = jdbc.query(
                "select * from fn_ultima_version(?)",
                mapearVersion,
                idAnteproyecto
        );

        if (lista.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SIN_VERSIONES");
        }

        return lista.get(0);
    }

    // ==========================================================
    // ✅ 4) Controller llama: service.guardarBorrador(id, req)
    // ==========================================================
    @Transactional
    public AnteproyectoVersionResponse guardarBorrador(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        jdbc.update(
                "call sp_guardar_borrador(?,?,?,?,?,?,?,?,?,?,?)",
                idAnteproyecto,
                req.titulo,
                req.temaInvestigacion,
                req.planteamientoProblema,
                req.objetivosGenerales,
                req.objetivosEspecificos,
                req.marcoTeorico,
                req.metodologia,
                req.resultadosEsperados,
                req.bibliografia,
                req.comentarioCambio
        );

        return ultimaVersion(idAnteproyecto);
    }

    // ==========================================================
    // ✅ 5) Controller llama: service.enviarRevision(id, req)
    // ==========================================================
    @Transactional
    public AnteproyectoVersionResponse enviarRevision(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        jdbc.update(
                "call sp_enviar_revision(?,?,?,?,?,?,?,?,?,?,?)",
                idAnteproyecto,
                req.titulo,
                req.temaInvestigacion,
                req.planteamientoProblema,
                req.objetivosGenerales,
                req.objetivosEspecificos,
                req.marcoTeorico,
                req.metodologia,
                req.resultadosEsperados,
                req.bibliografia,
                req.comentarioCambio
        );

        return ultimaVersion(idAnteproyecto);
    }

    private String textoSeguro(String s) {
        return (s == null) ? "" : s;
    }
}