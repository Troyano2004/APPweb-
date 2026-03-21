package com.erwin.backend.service;

import com.erwin.backend.dtos.PeriodoTitulacionDto;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.audit.dto.AuditEventDto;
import com.erwin.backend.audit.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PeriodoTitulacionService {

    private final PeriodoTitulacionRepository periodoRepo;
    private final AuditService auditService;

    public PeriodoTitulacionService(PeriodoTitulacionRepository periodoRepo, AuditService auditService) {
        this.periodoRepo = periodoRepo;
        this.auditService = auditService;
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

    @Auditable(entidad = "PeriodoTitulacion", accion = "CREATE", capturarArgs = true)
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

        PeriodoTitulacionDto estadoAnterior = convertirADto(periodo);

        periodo.setDescripcion(dto.getDescripcion());
        periodo.setFechaInicio(dto.getFechaInicio());
        periodo.setFechaFin(dto.getFechaFin());
        periodo.setActivo(dto.getActivo());

        PeriodoTitulacionDto resultado = convertirADto(periodoRepo.save(periodo));

        String username = null;
        try { Authentication a = SecurityContextHolder.getContext().getAuthentication();
              if (a != null && a.isAuthenticated()) username = a.getName(); } catch (Exception ignored) {}

        auditService.registrar(AuditEventDto.builder()
                .entidad("PeriodoTitulacion").accion("UPDATE")
                .entidadId(id.toString())
                .username(username)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(resultado)
                .build());

        return resultado;
    }

    public PeriodoTitulacionDto cambiarEstado(Integer id, Boolean activo) {
        PeriodoTitulacion periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Período no encontrado"));

        periodo.setActivo(activo);
        PeriodoTitulacion actualizado = periodoRepo.save(periodo);
        return convertirADto(actualizado);
    }

    @Auditable(entidad = "PeriodoTitulacion", accion = "ACTIVAR", capturarArgs = false)
    public PeriodoTitulacionDto activar(Integer id) {
        return cambiarEstado(id, true);
    }

    @Auditable(entidad = "PeriodoTitulacion", accion = "DESACTIVAR", capturarArgs = false)
    public PeriodoTitulacionDto desactivar(Integer id) {
        return cambiarEstado(id, false);
    }

    @Auditable(entidad = "PeriodoTitulacion", accion = "DELETE")
    public void eliminar(Integer id) {
        if (!periodoRepo.existsById(id)) {
            throw new RuntimeException("Período no encontrado");
        }
        periodoRepo.deleteById(id);
    }
    @Transactional(readOnly = true)
    public PeriodoTitulacionDto obtenerPeriodoActivo() {

        PeriodoTitulacion periodo = periodoRepo
                .findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NO_EXISTE_PERIODO_ACTIVO"
                ));

        return convertirADto(periodo);
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
