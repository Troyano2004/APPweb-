package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioRolAppId implements Serializable {

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_rol_app")
    private Integer idRolApp;

    public UsuarioRolAppId() {}

    public UsuarioRolAppId(Integer idUsuario, Integer idRolApp) {
        this.idUsuario = idUsuario;
        this.idRolApp = idRolApp;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdRolApp() { return idRolApp; }
    public void setIdRolApp(Integer idRolApp) { this.idRolApp = idRolApp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRolAppId)) return false;
        UsuarioRolAppId that = (UsuarioRolAppId) o;
        return Objects.equals(idUsuario, that.idUsuario) && Objects.equals(idRolApp, that.idRolApp);
    }

    @Override
    public int hashCode() { return Objects.hash(idUsuario, idRolApp); }
}