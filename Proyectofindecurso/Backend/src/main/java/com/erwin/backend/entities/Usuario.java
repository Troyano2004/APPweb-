package com.erwin.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 20)
    private String cedula;

    @Column(name = "correo_institucional", length = 100)
    private String correoInstitucional;

    @Column(nullable = false, length = 20)
    private String rol; // ADMIN/DOCENTE/ESTUDIANTE (texto)

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    private String nombres;
    private String apellidos;

    @Column(name = "rol_asignado")
    private String rolAsignado;

    @Column(nullable = false)
    private Boolean activo = true;

    // ✅ FK real a roles_sistema (esto llena usuario.id_rol)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolSistema rolSistema;

    // GETTERS
    public Integer getIdUsuario() { return idUsuario; }
    public String getCedula() { return cedula; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public String getRol() { return rol; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getRolAsignado() { return rolAsignado; }
    public Boolean getActivo() { return activo; }
    public RolSistema getRolSistema() { return rolSistema; }

    // SETTERS
    public void setCedula(String cedula) { this.cedula = cedula; }
    public void setCorreoInstitucional(String correoInstitucional) { this.correoInstitucional = correoInstitucional; }
    public void setRol(String rol) { this.rol = rol; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public void setRolAsignado(String rolAsignado) { this.rolAsignado = rolAsignado; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setRolSistema(RolSistema rolSistema) { this.rolSistema = rolSistema; }
}