package com.erwin.backend.service;


import com.erwin.backend.dtos.TutoriaHistorialResponse;
import com.erwin.backend.entities.TutoriaAnteproyecto;
import com.erwin.backend.repository.ActaRevisionDirectorRepository;
import com.erwin.backend.repository.AnteproyectoTitulacionRepository;
import com.erwin.backend.repository.TutoriaAnteproyectoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TutoriaEstudianteService {

    private final AnteproyectoTitulacionRepository anteRepo;
    private final TutoriaAnteproyectoRepository tutRepo;
    private final ActaRevisionDirectorRepository actaRepo;

    public TutoriaEstudianteService(
            AnteproyectoTitulacionRepository anteRepo,
            TutoriaAnteproyectoRepository tutRepo,
            ActaRevisionDirectorRepository actaRepo
    ) {
        this.anteRepo = anteRepo;
        this.tutRepo = tutRepo;
        this.actaRepo = actaRepo;
    }

    public List<TutoriaHistorialResponse> historial(Integer idEstudiante, Integer idAnteproyecto) {

        var ante = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANTEPROYECTO_NO_EXISTE"));

        // seguridad: ese anteproyecto debe pertenecer al estudiante
        if (!ante.getEstudiante().getIdEstudiante().equals(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_PERMITIDO");
        }

        return tutRepo.findByAnteproyecto_IdAnteproyectoOrderByFechaAsc(idAnteproyecto)
                .stream()
                .map(this::mapHistorial)
                .toList();
    }

    private TutoriaHistorialResponse mapHistorial(TutoriaAnteproyecto t) {
        TutoriaHistorialResponse r = new TutoriaHistorialResponse();
        r.setIdTutoria(t.getIdTutoria());
        r.setFecha(t.getFecha());
        r.setHora(t.getHora());
        r.setModalidad(t.getModalidad());
        r.setEstado(t.getEstado());

        // director nombre
        String directorNombre = "";
        try {
            var u = t.getDocente().getUsuario();
            directorNombre = (u.getNombres() == null ? "" : u.getNombres())
                    + " " + (u.getApellidos() == null ? "" : u.getApellidos());
            directorNombre = directorNombre.trim();
        } catch (Exception ignored) {
        }
        r.setDirectorNombre(directorNombre);

        // acta si existe
        actaRepo.findByTutoria_IdTutoria(t.getIdTutoria()).ifPresent(a -> {
            r.setIdActa(a.getIdActa());
            r.setObservaciones(a.getObservaciones());
            r.setCumplimiento(a.getCumplimiento());
            r.setConclusion(a.getConclusion());
        });

        return r;
    }
}