package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.repository.RolAppSpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolAppService {

    private final RolAppSpRepository repo;

    public RolAppService(RolAppSpRepository repo) {
        this.repo = repo;
    }

    public List<RolAppDto> listar() {
        return repo.listarRolesApp();
    }

    @Transactional
    public RolAppDto crear(RolAppCreateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");
        if (req.getNombre() == null || req.getNombre().trim().isEmpty())
            throw new RuntimeException("Nombre requerido");
        if (req.getPermisos() == null || req.getPermisos().isEmpty())
            throw new RuntimeException("Debe seleccionar al menos un permiso");

        Integer id = repo.crearRolApp(req.getNombre(), req.getDescripcion(), req.getActivo(), req.getPermisos());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }

    @Transactional
    public RolAppDto editar(Integer id, RolAppUpdateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");
        repo.editarRolApp(id, req.getNombre(), req.getDescripcion(), req.getActivo());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }

    @Transactional
    public RolAppDto cambiarEstado(Integer id, RolAppEstadoRequest req) {
        if (req == null || req.getActivo() == null)
            throw new RuntimeException("Debe enviar activo=true/false");

        repo.cambiarEstadoRolApp(id, req.getActivo());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }

    @Transactional
    public RolAppDto asignarPermisos(Integer id, RolAppPermisosRequest req) {
        if (req == null || req.getPermisos() == null || req.getPermisos().isEmpty())
            throw new RuntimeException("Debe seleccionar al menos un permiso");

        repo.asignarPermisosRolApp(id, req.getPermisos());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }
}