package com.erwin.backend.service;

import com.erwin.backend.dtos.CarreraDto;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Facultad;
import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.repository.CarreraModalidadRepository;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.FacultadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CarreraService {

    private final CarreraRepository carreraRepo;
    private final FacultadRepository facultadRepo;
    private final CarreraModalidadRepository carreraModalidadRepo;

    public CarreraService(CarreraRepository carreraRepo, FacultadRepository facultadRepo, CarreraModalidadRepository carreraModalidadRepo) {
        this.carreraRepo = carreraRepo;
        this.facultadRepo = facultadRepo;
        this.carreraModalidadRepo = carreraModalidadRepo;
    }

    public List<CarreraDto> listarTodas() {
        return carreraRepo.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<CarreraDto> listarPorFacultad(Integer idFacultad) {
        return carreraRepo.findByFacultadIdFacultad(idFacultad).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public CarreraDto obtenerPorId(Integer id) {
        Carrera carrera = carreraRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
        return convertirADto(carrera);
    }

    @Auditable(entidad = "Carrera", accion = "CREATE", capturarArgs = true)
    public CarreraDto crear(CarreraDto dto) {
        Facultad facultad = facultadRepo.findById(dto.getIdFacultad())
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        
        Carrera carrera = new Carrera();
        carrera.setNombre(dto.getNombre());
        carrera.setFacultad(facultad);
        
        Carrera guardada = carreraRepo.save(carrera);
        return convertirADto(guardada);
    }

    @Auditable(entidad = "Carrera", accion = "UPDATE", capturarArgs = true)
    public CarreraDto actualizar(Integer id, CarreraDto dto) {
        Carrera carrera = carreraRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
        
        Facultad facultad = facultadRepo.findById(dto.getIdFacultad())
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        
        carrera.setNombre(dto.getNombre());
        carrera.setFacultad(facultad);
        
        Carrera actualizada = carreraRepo.save(carrera);
        return convertirADto(actualizada);
    }

    @Auditable(entidad = "Carrera", accion = "DELETE", capturarArgs = false)
    public void eliminar(Integer id) {
        Carrera carrera = carreraRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));

        carreraModalidadRepo.deleteAll(carreraModalidadRepo.findById_IdCarrera(id));

        try {
            carreraRepo.delete(carrera);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("No se puede eliminar la carrera porque tiene registros asociados (estudiantes, propuestas u otros módulos).", ex);
        }
    }

    private CarreraDto convertirADto(Carrera c) {
        CarreraDto dto = new CarreraDto();

        dto.setIdCarrera(c.getIdCarrera());
        dto.setNombre(c.getNombre());

        if (c.getFacultad() != null) {
            dto.setIdFacultad(c.getFacultad().getIdFacultad());
            dto.setNombreFacultad(c.getFacultad().getNombre());
        }

        return dto;
    }
}
