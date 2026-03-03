
package com.erwin.backend.dtos;

public class UsuarioUpdateRequest {

    private String nombres;
    private String apellidos;

    // null => no tocar roles
    // []   => inválido (si llega, tu Service/Repo debe lanzar error o el SP)
    // [..] => reemplazar roles
    private Integer[] idsRolApp;

    private Boolean activo;
    private String password;

    public UsuarioUpdateRequest() {}

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public Integer[] getIdsRolApp() { return idsRolApp; }
    public void setIdsRolApp(Integer[] idsRolApp) { this.idsRolApp = idsRolApp; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}