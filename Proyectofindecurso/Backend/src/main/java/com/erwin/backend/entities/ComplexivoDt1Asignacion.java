package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "complexivo_dt1_asignacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cx_dt1_estudiante_periodo",
                columnNames = {"id_estudiante", "id_periodo"}))
public class ComplexivoDt1Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Integer idAsignacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_asignado_por", nullable = false)
    private Usuario asignadoPor;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaAsignacion == null)
            this.fechaAsignacion = LocalDateTime.now();
    }
}