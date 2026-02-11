package com.erwin.backend.dtos;

import java.util.List;

public class RolAppPermisosRequest {
    private List<Integer> permisos;

    public List<Integer> getPermisos() { return permisos; }
    public void setPermisos(List<Integer> permisos) { this.permisos = permisos; }
}