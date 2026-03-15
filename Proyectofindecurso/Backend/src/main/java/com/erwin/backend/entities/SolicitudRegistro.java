package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_registro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(nullable = true, length = 20, unique = true)
    private String cedula;

    @Column(nullable = true, length = 100)
    private String nombres;

    @Column(nullable = true, length = 100)
    private String apellidos;

    @Column(name = "correo", nullable = false, length = 100, unique = true)
    private String correo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera", nullable = true)
    private Carrera carrera;

    @Column(name = "codigo_verificacion", length = 10)
    private String codigoVerificacion;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();
}