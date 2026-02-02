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
@Table(name="login_bd_rol")
public class LoginBdRol {
    @EmbeddedId
    private LoginBdRolId id;

    @ManyToOne(optional = false)
    @MapsId("idLoginBd")
    @JoinColumn(name = "id_login_bd")
    private Loginbd loginBd;
}
