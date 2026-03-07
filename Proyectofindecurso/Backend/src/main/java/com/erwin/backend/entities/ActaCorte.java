package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Módulo 2 — Acta resumen al cierre de cada corte evaluativo.
 * Se genera cuando el director cierra el corte y consolida las asesorías.
 * Un proyecto puede tener como máximo 2 actas (corte 1 y corte 2).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "acta_corte",
        uniqueConstraints = @UniqueConstraint(name = "uq_acta_proyecto_corte",
                columnNames = {"id_proyecto", "numero_corte"}))
public class ActaCorte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acta_corte")
    private Integer idActaCorte;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    /** Número de corte: 1 (primer parcial) o 2 (segundo parcial). */
    @Column(name = "numero_corte", nullable = false)
    private Integer numeroCorte;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_director", nullable = false)
    private Docente director;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    /** Total de asesorías registradas en ese corte. */
    @Column(name = "total_asesorias", nullable = false)
    private Integer totalAsesorias;

    /** true si totalAsesorias >= 5 (mínimo requerido por corte). */
    @Column(name = "asesorias_suficientes", nullable = false)
    private Boolean asesoriasSuficientes;

    /** Nota del corte evaluativo registrada por el director (sobre 10). */
    @Column(name = "nota_corte", precision = 4, scale = 2)
    private BigDecimal notaCorte;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /** URL del acta PDF generada (opcional, puede generarse o adjuntarse). */
    @Column(name = "url_acta_pdf", length = 500)
    private String urlActaPdf;

    @PrePersist
    public void prePersist() {
        if (this.fechaGeneracion == null) {
            this.fechaGeneracion = LocalDateTime.now();
        }
    }
}
