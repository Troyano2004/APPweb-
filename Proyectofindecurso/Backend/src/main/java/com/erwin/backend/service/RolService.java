package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.repository.RolSpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolService {

    private final RolSpRepository rolSpRepository;

    public RolService(RolSpRepository rolSpRepository) {
        this.rolSpRepository = rolSpRepository;
    }

    public List<RolDto> listarRoles() {
        return rolSpRepository.listarRoles();
    }

    public List<PermisoDto> listarPermisos() {
        return rolSpRepository.listarPermisos();
    }

    @Transactional
    public RolDto crear(RolCreateRequest req) {
        validarCreate(req);

        String nombreRol = normalizarNombreRol(req.getNombreRol());
        Boolean activo = (req.getActivo() != null) ? req.getActivo() : true;

        Integer id = rolSpRepository.crearRol(nombreRol, activo, req.getPermisos());

        return buscarRolPorId(id);
    }

    @Transactional
    public RolDto editar(Integer id, RolUpdateRequest req) {
        if (id == null) throw new RuntimeException("Id requerido");
        if (req == null) throw new RuntimeException("Body requerido");

        String nombreRol = (req.getNombreRol() != null && !req.getNombreRol().trim().isEmpty())
                ? normalizarNombreRol(req.getNombreRol())
                : null;

        rolSpRepository.editarRol(id, nombreRol, req.getActivo());

        return buscarRolPorId(id);
    }

    @Transactional
    public RolDto cambiarEstado(Integer id, RolEstadoRequest req) {
        if (id == null) throw new RuntimeException("Id requerido");
        if (req == null || req.getActivo() == null) {
            throw new RuntimeException("Debe enviar activo=true/false");
        }

        rolSpRepository.cambiarEstado(id, req.getActivo());

        return buscarRolPorId(id);
    }

    @Transactional
    public RolDto asignarPermisos(Integer id, RolPermisosRequest req) {
        if (id == null) throw new RuntimeException("Id requerido");
        if (req == null || req.getPermisos() == null || req.getPermisos().isEmpty()) {
            throw new RuntimeException("Debe enviar al menos un permiso");
        }

        rolSpRepository.asignarPermisos(id, req.getPermisos());

        return buscarRolPorId(id);
    }

    // ================== helpers ==================

    private RolDto buscarRolPorId(Integer id) {
        return rolSpRepository.listarRoles().stream()
                .filter(r -> r.getIdRol().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol no existe"));
    }

    private void validarCreate(RolCreateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");

        if (req.getNombreRol() == null || req.getNombreRol().trim().isEmpty()) {
            throw new RuntimeException("Nombre de rol requerido");
        }

        if (req.getPermisos() == null || req.getPermisos().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un permiso");
        }
    }

    private String normalizarNombreRol(String nombreRol) {
        String r = nombreRol.trim().toUpperCase();
        if (r.startsWith("ROLE_")) return r;
        return "ROLE_" + r;
    }
}