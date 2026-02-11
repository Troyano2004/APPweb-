package com.erwin.backend.dtos;

import java.util.List;

public class RolPermisosRequest {
    private List<Integer> permisos;

    public List<Integer> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<Integer> permisos) {
        this.permisos = permisos;
    }
}