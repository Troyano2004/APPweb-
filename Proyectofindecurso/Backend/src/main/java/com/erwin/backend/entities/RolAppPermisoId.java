package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolAppPermisoId implements Serializable {

    @Column(name = "id_rol_app")
    private Integer idRolApp;

    @Column(name = "id_permiso")
    private Integer idPermiso;

    public RolAppPermisoId() {}

    public RolAppPermisoId(Integer idRolApp, Integer idPermiso) {
        this.idRolApp = idRolApp;
        this.idPermiso = idPermiso;
    }

    public Integer getIdRolApp() { return idRolApp; }
    public void setIdRolApp(Integer idRolApp) { this.idRolApp = idRolApp; }
    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolAppPermisoId)) return false;
        RolAppPermisoId that = (RolAppPermisoId) o;
        return Objects.equals(idRolApp, that.idRolApp) && Objects.equals(idPermiso, that.idPermiso);
    }

    @Override
    public int hashCode() { return Objects.hash(idRolApp, idPermiso); }
}