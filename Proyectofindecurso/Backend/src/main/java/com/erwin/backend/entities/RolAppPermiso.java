package com.erwin.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "rol_app_permiso")
public class RolAppPermiso {

    @EmbeddedId
    private RolAppPermisoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolApp")
    @JoinColumn(name = "id_rol_app")
    private RolApp rolApp;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPermiso")
    @JoinColumn(name = "id_permiso")
    private Permiso permiso;

    public RolAppPermiso() {}

    public RolAppPermiso(RolApp rolApp, Permiso permiso) {
        this.rolApp = rolApp;
        this.permiso = permiso;
        this.id = new RolAppPermisoId(rolApp.getIdRolApp(), permiso.getIdPermiso());
    }

    public RolAppPermisoId getId() { return id; }
    public RolApp getRolApp() { return rolApp; }
    public Permiso getPermiso() { return permiso; }

    public void setId(RolAppPermisoId id) { this.id = id; }
    public void setRolApp(RolApp rolApp) { this.rolApp = rolApp; }
    public void setPermiso(Permiso permiso) { this.permiso = permiso; }
}