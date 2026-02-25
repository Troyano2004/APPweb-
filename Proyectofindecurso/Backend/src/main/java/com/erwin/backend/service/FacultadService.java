package com.erwin.backend.service;

import com.erwin.backend.dtos.FacultadDto;
import com.erwin.backend.entities.Facultad;
import com.erwin.backend.entities.Universidad;
import com.erwin.backend.repository.FacultadRepository;
import com.erwin.backend.repository.UniversidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FacultadService {

    private final FacultadRepository facultadRepo;
    private final UniversidadRepository universidadRepo;

    public FacultadService(FacultadRepository facultadRepo, UniversidadRepository universidadRepo) {
        this.facultadRepo = facultadRepo;
        this.universidadRepo = universidadRepo;
    }

    public List<FacultadDto> listarTodas() {
        return facultadRepo.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<FacultadDto> listarPorUniversidad(Integer idUniversidad) {
        return facultadRepo.findByUniversidadIdUniversidad(idUniversidad).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public FacultadDto obtenerPorId(Integer id) {
        Facultad facultad = facultadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        return convertirADto(facultad);
    }

    public FacultadDto crear(FacultadDto dto) {
        Universidad universidad = universidadRepo.findById(dto.getIdUniversidad())
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));
        
        Facultad facultad = new Facultad();
        facultad.setNombre(dto.getNombre());
        facultad.setUniversidad(universidad);
        
        Facultad guardada = facultadRepo.save(facultad);
        return convertirADto(guardada);
    }

    public FacultadDto actualizar(Integer id, FacultadDto dto) {
        Facultad facultad = facultadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        
        Universidad universidad = universidadRepo.findById(dto.getIdUniversidad())
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));
        
        facultad.setNombre(dto.getNombre());
        facultad.setUniversidad(universidad);
        
        Facultad actualizada = facultadRepo.save(facultad);
        return convertirADto(actualizada);
    }

    public void eliminar(Integer id) {
        if (!facultadRepo.existsById(id)) {
            throw new RuntimeException("Facultad no encontrada");
        }
        facultadRepo.deleteById(id);
    }

    private FacultadDto convertirADto(Facultad f) {
        return new FacultadDto(
            f.getIdFacultad(),
            f.getNombre(),
            f.getUniversidad() != null ? f.getUniversidad().getIdUniversidad() : null,
            f.getUniversidad() != null ? f.getUniversidad().getNombre() : null
        );
    }
}
