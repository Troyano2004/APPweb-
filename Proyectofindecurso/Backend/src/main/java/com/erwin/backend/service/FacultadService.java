package com.erwin.backend.service;

import com.erwin.backend.dtos.FacultadDto;
import com.erwin.backend.entities.Facultad;
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
public class FacultadService {

    private final FacultadRepository facultadRepo;
    private final UniversidadRepository universidadRepo;
    private final CarreraRepository carreraRepo;
    private final CarreraModalidadRepository carreraModalidadRepo;

    public FacultadService(FacultadRepository facultadRepo, UniversidadRepository universidadRepo,
                           CarreraRepository carreraRepo, CarreraModalidadRepository carreraModalidadRepo) {
        this.facultadRepo = facultadRepo;
        this.universidadRepo = universidadRepo;
        this.carreraRepo = carreraRepo;
        this.carreraModalidadRepo = carreraModalidadRepo;
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

    @Auditable(entidad = "Facultad", accion = "CREATE", capturarArgs = true)
    public FacultadDto crear(FacultadDto dto) {
        Universidad universidad = universidadRepo.findById(dto.getIdUniversidad())
                .orElseThrow(() -> new RuntimeException("Universidad no encontrada"));
        
        Facultad facultad = new Facultad();
        facultad.setNombre(dto.getNombre());
        facultad.setUniversidad(universidad);
        
        Facultad guardada = facultadRepo.save(facultad);
        return convertirADto(guardada);
    }

    @Auditable(entidad = "Facultad", accion = "UPDATE", capturarArgs = true)
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

    @Auditable(entidad = "Facultad", accion = "DELETE", capturarArgs = false)
    public void eliminar(Integer id) {
        Facultad facultad = facultadRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));

        var carreras = carreraRepo.findByFacultadIdFacultad(id);
        for (var carrera : carreras) {
            carreraModalidadRepo.deleteAll(carreraModalidadRepo.findById_IdCarrera(carrera.getIdCarrera()));
        }

        try {
            carreraRepo.deleteAll(carreras);
            facultadRepo.delete(facultad);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("No se puede eliminar la facultad porque tiene registros asociados en otros módulos.", ex);
        }
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
