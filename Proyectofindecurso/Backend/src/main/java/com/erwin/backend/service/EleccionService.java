package com.erwin.backend.service;

import com.erwin.backend.entities.Elecciontitulacion;
import com.erwin.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EleccionService {
    private final EleccionTitulacionRepository repo;
    private final CarreraModalidadRepository carreraModalidadRepo;
    private final EstudianteRepository estudianteRepo;
    private final CarreraRepository carreraRepo;
    private final ModalidadTitulacionRepository modalidadRepo;
    private final PeriodoTitulacionRepository periodoRepo;

    public EleccionService(EleccionTitulacionRepository repo,
                           CarreraModalidadRepository carreraModalidadRepo,
                           EstudianteRepository estudianteRepo,
                           CarreraRepository carreraRepo,
                           ModalidadTitulacionRepository modalidadRepo,
                           PeriodoTitulacionRepository periodoRepo) {
        this.repo = repo;
        this.carreraModalidadRepo = carreraModalidadRepo;
        this.estudianteRepo = estudianteRepo;
        this.carreraRepo = carreraRepo;
        this.modalidadRepo = modalidadRepo;
        this.periodoRepo = periodoRepo;
    }

    public Elecciontitulacion elegir(Integer idEstudiante,
                                     Integer idCarrera,
                                     Integer idModalidad,
                                     Integer idPeriodo) {

        boolean permitido =
                carreraModalidadRepo
                        .existsById_IdCarreraAndId_IdModalidadAndActivoTrue(
                                idCarrera, idModalidad
                        );

        if (!permitido) {
            throw new RuntimeException("Modalidad no permitida para la carrera");
        }

        Elecciontitulacion e = new Elecciontitulacion();
        e.setEstudiante(estudianteRepo.findById(idEstudiante).orElseThrow());
        e.setCarrera(carreraRepo.findById(idCarrera).orElseThrow());
        e.setModalidad(modalidadRepo.findById(idModalidad).orElseThrow());
        e.setPeriodo(periodoRepo.findById(idPeriodo).orElseThrow());
        e.setFechaEleccion(LocalDate.now());
        e.setEstado("ACTIVA");

        return repo.save(e);
    }
}
