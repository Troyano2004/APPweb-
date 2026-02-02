package com.erwin.backend.service;

import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.entities.Loginaplicativo;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.LoginAplicativoRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final LoginAplicativoRepository loginRepo;

    public AuthService(LoginAplicativoRepository loginRepo) {
        this.loginRepo = loginRepo;
    }

    public LoginResponse login(LoginRequest req) {

        if (req == null || req.usuarioLogin == null || req.password == null) {
            throw new RuntimeException("Faltan datos de login");
        }

        Loginaplicativo login = loginRepo.findByUsuarioLogin(req.usuarioLogin)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        if (login.getEstado() == null || !login.getEstado()) {
            throw new RuntimeException("Usuario inactivo");
        }

        if (!login.getPasswordLogin().equals(req.password)) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        Usuario u = login.getUsuario();
        return new LoginResponse(
                u.getIdUsuario(),
                u.getRol(),
                u.getNombres(),
                u.getApellidos()
        );
    }
}
