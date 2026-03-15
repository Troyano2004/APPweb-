package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_activa", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SesionActiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    private String nombres;
    private String apellidos;
    private String rol;
    private String ip;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    private Boolean activo = true;
}