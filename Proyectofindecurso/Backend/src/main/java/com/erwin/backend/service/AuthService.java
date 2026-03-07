
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
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    public AuthService(UsuarioRepository usuarioRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepo     = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
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

        // ✅ 1) Validar password del aplicativo (BCrypt o texto plano)
        validarPasswordMixto(passwordApp, usuario.getPasswordHash());

        // ✅ 2) Credenciales BD
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : (usuario.getUsername() != null ? usuario.getUsername().trim() : null);

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();
        String dbPass = null;

        // ✅ 3) Guardar en sesión HTTP + generar JWT con credenciales BD
        //
        // FIX: antes solo se guardaba en sesión, pero el frontend Angular no enviaba
        // la cookie JSESSIONID (falta withCredentials). Ahora TAMBIÉN se genera un JWT
        // con db_user/db_pass para que el DbSessionFilter lo use como PRIORIDAD 1.
        // Así funciona aunque las cookies fallen.
        String jwtToken = null;

        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.trim().isEmpty()) {
            dbPass = CryptoUtil.decrypt(dbPassEncrypted.trim());

            // Guardar en sesión HTTP (funciona si el frontend envía withCredentials)
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser);
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass);

            // ✅ FIX ADICIONAL: generar JWT con las credenciales BD embebidas
            // El DbSessionFilter ya lo lee como PRIORIDAD 1 (antes del fallback a sesión)
            jwtToken = jwtService.generateToken(usernameLogin, dbUser, dbPass);

        } else {
            // Sin credenciales BD configuradas: limpiar sesión y generar JWT básico
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
            jwtToken = jwtService.generateToken(usernameLogin);
        }

        // ✅ 4) Rol para frontend
        String rolFrontend = convertirRol(usuario.getRolAsignado());

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolFrontend,
                usuario.getNombres(),
                usuario.getApellidos(),
                jwtToken           // ✅ FIX: se devuelve el token al frontend
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

        if (rol.equals("ADMIN"))       return "ROLE_ADMIN";
        if (rol.equals("DOCENTE"))     return "ROLE_DOCENTE";
        if (rol.equals("ESTUDIANTE"))  return "ROLE_ESTUDIANTE";

        return rol;
    }
}