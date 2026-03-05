package com.erwin.backend.dtos;

/**
 * DTO para los roles físicos de la base de datos (pg_roles),
 * mostrados dinámicamente en el aplicativo.
 */
public class RolBdDto {

    private String rolBd;         // nombre en pg_roles, ej: rol_admin
    private String rolApp;        // nombre en rol_app, ej: ROLE_ADMIN
    private Integer idRolApp;     // id en rol_app
    private String rolBase;       // nombre en roles_sistema, ej: ADMIN

    public RolBdDto() {}

    public RolBdDto(String rolBd, String rolApp, Integer idRolApp, String rolBase) {
        this.rolBd = rolBd;
        this.rolApp = rolApp;
        this.idRolApp = idRolApp;
        this.rolBase = rolBase;
    }

    public String getRolBd() { return rolBd; }
    public void setRolBd(String rolBd) { this.rolBd = rolBd; }

    public String getRolApp() { return rolApp; }
    public void setRolApp(String rolApp) { this.rolApp = rolApp; }

    public Integer getIdRolApp() { return idRolApp; }
    public void setIdRolApp(Integer idRolApp) { this.idRolApp = idRolApp; }

    public String getRolBase() { return rolBase; }
    public void setRolBase(String rolBase) { this.rolBase = rolBase; }
}