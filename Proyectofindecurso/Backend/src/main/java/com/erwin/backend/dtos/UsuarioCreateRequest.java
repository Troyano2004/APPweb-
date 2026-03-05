
package com.erwin.backend.dtos;

public class UsuarioCreateRequest {

    private String cedula;
    private String correoInstitucional;
    private String username;

    // password del sistema (BCrypt)
    private String passwordApp;

    private String nombres;
    private String apellidos;

    // Uno o muchos roles
    private Integer[] idsRolApp = new Integer[0];

    // ✅ NUEVO: rol principal (obligatorio para sp_crear_usuario_v4)
    private Integer idRolAppPrincipal;

    private Boolean activo;

    public UsuarioCreateRequest() {}

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordApp() { return passwordApp; }
    public void setPasswordApp(String passwordApp) { this.passwordApp = passwordApp; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public Integer[] getIdsRolApp() { return idsRolApp; }
    public void setIdsRolApp(Integer[] idsRolApp) {
        this.idsRolApp = (idsRolApp != null) ? idsRolApp : new Integer[0];
    }

    // ✅ NUEVO getter/setter
    public Integer getIdRolAppPrincipal() { return idRolAppPrincipal; }
    public void setIdRolAppPrincipal(Integer idRolAppPrincipal) {
        this.idRolAppPrincipal = idRolAppPrincipal;
    }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}