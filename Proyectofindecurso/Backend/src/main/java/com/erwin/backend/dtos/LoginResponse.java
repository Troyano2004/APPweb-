
package com.erwin.backend.dtos;

public class LoginResponse {

    private Integer idUsuario;
    private String  rol;
    private String  nombres;
    private String  apellidos;
    private String  token;   // ✅ NUEVO

    public LoginResponse() {}

    public LoginResponse(Integer idUsuario, String rol,
                         String nombres, String apellidos, String token) {
        this.idUsuario = idUsuario;
        this.rol       = rol;
        this.nombres   = nombres;
        this.apellidos = apellidos;
        this.token     = token;
    }

    // Getters
    public Integer getIdUsuario() { return idUsuario; }
    public String  getRol()       { return rol; }
    public String  getNombres()   { return nombres; }
    public String  getApellidos() { return apellidos; }
    public String  getToken()     { return token; }

    // Setters
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public void setRol(String rol)              { this.rol = rol; }
    public void setNombres(String nombres)      { this.nombres = nombres; }
    public void setApellidos(String apellidos)  { this.apellidos = apellidos; }
    public void setToken(String token)          { this.token = token; }
}