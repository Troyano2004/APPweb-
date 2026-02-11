package com.erwin.backend.dtos;

public class UsuarioAdminDto {
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String rol;
    private Boolean activo;

    public UsuarioAdminDto() {}

    public UsuarioAdminDto(Integer idUsuario, String username, String nombres, String apellidos, String rol, Boolean activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.rol = rol;
        this.activo = activo;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}