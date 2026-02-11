package com.erwin.backend.dtos;

public class LoginResponse {

    private Integer idUsuario;
    private String rol;
    private String nombres;
    private String apellidos;

    public LoginResponse(Integer idUsuario, String rol, String nombres, String apellidos) {
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getRol() {
        return rol;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }
}