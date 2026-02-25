package com.erwin.backend.service;

import com.erwin.backend.dtos.UniversidadDto;
import com.erwin.backend.entities.Universidad;
import com.erwin.backend.repository.UniversidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UniversidadService {

    private final UniversidadRepository universidadRepo;

    public UniversidadService(UniversidadRepository universidadRepo) {
        this.universidadRepo = universidadRepo;
    }

    public List<UniversidadDto> listarTodas() {
        return universidadRepo.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public UniversidadDto obtenerPorId(Integer id) {
        Universidad universidad = universidadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));
        return convertirADto(universidad);
    }

    public UniversidadDto crear(UniversidadDto dto) {
        Universidad universidad = new Universidad();
        universidad.setNombre(dto.getNombre());
        universidad.setMision(dto.getMision());
        universidad.setVision(dto.getVision());
        universidad.setLema(dto.getLema());
        universidad.setCampus(dto.getCampus());
        universidad.setDireccion(dto.getDireccion());
        universidad.setContactoInfo(dto.getContactoInfo());
        
        Universidad guardada = universidadRepo.save(universidad);
        return convertirADto(guardada);
    }

    public UniversidadDto actualizar(Integer id, UniversidadDto dto) {
        Universidad universidad = universidadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));
        
        universidad.setNombre(dto.getNombre());
        universidad.setMision(dto.getMision());
        universidad.setVision(dto.getVision());
        universidad.setLema(dto.getLema());
        universidad.setCampus(dto.getCampus());
        universidad.setDireccion(dto.getDireccion());
        universidad.setContactoInfo(dto.getContactoInfo());
        
        Universidad actualizada = universidadRepo.save(universidad);
        return convertirADto(actualizada);
    }

    public void eliminar(Integer id) {
        if (!universidadRepo.existsById(id)) {
            throw new RuntimeException("Universidad no encontrada");
        }
        universidadRepo.deleteById(id);
    }

    private UniversidadDto convertirADto(Universidad u) {
        return new UniversidadDto(
            u.getIdUniversidad(),
            u.getNombre(),
            u.getMision(),
            u.getVision(),
            u.getLema(),
            u.getCampus(),
            u.getDireccion(),
            u.getContactoInfo()
        );
    }
}
