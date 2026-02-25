package com.erwin.backend.service;

import com.erwin.backend.dtos.PeriodoTitulacionDto;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PeriodoTitulacionService {

    private final PeriodoTitulacionRepository periodoRepo;

    public PeriodoTitulacionService(PeriodoTitulacionRepository periodoRepo) {
        this.periodoRepo = periodoRepo;
    }

    public List<PeriodoTitulacionDto> listarTodos() {
        return periodoRepo.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<PeriodoTitulacionDto> listarActivos() {
        return periodoRepo.findByActivo(true).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public PeriodoTitulacionDto obtenerPorId(Integer id) {
        PeriodoTitulacion periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Período no encontrado"));
        return convertirADto(periodo);
    }

    public PeriodoTitulacionDto crear(PeriodoTitulacionDto dto) {
        PeriodoTitulacion periodo = new PeriodoTitulacion();
        periodo.setDescripcion(dto.getDescripcion());
        periodo.setFechaInicio(dto.getFechaInicio());
        periodo.setFechaFin(dto.getFechaFin());
        periodo.setActivo(dto.getActivo() != null ? dto.getActivo() : false);
        
        PeriodoTitulacion guardado = periodoRepo.save(periodo);
        return convertirADto(guardado);
    }

    public PeriodoTitulacionDto actualizar(Integer id, PeriodoTitulacionDto dto) {
        PeriodoTitulacion periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Período no encontrado"));
        
        periodo.setDescripcion(dto.getDescripcion());
        periodo.setFechaInicio(dto.getFechaInicio());
        periodo.setFechaFin(dto.getFechaFin());
        periodo.setActivo(dto.getActivo());
        
        PeriodoTitulacion actualizado = periodoRepo.save(periodo);
        return convertirADto(actualizado);
    }

    public PeriodoTitulacionDto cambiarEstado(Integer id, Boolean activo) {
        PeriodoTitulacion periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Período no encontrado"));
        
        periodo.setActivo(activo);
        PeriodoTitulacion actualizado = periodoRepo.save(periodo);
        return convertirADto(actualizado);
    }

    public void eliminar(Integer id) {
        if (!periodoRepo.existsById(id)) {
            throw new RuntimeException("Período no encontrado");
        }
        periodoRepo.deleteById(id);
    }

    private PeriodoTitulacionDto convertirADto(PeriodoTitulacion p) {
        return new PeriodoTitulacionDto(
            p.getIdPeriodo(),
            p.getDescripcion(),
            p.getFechaInicio(),
            p.getFechaFin(),
            p.getActivo()
        );
    }
}
