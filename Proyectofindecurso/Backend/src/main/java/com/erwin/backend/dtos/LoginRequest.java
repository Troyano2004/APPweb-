package com.erwin.backend.dtos;

public class LoginRequest {

    private String usuarioLogin;
    private String password;

    // Constructor vacío requerido por Spring/Jackson
    public LoginRequest() {}

    // Getter
    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    // Setter
    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    // Getter
    public String getPassword() {
        return password;
    }

    // Setter
    public void setPassword(String password) {
        this.password = password;
    }
}