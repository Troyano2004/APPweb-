
package com.erwin.backend.dtos;

import java.util.List;

public class RolAppCreateRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private List<Integer> permisos;

    // ── FIX Error 2: campo obligatorio para asignar el rol base de PostgreSQL ──
    private Integer idRolBase;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<Integer> getPermisos() { return permisos; }
    public void setPermisos(List<Integer> permisos) { this.permisos = permisos; }

    public Integer getIdRolBase() { return idRolBase; }
    public void setIdRolBase(Integer idRolBase) { this.idRolBase = idRolBase; }
}