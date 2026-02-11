package com.erwin.backend.service;

import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.repository.UsuarioSpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final UsuarioSpRepository usuarioSpRepo;

    public AdminUsuarioService(UsuarioRepository usuarioRepo, UsuarioSpRepository usuarioSpRepo) {
        this.usuarioRepo = usuarioRepo;
        this.usuarioSpRepo = usuarioSpRepo;
    }

    public List<UsuarioAdminDto> listar() {
        return usuarioSpRepo.listarUsuarios();
    }

    @Transactional
    public UsuarioAdminDto crear(UsuarioCreateRequest req) {
        validarCreate(req);

        String username = req.getUsername().trim();
        String rol = normalizarRol(req.getRol());

        if (usuarioRepo.existsByUsername(username)) {
            throw new RuntimeException("El usuario ya existe");
        }

        Integer newId = usuarioSpRepo.crearUsuario(
                req.getCedula().trim(),
                req.getCorreoInstitucional() != null ? req.getCorreoInstitucional().trim() : null,
                username,
                req.getPassword(),
                req.getNombres().trim(),
                req.getApellidos().trim(),
                rol,
                req.getActivo() != null ? req.getActivo() : true
        );

        Usuario u = usuarioRepo.findById(newId)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));
        return toDto(u);
    }

    @Transactional
    public UsuarioAdminDto editar(Integer id, UsuarioUpdateRequest req) {
        String nombres = req.getNombres() != null ? req.getNombres().trim() : null;
        String apellidos = req.getApellidos() != null ? req.getApellidos().trim() : null;
        String rol = req.getRol() != null && !req.getRol().trim().isEmpty()
                ? normalizarRol(req.getRol())
                : null;
        String password = req.getPassword() != null ? req.getPassword().trim() : null;

        usuarioSpRepo.editarUsuario(id, nombres, apellidos, rol, req.getActivo(), password);

        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));
        return toDto(u);
    }

    @Transactional
    public UsuarioAdminDto cambiarEstado(Integer id, UsuarioEstadoRequest req) {
        if (req == null || req.getActivo() == null) {
            throw new RuntimeException("Debe enviar activo=true/false");
        }

        usuarioSpRepo.cambiarEstado(id, req.getActivo());

        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));
        return toDto(u);
    }

    private UsuarioAdminDto toDto(Usuario u) {
        return new UsuarioAdminDto(
                u.getIdUsuario(),
                u.getUsername(),
                u.getNombres(),
                u.getApellidos(),
                u.getRol(),
                u.getActivo()
        );
    }

    private void validarCreate(UsuarioCreateRequest req) {
        if (req == null) throw new RuntimeException("Body requerido");
        if (vacio(req.getCedula())) throw new RuntimeException("Cédula requerida");
        if (vacio(req.getUsername())) throw new RuntimeException("Username requerido");
        if (vacio(req.getPassword())) throw new RuntimeException("Password requerida");
        if (vacio(req.getNombres())) throw new RuntimeException("Nombres requeridos");
        if (vacio(req.getApellidos())) throw new RuntimeException("Apellidos requeridos");
        if (vacio(req.getRol())) throw new RuntimeException("Rol requerido");
    }

    private boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalizarRol(String rol) {
        String r = rol.trim().toUpperCase();

        if (r.equals("ROLE_ADMIN")) return "ADMIN";
        if (r.equals("ROLE_DOCENTE")) return "DOCENTE";
        if (r.equals("ROLE_ESTUDIANTE")) return "ESTUDIANTE";

        if (!r.equals("ADMIN") && !r.equals("DOCENTE") && !r.equals("ESTUDIANTE")) {
            throw new RuntimeException("Rol inválido. Use ADMIN, DOCENTE o ESTUDIANTE");
        }
        return r;
    }

}