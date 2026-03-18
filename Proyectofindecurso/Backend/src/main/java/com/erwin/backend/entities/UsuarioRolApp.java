package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_rol_app")
public class UsuarioRolApp {

    @EmbeddedId
    private UsuarioRolAppId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolApp")
    @JoinColumn(name = "id_rol_app")
    private RolApp rolApp;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @PrePersist
    public void prePersist() {
        if (fechaAsignacion == null) fechaAsignacion = LocalDateTime.now();
    }

    public UsuarioRolApp() {}

    public UsuarioRolApp(Usuario usuario, RolApp rolApp) {
        this.usuario = usuario;
        this.rolApp = rolApp;
        this.id = new UsuarioRolAppId(usuario.getIdUsuario(), rolApp.getIdRolApp());
    }

    public UsuarioRolAppId getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public RolApp getRolApp() { return rolApp; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }

    public void setId(UsuarioRolAppId id) { this.id = id; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setRolApp(RolApp rolApp) { this.rolApp = rolApp; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}