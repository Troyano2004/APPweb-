package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Módulo 3 — Historial de intentos del certificado antiplagio COMPILATIO.
 * Umbral aprobatorio: menos del 10% de coincidencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "antiplagio_intento")
public class AntiplacioIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Integer idIntento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    /** Director que subió el informe COMPILATIO. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_director", nullable = false)
    private Docente director;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento;

    /** Porcentaje de coincidencia obtenido en COMPILATIO (ej. 8.50). */
    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2, nullable = false)
    private BigDecimal porcentajeCoincidencia;

    /** URL del PDF del informe COMPILATIO en Azure Blob Storage. */
    @Column(name = "url_informe", length = 500, nullable = false)
    private String urlInforme;

    /** true si el porcentaje < 10% (favorable); false si >= 10% (bloqueado). */
    @Column(name = "favorable", nullable = false)
    private Boolean favorable;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    public void prePersist() {
        if (this.fechaIntento == null) {
            this.fechaIntento = LocalDateTime.now();
        }
    }
}
