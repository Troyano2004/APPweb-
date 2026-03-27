
package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
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

    @Auditable(entidad = "RolApp", accion = "CREATE", capturarArgs = true)
    @Transactional
    public RolAppDto crear(RolAppCreateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");
        if (req.getNombre() == null || req.getNombre().trim().isEmpty())
            throw new RuntimeException("Nombre requerido");
        if (req.getPermisos() == null || req.getPermisos().isEmpty())
            throw new RuntimeException("Debe seleccionar al menos un permiso");

        // ── FIX Error 2: se pasa idRolBase al SP ──────────────────────────
        if (req.getIdRolBase() == null)
            throw new RuntimeException("Debe seleccionar un Rol Base del sistema (Admin, Docente, Estudiante, etc.)");

        Integer id = repo.crearRolApp(
                req.getNombre(),
                req.getDescripcion(),
                req.getActivo(),
                req.getPermisos(),
                req.getIdRolBase()   // ← parámetro nuevo
        );

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }

    @Auditable(entidad = "RolApp", accion = "UPDATE", capturarArgs = true)
    @Transactional
    public RolAppDto editar(Integer id, RolAppUpdateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");

        // ── FIX Error 2: se pasa idRolBase al SP (puede ser null → COALESCE mantiene el valor) ──
        repo.editarRolApp(id, req.getNombre(), req.getDescripcion(),
                req.getActivo(), req.getIdRolBase());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }

    @Auditable(entidad = "RolApp", accion = "CAMBIO_ESTADO", capturarArgs = false)
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

    @Auditable(entidad = "RolApp", accion = "ASIGNAR_PERMISOS", capturarArgs = false)
    @Transactional
    public RolAppDto asignarPermisos(Integer id, RolAppPermisosRequest req) {
        // ── FIX Error 4: validación explícita antes de llamar al SP ──────
        if (req == null || req.getPermisos() == null || req.getPermisos().isEmpty())
            throw new RuntimeException("Debe seleccionar al menos un permiso");

        repo.asignarPermisosRolApp(id, req.getPermisos());

        return repo.listarRolesApp().stream()
                .filter(x -> x.getIdRolApp().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol app no existe"));
    }
}