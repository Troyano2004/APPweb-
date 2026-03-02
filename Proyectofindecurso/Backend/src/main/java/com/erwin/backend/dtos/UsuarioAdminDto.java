
package com.erwin.backend.dtos;

public class UsuarioAdminDto {

    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;

    // ===== Compatibilidad (modelo viejo) =====
    // 1 rol (principal)
    private String rolApp;
    private Integer idRolApp;

    // ===== Nuevo (modelo multi-rol) =====
    // Roles concatenados para mostrar en tabla
    private String rolesApp;

    // Ids para precargar multi-select en edición (nunca null)
    private Integer[] idsRolApp = new Integer[0];

    private Boolean activo;

    public UsuarioAdminDto() {}

    public UsuarioAdminDto(Integer idUsuario, String username, String nombres, String apellidos,
                           String rolApp, Integer idRolApp,
                           String rolesApp, Integer[] idsRolApp,
                           Boolean activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.rolApp = rolApp;
        this.idRolApp = idRolApp;
        this.rolesApp = rolesApp;
        this.idsRolApp = (idsRolApp != null) ? idsRolApp : new Integer[0];
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

    public String getRolApp() { return rolApp; }
    public void setRolApp(String rolApp) { this.rolApp = rolApp; }

    public Integer getIdRolApp() { return idRolApp; }
    public void setIdRolApp(Integer idRolApp) { this.idRolApp = idRolApp; }

    public String getRolesApp() { return rolesApp; }
    public void setRolesApp(String rolesApp) { this.rolesApp = rolesApp; }

    public Integer[] getIdsRolApp() { return idsRolApp; }
    public void setIdsRolApp(Integer[] idsRolApp) {
        this.idsRolApp = (idsRolApp != null) ? idsRolApp : new Integer[0];
    }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}