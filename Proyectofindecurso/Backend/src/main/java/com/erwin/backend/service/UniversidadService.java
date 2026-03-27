package com.erwin.backend.service;

import com.erwin.backend.dtos.UniversidadDto;
import com.erwin.backend.entities.Universidad;
import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.repository.CarreraModalidadRepository;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.FacultadRepository;
import com.erwin.backend.repository.UniversidadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UniversidadService {

    private final UniversidadRepository universidadRepo;
    private final FacultadRepository facultadRepo;
    private final CarreraRepository carreraRepo;
    private final CarreraModalidadRepository carreraModalidadRepo;

    public UniversidadService(UniversidadRepository universidadRepo, FacultadRepository facultadRepo,
                              CarreraRepository carreraRepo, CarreraModalidadRepository carreraModalidadRepo) {
        this.universidadRepo = universidadRepo;
        this.facultadRepo = facultadRepo;
        this.carreraRepo = carreraRepo;
        this.carreraModalidadRepo = carreraModalidadRepo;
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

    @Auditable(entidad = "Universidad", accion = "CREATE", capturarArgs = true)
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

    @Auditable(entidad = "Universidad", accion = "UPDATE", capturarArgs = true)
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

    @Auditable(entidad = "Universidad", accion = "DELETE", capturarArgs = false)
    public void eliminar(Integer id) {
        Universidad universidad = universidadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));

        var facultades = facultadRepo.findByUniversidadIdUniversidad(id);
        for (var facultad : facultades) {
            var carreras = carreraRepo.findByFacultadIdFacultad(facultad.getIdFacultad());
            for (var carrera : carreras) {
                carreraModalidadRepo.deleteAll(carreraModalidadRepo.findById_IdCarrera(carrera.getIdCarrera()));
            }
            carreraRepo.deleteAll(carreras);
        }

        try {
            facultadRepo.deleteAll(facultades);
            universidadRepo.delete(universidad);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("No se puede eliminar la universidad porque tiene registros asociados en otros módulos.", ex);
        }
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
