package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "rol_compuesto",
        uniqueConstraints = @UniqueConstraint(name = "rol_compuesto_nombre_key", columnNames = "nombre"))
public class RolCompuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_compuesto")
    private Integer idRolCompuesto;

    @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = OffsetDateTime.now();
    }

    public Integer getIdRolCompuesto() { return idRolCompuesto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Boolean getActivo() { return activo; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }

    public void setIdRolCompuesto(Integer idRolCompuesto) { this.idRolCompuesto = idRolCompuesto; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}