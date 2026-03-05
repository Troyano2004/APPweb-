
package com.erwin.backend.service;

import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.CryptoUtil;
import com.erwin.backend.security.JwtService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepo   = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService    = jwtService;
    }

    public LoginResponse login(LoginRequest req, HttpSession session) {

        if (req == null || req.getUsuarioLogin() == null || req.getPassword() == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        String usernameLogin = req.getUsuarioLogin().trim();
        String passwordApp   = req.getPassword().trim();

        Usuario usuario = usuarioRepo
                .findByUsername(usernameLogin)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        validarPasswordMixto(passwordApp, usuario.getPasswordHash());

        // ✅ Resolver usuario BD
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().isBlank())
                ? usuario.getUsernameDb().trim()
                : usuario.getUsername().trim();

        String dbPassPlain = "";
        String dbPassEncrypted = usuario.getPasswordDbEncrypted();

        if (dbPassEncrypted != null && !dbPassEncrypted.isBlank()) {
            dbPassPlain = CryptoUtil.decrypt(dbPassEncrypted.trim());
        }

        // ✅ Guardar en sesión (por si se usa desde requests con cookie)
        if (!dbUser.isBlank() && !dbPassPlain.isBlank()) {
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser);
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPassPlain);
        } else {
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
        }

        // ✅ CLAVE: generar JWT con credenciales BD embebidas
        String token = jwtService.generateToken(usernameLogin, dbUser, dbPassPlain);

        String rolFrontend = convertirRol(usuario.getRolAsignado());

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolFrontend,
                usuario.getNombres(),
                usuario.getApellidos(),
                token   // ← asegúrate de tener este campo en LoginResponse
        );
    }

    private void validarPasswordMixto(String rawPassword, String storedPassword) {
        if (rawPassword == null) throw new RuntimeException("Contraseña incorrecta");
        if (storedPassword == null || storedPassword.isBlank())
            throw new RuntimeException("El usuario no tiene passwordHash configurado");

        String stored = storedPassword.trim();
        boolean esBcrypt = stored.matches("^\\$2[aby]\\$\\d\\d\\$.+");

        boolean ok = esBcrypt
                ? passwordEncoder.matches(rawPassword, stored)
                : rawPassword.equals(stored);

        if (!ok) throw new RuntimeException("Contraseña incorrecta");
    }

    private String convertirRol(String rolAsignado) {
        if (rolAsignado == null) return "";
        return switch (rolAsignado.trim().toUpperCase()) {
            case "ADMIN"      -> "ROLE_ADMIN";
            case "DOCENTE"    -> "ROLE_DOCENTE";
            case "ESTUDIANTE" -> "ROLE_ESTUDIANTE";
            default           -> rolAsignado.trim().toUpperCase();
        };
    }
}