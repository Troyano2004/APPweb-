package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolCompuestoBaseId implements Serializable {

    @Column(name = "id_rol_compuesto")
    private Integer idRolCompuesto;

    @Column(name = "id_rol_base")
    private Integer idRolBase;

    public RolCompuestoBaseId() {}

    public RolCompuestoBaseId(Integer idRolCompuesto, Integer idRolBase) {
        this.idRolCompuesto = idRolCompuesto;
        this.idRolBase = idRolBase;
    }

    public Integer getIdRolCompuesto() { return idRolCompuesto; }
    public void setIdRolCompuesto(Integer idRolCompuesto) { this.idRolCompuesto = idRolCompuesto; }
    public Integer getIdRolBase() { return idRolBase; }
    public void setIdRolBase(Integer idRolBase) { this.idRolBase = idRolBase; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolCompuestoBaseId)) return false;
        RolCompuestoBaseId that = (RolCompuestoBaseId) o;
        return Objects.equals(idRolCompuesto, that.idRolCompuesto) && Objects.equals(idRolBase, that.idRolBase);
    }

    @Override
    public int hashCode() { return Objects.hash(idRolCompuesto, idRolBase); }
}