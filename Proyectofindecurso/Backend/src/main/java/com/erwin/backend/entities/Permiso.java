package com.erwin.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "permisos")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @Column(name = "codigo", nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Integer getIdPermiso() { return idPermiso; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public Boolean getActivo() { return activo; }

    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}