package com.erwin.backend.service;

import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.CryptoUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest req, HttpSession session) {

        if (req == null || req.getUsuarioLogin() == null || req.getPassword() == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        String usernameLogin = req.getUsuarioLogin().trim();
        String passwordApp = req.getPassword().trim();

        Usuario usuario = usuarioRepo
                .findByUsername(usernameLogin)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        validarPasswordMixto(passwordApp, usuario.getPasswordHash());

        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : (usuario.getUsername() != null ? usuario.getUsername().trim() : null);

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();

        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.trim().isEmpty()) {
            String dbPass = CryptoUtil.decrypt(dbPassEncrypted.trim());
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser);
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass);
        } else {
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
        }

        // Rol principal del usuario
        String rolPrincipal = convertirRol(usuario.getRolAsignado());

        // Construir lista de todos los roles disponibles para el usuario
        List<String> roles = new ArrayList<>();
        roles.add(rolPrincipal);

        // Agregar roles adicionales asignados
        Set<String> rolesAdicionales = usuario.getRolesAplicativo();
        if (rolesAdicionales != null) {
            for (String rolAdicional : rolesAdicionales) {
                String rolConvertido = convertirRol(rolAdicional);
                if (!rolConvertido.isEmpty() && !roles.contains(rolConvertido)) {
                    roles.add(rolConvertido);
                }
            }
        }

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolPrincipal,
                roles,
                usuario.getNombres(),
                usuario.getApellidos()
        );
    }

    private void validarPasswordMixto(String rawPassword, String storedPassword) {
        if (rawPassword == null) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene passwordHash configurado");
        }

        String stored = storedPassword.trim();
        boolean esBcrypt = stored.matches("^\\$2[aby]\\$\\d\\d\\$.+");

        boolean ok;
        if (esBcrypt) {
            ok = passwordEncoder.matches(rawPassword, stored);
        } else {
            ok = rawPassword.equals(stored);
        }

        if (!ok) {
            throw new RuntimeException("Contraseña incorrecta");
        }
    }

    private String convertirRol(String rolAsignado) {
        if (rolAsignado == null) return "";

        String rol = rolAsignado.trim().toUpperCase();

        if (rol.equals("ADMIN")) return "ROLE_ADMIN";
        if (rol.equals("DOCENTE")) return "ROLE_DOCENTE";
        if (rol.equals("ESTUDIANTE")) return "ROLE_ESTUDIANTE";
        if (rol.equals("COORDINADOR")) return "ROLE_COORDINADOR";
        if (rol.equals("DIRECTOR")) return "ROLE_DIRECTOR";
        if (rol.equals("TRIBUNAL")) return "ROLE_TRIBUNAL";
        if (rol.equals("COMISION_FORMATIVA")) return "ROLE_COMISION_FORMATIVA";

        // Si ya viene con prefijo ROLE_
        if (rol.startsWith("ROLE_")) return rol;

        return rol;
    }
}
