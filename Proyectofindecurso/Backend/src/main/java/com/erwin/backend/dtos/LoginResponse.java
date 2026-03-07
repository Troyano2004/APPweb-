package com.erwin.backend.dtos;

import java.util.List;

public class LoginResponse {

    private Integer idUsuario;
    private String rol;
    private List<String> roles;
    private String nombres;
    private String apellidos;

    public LoginResponse(Integer idUsuario, String rol, List<String> roles, String nombres, String apellidos) {
        this.idUsuario = idUsuario;
        this.rol = rol;
        this.roles = roles;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public String getRol() { return rol; }
    public List<String> getRoles() { return roles; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
}
