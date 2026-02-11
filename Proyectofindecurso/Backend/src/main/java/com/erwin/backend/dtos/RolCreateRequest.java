package com.erwin.backend.dtos;

import java.util.List;

public class RolCreateRequest {
    private String nombreRol;
    private Boolean activo;
    private List<Integer> permisos;

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

    public List<Integer> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<Integer> permisos) {
        this.permisos = permisos;
    }
}