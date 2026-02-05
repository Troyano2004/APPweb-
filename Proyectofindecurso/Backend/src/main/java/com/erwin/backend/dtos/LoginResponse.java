package com.erwin.backend.dtos;

public class LoginResponse {
    public Integer idUsuario;
    public String rol;
    public String nombres;
    public String apellidos;
    public String token;

    public LoginResponse(Integer idUsuario, String rol, String nombres, String apellidos, String token) {
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.token = token;
    }
}
