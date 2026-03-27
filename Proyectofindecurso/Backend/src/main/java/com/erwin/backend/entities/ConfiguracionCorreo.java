package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_correo", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String proveedor; // GMAIL, YAHOO, OUTLOOK

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = true, length = 255)
    private String password; // guardado encriptado con CryptoUtil

    // === NUEVOS CAMPOS PARA OAUTH2 (Nulos por defecto) ===
    @Column(name = "client_id", nullable = true, length = 255)
    private String clientId;

    @Column(name = "client_secret", nullable = true, length = 500)
    private String clientSecret;

    // Los refresh tokens de Microsoft pueden ser bastante largos, le damos más longitud
    @Column(name = "refresh_token", nullable = true, length = 2000)
    private String refreshToken;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    public void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}