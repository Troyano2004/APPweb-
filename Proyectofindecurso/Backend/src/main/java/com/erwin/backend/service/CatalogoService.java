package com.erwin.backend.service;

import com.erwin.backend.entities.*;
import com.erwin.backend.repository.CarreraModalidadRepository;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {
    private final CarreraRepository carreraRepo;
    private final ModalidadTitulacionRepository modalidadRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final CarreraModalidadRepository carreraModalidadRepo;

    public CatalogoService(CarreraRepository carreraRepo,
                           ModalidadTitulacionRepository modalidadRepo,
                           PeriodoTitulacionRepository periodoRepo,
                           CarreraModalidadRepository carreraModalidadRepo) {
        this.carreraRepo = carreraRepo;
        this.modalidadRepo = modalidadRepo;
        this.periodoRepo = periodoRepo;
        this.carreraModalidadRepo = carreraModalidadRepo;
    }

    public List<Carrera> carreras() {
        return carreraRepo.findAll();
    }

    public List<Modalidadtitulacion> modalidades() {
        return modalidadRepo.findAll();
    }

    public PeriodoTitulacion periodoActivo() {
        return periodoRepo.findByActivoTrue()
                .orElseThrow(() -> new RuntimeException("No hay período activo"));
    }

    public void asignarModalidad(Integer idCarrera, Integer idModalidad) {
        Carreramodalidadid id = new Carreramodalidadid(idCarrera, idModalidad);
        if (!carreraModalidadRepo.existsById(id)) {
            Carreramodalidad cm = new Carreramodalidad();
            cm.setId(id);
            cm.setActivo(true);
            carreraModalidadRepo.save(cm);
        }
    }
}
