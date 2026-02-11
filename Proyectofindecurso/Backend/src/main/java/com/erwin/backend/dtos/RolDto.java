package com.erwin.backend.dtos;

import java.util.List;

public class RolDto {
    private Integer idRol;
    private String nombreRol;
    private Boolean activo;
    private List<String> permisos;

    public RolDto() {}

    public RolDto(Integer idRol, String nombreRol, Boolean activo, List<String> permisos) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.activo = activo;
        this.permisos = permisos;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<String> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<String> permisos) {
        this.permisos = permisos;
    }
}