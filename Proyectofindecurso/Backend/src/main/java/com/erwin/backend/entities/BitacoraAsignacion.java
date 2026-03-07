package com.erwin.backend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * Módulo 1 — Bitácora de todas las asignaciones realizadas en DT2.
 * Registra quién asignó qué y cuándo (docente DT2, director, tribunal).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bitacora_asignacion")
public class BitacoraAsignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Integer idBitacora;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;
    /**
     * Tipo de asignación realizada.
     * Valores: DOCENTE_DT2 | DIRECTOR | TRIBUNAL
     */
    @Column(name = "tipo_asignacion", length = 30, nullable = false)
    private String tipoAsignacion;
    /** ID del docente/director/tribunal asignado. */
    @Column(name = "id_asignado", nullable = false)
    private Integer idAsignado;
    /** Nombre completo del asignado (desnormalizado para trazabilidad). */
    @Column(name = "nombre_asignado", length = 200, nullable = false)
    private String nombreAsignado;
    /** Cargo en el tribunal si aplica: PRESIDENTE/VOCAL/SUPLENTE. */
    @Column(name = "cargo", length = 30)
    private String cargo;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_realizado_por", nullable = false)
    private Usuario realizadoPor;
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
    @Column(name = "periodo", length = 100)
    private String periodo;
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
    @PrePersist
    public void prePersist() {
        if (this.fecha == null) {
            this.fecha = LocalDateTime.now();
        }
    }
}