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

    public Modalidadtitulacion crearModalidad(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new RuntimeException("El nombre de la modalidad es obligatorio");
        }

        String normalizado = nombre.trim();

        boolean existe = modalidadRepo.findByNombreIgnoreCase(normalizado).isPresent();

        if (existe) {
            throw new RuntimeException("Ya existe una modalidad con ese nombre");
        }

        Modalidadtitulacion modalidad = new Modalidadtitulacion();
        modalidad.setNombre(normalizado);
        return modalidadRepo.save(modalidad);
    }

    public Modalidadtitulacion actualizarModalidad(Integer idModalidad, String nombre) {
        if (idModalidad == null) {
            throw new RuntimeException("El id de modalidad es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new RuntimeException("El nombre de la modalidad es obligatorio");
        }

        String normalizado = nombre.trim();
        Modalidadtitulacion modalidad = modalidadRepo.findById(idModalidad)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        modalidadRepo.findByNombreIgnoreCase(normalizado)
                .filter(existente -> !existente.getIdModalidad().equals(idModalidad))
                .ifPresent(existente -> {
                    throw new RuntimeException("Ya existe una modalidad con ese nombre");
                });

        modalidad.setNombre(normalizado);
        return modalidadRepo.save(modalidad);
    }

    public void eliminarModalidad(Integer idModalidad) {
        if (idModalidad == null) {
            throw new RuntimeException("El id de modalidad es obligatorio");
        }

        Modalidadtitulacion modalidad = modalidadRepo.findById(idModalidad)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        if (carreraModalidadRepo.existsById_IdModalidadAndActivoTrue(idModalidad)) {
            throw new RuntimeException("La modalidad tiene carreras activas asociadas. Desasígnala antes de eliminarla");
        }

        if (carreraModalidadRepo.existsById_IdModalidad(idModalidad)) {
            throw new RuntimeException("La modalidad tiene historial de asignaciones. No se puede eliminar");
        }

        modalidadRepo.delete(modalidad);
    }

    public PeriodoTitulacion periodoActivo() {
        return periodoRepo.findByActivoTrue()
                .orElseThrow(() -> new RuntimeException("No hay período activo"));
    }

    public void asignarModalidad(Integer idCarrera, Integer idModalidad) {
        Carreramodalidadid id = new Carreramodalidadid(idCarrera, idModalidad);

        Carreramodalidad existente = carreraModalidadRepo.findById(id).orElse(null);
        if (existente != null) {
            if (!Boolean.TRUE.equals(existente.getActivo())) {
                existente.setActivo(true);
                carreraModalidadRepo.save(existente);
            }
            return;
        }

        Carreramodalidad cm = new Carreramodalidad();
        cm.setId(id);
        cm.setCarrera(carreraRepo.findById(idCarrera).orElseThrow(() -> new RuntimeException("Carrera no encontrada")));
        cm.setModalidad(modalidadRepo.findById(idModalidad).orElseThrow(() -> new RuntimeException("Modalidad no encontrada")));
        cm.setActivo(true);
        carreraModalidadRepo.save(cm);
    }

    public void desactivarModalidad(Integer idCarrera, Integer idModalidad) {
        Carreramodalidadid id = new Carreramodalidadid(idCarrera, idModalidad);
        Carreramodalidad existente = carreraModalidadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("La relación carrera-modalidad no existe"));

        if (Boolean.TRUE.equals(existente.getActivo())) {
            existente.setActivo(false);
            carreraModalidadRepo.save(existente);
        }
    }

    public List<CarreraModalidadDto> carreraModalidad() {
        return carreraModalidadRepo.findAll().stream()
                .map(cm -> new CarreraModalidadDto(
                        cm.getCarrera() != null ? cm.getCarrera().getIdCarrera() : cm.getId().getIdCarrera(),
                        cm.getCarrera() != null ? cm.getCarrera().getNombre() : "Sin carrera",
                        cm.getModalidad() != null ? cm.getModalidad().getIdModalidad() : cm.getId().getIdModalidad(),
                        cm.getModalidad() != null ? cm.getModalidad().getNombre() : "Sin modalidad",
                        Boolean.TRUE.equals(cm.getActivo())
                ))
                .toList();
    }

    public record CarreraModalidadDto(
            Integer idCarrera,
            String carrera,
            Integer idModalidad,
            String modalidad,
            boolean activo
    ) {
    }
}
