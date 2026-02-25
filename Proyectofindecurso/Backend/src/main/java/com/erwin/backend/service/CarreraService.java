package com.erwin.backend.service;

import com.erwin.backend.dtos.CarreraDto;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Facultad;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.FacultadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CarreraService {

    private final CarreraRepository carreraRepo;
    private final FacultadRepository facultadRepo;

    public CarreraService(CarreraRepository carreraRepo, FacultadRepository facultadRepo) {
        this.carreraRepo = carreraRepo;
        this.facultadRepo = facultadRepo;
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

    public CarreraDto crear(CarreraDto dto) {
        Facultad facultad = facultadRepo.findById(dto.getIdFacultad())
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        
        Carrera carrera = new Carrera();
        carrera.setNombre(dto.getNombre());
        carrera.setFacultad(facultad);
        
        Carrera guardada = carreraRepo.save(carrera);
        return convertirADto(guardada);
    }

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

    public void eliminar(Integer id) {
        if (!carreraRepo.existsById(id)) {
            throw new RuntimeException("Carrera no encontrada");
        }
        carreraRepo.deleteById(id);
    }

    private CarreraDto convertirADto(Carrera c) {
        return new CarreraDto(
            c.getIdCarrera(),
            c.getNombre(),
            c.getFacultad() != null ? c.getFacultad().getIdFacultad() : null,
            c.getFacultad() != null ? c.getFacultad().getNombre() : null
        );
    }
}
