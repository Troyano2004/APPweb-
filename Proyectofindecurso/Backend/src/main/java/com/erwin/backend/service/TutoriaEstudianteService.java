package com.erwin.backend.service;


import com.erwin.backend.dtos.TutoriaHistorialResponse;
import com.erwin.backend.dtos.TutoriaPendienteResponse;
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

    //No funciona

    private final AnteproyectoTitulacionRepository anteRepo;
    private final TutoriaAnteproyectoRepository tutRepo;
    private final ActaRevisionDirectorRepository actaRepo;
    private final EmailService emailService;

    public TutoriaEstudianteService(
            AnteproyectoTitulacionRepository anteRepo,
            TutoriaAnteproyectoRepository tutRepo,
            ActaRevisionDirectorRepository actaRepo,
            EmailService emailService
    ) {
        this.anteRepo = anteRepo;
        this.tutRepo = tutRepo;
        this.actaRepo = actaRepo;
        this.emailService = emailService;
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
    public List<TutoriaPendienteResponse> Reuniones(Integer idEstudiante) {
        // 1. IMPORTANTE: Agregué el "return" al inicio
        return tutRepo.findByAnteproyecto_Estudiante_IdEstudianteAndEstado(idEstudiante, "PROGRAMADA")
                .stream()
                .map(tutoria -> {
                    TutoriaPendienteResponse r = new TutoriaPendienteResponse();
                    r.setIdTutoria(tutoria.getIdTutoria());
                    r.setHora(tutoria.getHora());
                    r.setFecha(tutoria.getFecha());
                    r.setEstado(tutoria.getEstado());

                    // 2. Manejo elegante de Nombres (sin try-catch vacío)
                    String nombreTutor = "Sin asignar";
                    if (tutoria.getDocente() != null && tutoria.getDocente().getUsuario() != null) {
                        var u = tutoria.getDocente().getUsuario();
                        nombreTutor = (u.getNombres() + " " + u.getApellidos()).trim();
                    }
                    r.setTutor(nombreTutor);

                    return r;
                })
                .toList();
    }

    private TutoriaHistorialResponse mapHistorial(TutoriaAnteproyecto t) {
        TutoriaHistorialResponse r = new TutoriaHistorialResponse();
        r.setIdTutoria(t.getIdTutoria());
        r.setFecha(t.getFecha());
        r.setHora(t.getHora());
        r.setModalidad(t.getModalidad());
        r.setEstado(t.getEstado());
        r.setLinkReunion(t.getLinkReunion()); // ← agregar

        // director nombre
        String directorNombre = "";
        try {
            var u = t.getDocente().getUsuario();
            directorNombre = (u.getNombres() == null ? "" : u.getNombres())
                    + " " + (u.getApellidos() == null ? "" : u.getApellidos());
            directorNombre = directorNombre.trim();
        } catch (Exception ignored) {}
        r.setDirectorNombre(directorNombre);

        actaRepo.findByTutoria_IdTutoria(t.getIdTutoria()).ifPresent(a -> {
            r.setIdActa(a.getIdActa());
            r.setObservaciones(a.getObservaciones());
            r.setCumplimiento(a.getCumplimiento());
            r.setConclusion(a.getConclusion());
        });

        return r;
    }
}