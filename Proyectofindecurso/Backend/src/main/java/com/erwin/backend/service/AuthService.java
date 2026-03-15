package com.erwin.backend.service;

import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.config.SessionStore;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.SesionActiva;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.SesionActivaRepository;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.CryptoUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final SesionActivaRepository sesionActivaRepo;

    public AuthService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder,     SesionActivaRepository sesionActivaRepo) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.sesionActivaRepo = sesionActivaRepo;

    }

    public LoginResponse login(LoginRequest req, HttpSession session,  HttpServletRequest request) {

        if (req == null || req.getUsuarioLogin() == null || req.getPassword() == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        String usernameLogin = req.getUsuarioLogin().trim();
        String passwordApp = req.getPassword().trim();

        Usuario usuario = usuarioRepo
                .findByUsername(usernameLogin)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        // ✅ 1) Validar password del aplicativo (BCrypt o texto plano - PRUEBAS)
        validarPasswordMixto(passwordApp, usuario.getPasswordHash());

        // ✅ 2) Credenciales BD (OPCIONAL EN PRUEBAS)
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : (usuario.getUsername() != null ? usuario.getUsername().trim() : null);

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();

        // ✅ 3) Guardar en sesión SOLO si existe password_db_encrypted
        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.trim().isEmpty()) {
            String dbPass = CryptoUtil.decrypt(dbPassEncrypted.trim());
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser); // "DB_USER"
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass); // "DB_PASS"
        } else {
            // Modo prueba: si no hay credenciales BD, igual dejamos iniciar sesión.
            // (Opcional) limpiar por si quedó algo viejo
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
        }

        // ✅ 4) Rol para frontend
        String rolFrontend = convertirRol(usuario.getRolAsignado());
        // Cerrar sesiones anteriores del mismo usuario
        sesionActivaRepo.findByIdUsuarioAndActivoTrue(usuario.getIdUsuario()).forEach(s -> {
            s.setActivo(false);
            sesionActivaRepo.save(s);
        });


        SesionActiva sesionActiva = new SesionActiva();
        sesionActiva.setRol(usuario.getRolAsignado());
        sesionActiva.setActivo(true);
        sesionActiva.setIp(obtenerIp(request));
        sesionActiva.setSessionId(session.getId());
        sesionActiva.setApellidos(usuario.getApellidos());
        sesionActiva.setNombres(usuario.getNombres());
        sesionActiva.setFechaInicio(LocalDateTime.now());
        sesionActiva.setUltimaActividad(LocalDateTime.now());
        sesionActiva.setIdUsuario(usuario.getIdUsuario());
        sesionActivaRepo.save(sesionActiva);

        SessionStore.register(session.getId(), session);

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolFrontend,
                usuario.getNombres(),
                usuario.getApellidos()
        );
    }

    /**
     * Permite login con passwordHash BCrypt o en texto plano (solo pruebas).
     */
    private void validarPasswordMixto(String rawPassword, String storedPassword) {
        if (rawPassword == null) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene passwordHash configurado");
        }

        String stored = storedPassword.trim();

        // Detecta BCrypt típico: $2a$ / $2b$ / $2y$
        boolean esBcrypt = stored.matches("^\\$2[aby]\\$\\d\\d\\$.+");

        boolean ok;
        if (esBcrypt) {
            ok = passwordEncoder.matches(rawPassword, stored);
        } else {
            // Texto plano
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

        return rol;
    }
    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}