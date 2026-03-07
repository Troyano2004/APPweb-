package com.erwin.backend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * Módulo 1 — Asignación del docente DT2 a un proyecto de titulación.
 * El docente DT2 supervisa la materia Titulación II (es quien evalúa el 60% en predefensa).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dt2_asignacion",
        uniqueConstraints = @UniqueConstraint(name = "uq_dt2_proyecto", columnNames = "id_proyecto"))
public class Dt2Asignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dt2_asignacion")
    private Integer idDt2Asignacion;
    @OneToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private ProyectoTitulacion proyecto;
    /** Docente de la materia DT2 (supervisa y evalúa en predefensa con 60%). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente_dt2", nullable = false)
    private Docente docenteDt2;
    /** Coordinador/Usuario que realizó la asignación. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_asignado_por", nullable = false)
    private Usuario asignadoPor;
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;
    @Column(name = "periodo", length = 100)
    private String periodo;
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
    @PrePersist
    public void prePersist() {
        if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDateTime.now();
        }
    }
}