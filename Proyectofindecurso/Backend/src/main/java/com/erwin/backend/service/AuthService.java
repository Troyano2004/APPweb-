
package com.erwin.backend.service;

import com.erwin.backend.config.DbSessionFilter;
import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.dtos.UsuarioAdminDto;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.repository.UsuarioSpRepository;
import com.erwin.backend.security.CryptoUtil;
import com.erwin.backend.security.JwtService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository    usuarioRepo;
    private final UsuarioSpRepository  usuarioSpRepo;
    private final PasswordEncoder      passwordEncoder;
    private final JwtService           jwtService;

    public AuthService(UsuarioRepository usuarioRepo,
                       UsuarioSpRepository usuarioSpRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepo    = usuarioRepo;
        this.usuarioSpRepo  = usuarioSpRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService     = jwtService;
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

        // 1) Validar password del aplicativo
        validarPasswordMixto(passwordApp, usuario.getPasswordHash());

        // 2) Credenciales BD
        String dbUser = (usuario.getUsernameDb() != null && !usuario.getUsernameDb().trim().isEmpty())
                ? usuario.getUsernameDb().trim()
                : (usuario.getUsername() != null ? usuario.getUsername().trim() : null);

        String dbPassEncrypted = usuario.getPasswordDbEncrypted();
        String dbPass = null;
        String jwtToken;

        if (dbUser != null && dbPassEncrypted != null && !dbPassEncrypted.trim().isEmpty()) {
            dbPass = CryptoUtil.decrypt(dbPassEncrypted.trim());
            session.setAttribute(DbSessionFilter.SES_DB_USER, dbUser);
            session.setAttribute(DbSessionFilter.SES_DB_PASS, dbPass);
            jwtToken = jwtService.generateToken(usernameLogin, dbUser, dbPass);
        } else {
            session.removeAttribute(DbSessionFilter.SES_DB_USER);
            session.removeAttribute(DbSessionFilter.SES_DB_PASS);
            jwtToken = jwtService.generateToken(usernameLogin);
        }

        // ✅ 3) Obtener TODOS los roles del usuario desde la vista vw_usuario_admin_v3
        List<String> todosLosRoles = new ArrayList<>();
        String rolPrincipal = "";

        try {
            UsuarioAdminDto dto = usuarioSpRepo.obtenerPorId(usuario.getIdUsuario());
            if (dto != null && dto.getIdsRolApp() != null && dto.getIdsRolApp().length > 0) {
                // rolesApp es algo como "ROLE_COORDINADOR, ROLE_DOCENTE"
                String rolesAppStr = dto.getRolesApp();
                if (rolesAppStr != null && !rolesAppStr.isBlank()) {
                    for (String r : rolesAppStr.split(",")) {
                        String limpio = normalizarRolApp(r.trim());
                        if (!limpio.isEmpty()) todosLosRoles.add(limpio);
                    }
                }
                // Rol principal = el primero de la lista o rol_app_principal
                rolPrincipal = todosLosRoles.isEmpty() ? "" : todosLosRoles.get(0);
            }
        } catch (Exception ignored) {
            // fallback: usar rol_asignado del usuario
        }

        // Fallback si no se encontraron roles
        if (todosLosRoles.isEmpty()) {
            rolPrincipal = convertirRol(usuario.getRolAsignado());
            if (!rolPrincipal.isEmpty()) todosLosRoles.add(rolPrincipal);
        }

        return new LoginResponse(
                usuario.getIdUsuario(),
                rolPrincipal,
                todosLosRoles,   // ✅ NUEVO: lista completa de roles
                usuario.getNombres(),
                usuario.getApellidos(),
                jwtToken
        );
    }

    // -------------------------------------------------------
    // Convierte "ROLE_COORDINADOR" o "coordinador" → "ROLE_COORDINADOR"
    // -------------------------------------------------------
    private String normalizarRolApp(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String upper = nombre.trim().toUpperCase();
        if (upper.startsWith("ROLE_")) return upper;
        return "ROLE_" + upper;
    }

    private void validarPasswordMixto(String rawPassword, String storedPassword) {
        if (rawPassword == null)
            throw new RuntimeException("Contraseña incorrecta");
        if (storedPassword == null || storedPassword.trim().isEmpty())
            throw new RuntimeException("El usuario no tiene passwordHash configurado");

        String stored  = storedPassword.trim();
        boolean esBcrypt = stored.matches("^\\$2[aby]\\$\\d\\d\\$.+");
        boolean ok = esBcrypt
                ? passwordEncoder.matches(rawPassword, stored)
                : rawPassword.equals(stored);

        if (!ok) throw new RuntimeException("Contraseña incorrecta");
    }

    private String convertirRol(String rolAsignado) {
        if (rolAsignado == null) return "";
        String rol = rolAsignado.trim().toUpperCase();
        if (rol.equals("ADMIN"))      return "ROLE_ADMIN";
        if (rol.equals("DOCENTE"))    return "ROLE_DOCENTE";
        if (rol.equals("ESTUDIANTE")) return "ROLE_ESTUDIANTE";
        return rol;
    }
}