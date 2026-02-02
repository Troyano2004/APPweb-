package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="login_bd")
public class Loginbd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_login_bd")
    private Integer idLoginBd;

    @Column(name="username_bd", nullable=false, unique=true)
    private String usernameBd;

    @Column(name="password_bd", nullable=false)
    private String passwordBd; // HASH

    @Column(nullable=false)
    private Boolean estado = true;

    @OneToOne(optional=false)
    @JoinColumn(name="id_usuario", referencedColumnName="id_usuario", unique=true)
    private Usuario usuario;
}
