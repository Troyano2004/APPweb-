
package com.erwin.backend.dtos;

public class RolAppUpdateRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;

    // ── FIX Error 2: permite actualizar el rol base al editar ──
    private Integer idRolBase;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Integer getIdRolBase() { return idRolBase; }
    public void setIdRolBase(Integer idRolBase) { this.idRolBase = idRolBase; }
}