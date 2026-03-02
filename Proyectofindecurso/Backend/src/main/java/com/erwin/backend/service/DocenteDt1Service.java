package com.erwin.backend.service;

import com.erwin.backend.dtos.Dt1RevisionRequest;
import com.erwin.backend.dtos.Dt1EnviadoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocenteDt1Service {

    private final JdbcTemplate jdbc;

    public DocenteDt1Service(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Dt1EnviadoResponse> enviados(Integer idDocente) {
        return jdbc.query(
                "select * from fn_dt1_enviados(?)",
                (rs, i) -> {
                    Dt1EnviadoResponse r = new Dt1EnviadoResponse();
                    r.setIdAnteproyecto(rs.getInt("id_anteproyecto"));
                    r.setIdEstudiante(rs.getInt("id_estudiante"));
                    r.setEstudiante(rs.getString("estudiante"));
                    r.setTitulo(rs.getString("titulo"));
                    r.setEstado(rs.getString("estado"));
                    r.setVersion(rs.getInt("version"));
                    r.setFechaEnvio(rs.getDate("fecha_envio").toLocalDate());
                    return r;
                },
                idDocente
        );
    }

    @Transactional
    public void decidir(Integer idAnteproyecto, Dt1RevisionRequest req) {

        if (req == null || req.getDecision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DECISION_REQUERIDA");
        }

        String decision = req.getDecision().trim().toUpperCase();
        String obs = (req.getObservacion() == null) ? "" : req.getObservacion().trim();

        try {
            if ("APROBAR".equals(decision)) {
                jdbc.update("call sp_dt1_aprobar(?,?)", idAnteproyecto, obs);
                return;
            }
            if ("RECHAZAR".equals(decision)) {
                jdbc.update("call sp_dt1_rechazar(?,?)", idAnteproyecto, obs);
                return;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DECISION_INVALIDA");

        } catch (Exception e) {
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            if (msg.contains("SIN_VERSION_ENVIADA")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SIN_VERSION_ENVIADA");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_BD");
        }
    }
}