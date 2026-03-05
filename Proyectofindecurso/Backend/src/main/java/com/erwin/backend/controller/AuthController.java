package com.erwin.backend.controller;

import com.erwin.backend.dtos.LoginRequest;
import com.erwin.backend.dtos.LoginResponse;
import com.erwin.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpSession session) {
        return authService.login(req, session);
    }

    // ✅ LOGOUT
    @PostMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate(); // destruye la sesión activa
    }


}