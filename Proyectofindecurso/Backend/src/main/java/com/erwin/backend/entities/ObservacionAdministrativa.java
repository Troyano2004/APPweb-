package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observacion_administrativa")
public class ObservacionAdministrativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observacion_admin")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto")
    private ProyectoTitulacion proyecto;

    @Column(name = "tipo", length = 30, nullable = false)
    private String tipo; // RETRASO/INCUMPLIMIENTO/ADMINISTRATIVO

    @Column(name = "detalle", columnDefinition = "TEXT", nullable = false)
    private String detalle;

    @Column(name = "creado_por", length = 120)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}
