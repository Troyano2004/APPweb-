package com.erwin.backend.service;

import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;

    public AuthService(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    public LoginResponse login(LoginRequest req) {

        if (req == null || req.getUsuarioLogin() == null || req.getPassword() == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        Usuario usuario = usuarioRepo
                .findByUsername(req.getUsuarioLogin())
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        if (!usuario.getPasswordHash().equals(req.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // ✅ CONVERTIR ROL A FORMATO ROLE_...
        String rolFrontend = convertirRol(usuario.getRolAsignado());

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolFrontend,
                usuario.getNombres(),
                usuario.getApellidos()
        );
    }

    private String convertirRol(String rolAsignado) {
        if (rolAsignado == null) return "";

        String rol = rolAsignado.trim().toUpperCase();

        if (rol.equals("ADMIN")) return "ROLE_ADMIN";
        if (rol.equals("DOCENTE")) return "ROLE_DOCENTE";
        if (rol.equals("ESTUDIANTE")) return "ROLE_ESTUDIANTE";
        if (rol.equals("COORDINADOR")) return "ROLE_COORDINADOR";

        return rol;
    }
}
