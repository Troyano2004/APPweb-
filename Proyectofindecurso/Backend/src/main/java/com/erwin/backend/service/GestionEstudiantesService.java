package com.erwin.backend.service;

import com.erwin.backend.dtos.EstudianteCarreraResponse;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GestionEstudiantesService {
    private final EstudianteRepository estudianteRepo;
    private final UsuarioRepository usuarioRepo;
    private final CarreraRepository carreraRepo;
    private final CoordinadorRepository coordinadorRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final Dt1TutorEstudianteRepository dt1TutorRepo;

    public GestionEstudiantesService(EstudianteRepository estudianteRepo, UsuarioRepository usuarioRepo, CarreraRepository carreraRepo,
                                     PeriodoTitulacionRepository periodoRepo, CoordinadorRepository coordinadorRepo, Dt1TutorEstudianteRepository dt1TutorRepo) {
        this.estudianteRepo = estudianteRepo;
        this.usuarioRepo = usuarioRepo;
        this.carreraRepo = carreraRepo;
        this.periodoRepo = periodoRepo;
        this.coordinadorRepo = coordinadorRepo;
        this.dt1TutorRepo = dt1TutorRepo;
    }
    public List<EstudianteCarreraResponse> listarEstudiantes(Integer idUsuario) {
        Coordinador coord = coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "USUARIO NO ES COORDINADOR"));
        Carrera carrera = coord.getCarrera();
        if (carrera == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COORDINADOR SIN CARRERA");


        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));

        return estudianteRepo.findByCarrera_IdCarrera(carrera.getIdCarrera()).stream().map(e ->
        {
            EstudianteCarreraResponse carreraResponse = new EstudianteCarreraResponse();
            carreraResponse.setIdEstudiante(e.getIdEstudiante());
            carreraResponse.setCarrera(carrera.getNombre());
            carreraResponse.setNombres(e.getUsuario().getNombres());
            carreraResponse.setApellidos(e.getUsuario().getApellidos());
            carreraResponse.setCedula(e.getUsuario().getCedula());

            dt1TutorRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodoAndActivoTrue(
                            e.getIdEstudiante(), periodo.getIdPeriodo())
                    .ifPresentOrElse(t -> {

                        carreraResponse.setTutorNombre(
                                t.getDocente().getUsuario().getNombres() + " " +
                                        t.getDocente().getUsuario().getApellidos()
                        );
                        carreraResponse.setTieneTutor(true);
                    }, () -> {

                        carreraResponse.setTutorNombre(null);
                        carreraResponse.setTieneTutor(false);
                    });

            return  carreraResponse;

        }).collect(java.util.stream.Collectors.toList());

    }
}
