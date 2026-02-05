package com.erwin.backend.service;

import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.Loginaplicativo;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.LoginAplicativoRepository;
import com.erwin.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final LoginAplicativoRepository loginRepo;
    private final JwtService jwtService;

    public AuthService(LoginAplicativoRepository loginRepo, JwtService jwtService) {
        this.loginRepo = loginRepo;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest req) {

        if (req == null || req.usuarioLogin == null || req.password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos de login");
        }

        String user = req.usuarioLogin.trim();
        String pass = req.password; // si quieres: req.password.trim()

        Loginaplicativo login = loginRepo.findByUsuarioLoginIgnoreCase(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"
                ));

        if (login.getEstado() == null || !login.getEstado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        // ⚠️ Comparación simple (texto plano). Si usas BCrypt, avísame y te lo cambio.
        if (login.getPasswordLogin() == null || !login.getPasswordLogin().equals(pass)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"
            );
        }

        Usuario u = login.getUsuario();
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login sin usuario asociado");
        }

        // ✅ Generar token
        String token = jwtService.generateToken(login.getUsuarioLogin());

        return new LoginResponse(
                u.getIdUsuario(),
                u.getRol(),
                u.getNombres(),
                u.getApellidos(),
                token
        );
    }
}
