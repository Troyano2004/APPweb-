package com.erwin.backend.dtos;

import java.util.List;

public class RolAppDto {
    private Integer idRolApp;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private List<String> permisos;

    public RolAppDto() {}

    public RolAppDto(Integer idRolApp, String nombre, String descripcion, Boolean activo, List<String> permisos) {
        this.idRolApp = idRolApp;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.permisos = permisos;
    }

    public Integer getIdRolApp() { return idRolApp; }
    public void setIdRolApp(Integer idRolApp) { this.idRolApp = idRolApp; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<String> getPermisos() { return permisos; }
    public void setPermisos(List<String> permisos) { this.permisos = permisos; }
}