package com.erwin.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "rol_compuesto_base")
public class RolCompuestoBase {

    @EmbeddedId
    private RolCompuestoBaseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolCompuesto")
    @JoinColumn(name = "id_rol_compuesto")
    private RolCompuesto rolCompuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolBase")
    @JoinColumn(name = "id_rol_base")
    private RolSistema rolBase;

    public RolCompuestoBase() {}

    public RolCompuestoBase(RolCompuesto rolCompuesto, RolSistema rolBase) {
        this.rolCompuesto = rolCompuesto;
        this.rolBase = rolBase;
        this.id = new RolCompuestoBaseId(rolCompuesto.getIdRolCompuesto(), rolBase.getIdRol());
    }

    public RolCompuestoBaseId getId() { return id; }
    public RolCompuesto getRolCompuesto() { return rolCompuesto; }
    public RolSistema getRolBase() { return rolBase; }

    public void setId(RolCompuestoBaseId id) { this.id = id; }
    public void setRolCompuesto(RolCompuesto rolCompuesto) { this.rolCompuesto = rolCompuesto; }
    public void setRolBase(RolSistema rolBase) { this.rolBase = rolBase; }
}