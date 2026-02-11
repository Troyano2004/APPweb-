package com.erwin.backend.dtos;

public class PermisoDto {
    private Integer idPermiso;
    private String codigo;
    private String descripcion;
    private Boolean activo;

    public PermisoDto() {}

    public PermisoDto(Integer idPermiso, String codigo, String descripcion, Boolean activo) {
        this.idPermiso = idPermiso;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    public Integer getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(Integer idPermiso) {
        this.idPermiso = idPermiso;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}