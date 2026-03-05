
package com.erwin.backend.dtos;

import java.util.List;

public class RolAppCreateRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private List<Integer> permisos;

    // ✅ NUEVO: id del rol base de BD (roles_sistema) al que pertenece este rol del aplicativo
    // Ejemplo: 1=ADMIN, 2=DOCENTE, 3=ESTUDIANTE, 4=COORDINADOR, 5=DIRECTOR_ADMINISTRATIVO
    private Integer idRolBase;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<Integer> getPermisos() { return permisos; }
    public void setPermisos(List<Integer> permisos) { this.permisos = permisos; }

    // ✅ NUEVO
    public Integer getIdRolBase() { return idRolBase; }
    public void setIdRolBase(Integer idRolBase) { this.idRolBase = idRolBase; }
}