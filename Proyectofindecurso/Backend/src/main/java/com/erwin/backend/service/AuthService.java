
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

        // ✅ 1) Validar password del aplicativo (BCrypt)
        if (!passwordEncoder.matches(passwordApp, usuario.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // ✅ 2) Credenciales BD (username_db y password_db_encrypted)
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : usuario.getUsername().trim();

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();
        if (dbPassEncrypted == null || dbPassEncrypted.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene password BD configurada (password_db_encrypted)");
        }

        String dbPass = CryptoUtil.decrypt(dbPassEncrypted);

        // ✅ 3) Guardar en sesión con las llaves que tu filtro realmente lee: DB_USER / DB_PASS
        session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser); // "DB_USER"
        session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass); // "DB_PASS"

        // ✅ 4) Rol para frontend
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

        return rol;
    }
}