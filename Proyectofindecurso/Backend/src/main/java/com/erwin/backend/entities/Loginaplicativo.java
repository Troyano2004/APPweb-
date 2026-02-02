package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "login_aplicativo")
public class Loginaplicativo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_login")
    private Integer idLogin;

    @Column(name="usuario_login", nullable=false, unique=true, length=50)
    private String usuarioLogin;

    @Column(name="password_login", nullable=false, length=200)
    private String passwordLogin; // BCrypt

    @Column(name="estado")
    private Boolean estado = true;

    @OneToOne(optional = false)
    @JoinColumn(name="id_usuario", nullable=false, unique=true)
    private Usuario usuario;
}
