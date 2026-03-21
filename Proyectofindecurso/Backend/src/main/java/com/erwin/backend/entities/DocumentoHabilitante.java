
package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "documentos_habilitantes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_proyecto_tipo",
                        columnNames = {"id_proyecto", "tipo_documento"}),
                @UniqueConstraint(name = "uq_complexivo_tipo",
                        columnNames = {"id_complexivo", "tipo_documento"})
        }
)
public class DocumentoHabilitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ── Relaciones ─────────────────────────────────────────────
    /** Para estudiantes de Integración Curricular (TIC) */
    @ManyToOne(optional = true)
    @JoinColumn(name = "id_proyecto", nullable = true)
    private ProyectoTitulacion proyecto;

    /** Para estudiantes de Examen Complexivo */
    @ManyToOne(optional = true)
    @JoinColumn(name = "id_complexivo", nullable = true)
    private ComplexivoTitulacion complexivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    /** Docente que valida */
    @ManyToOne
    @JoinColumn(name = "id_validado_por")
    private Docente validadoPor;

    // ── Tipo ───────────────────────────────────────────────────
    @Column(name = "tipo_documento", length = 60, nullable = false)
    private String tipoDocumento;

    // ── Archivo ────────────────────────────────────────────────
    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "formato", length = 10)
    private String formato = "PDF";

    // ── Antiplagio ─────────────────────────────────────────────
    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    @Column(name = "umbral_permitido", precision = 5, scale = 2)
    private BigDecimal umbralPermitido = new BigDecimal("10.00");

    @Column(name = "resultado_antiplagio", length = 20)
    private String resultadoAntiplagio;

    // ── Flujo ──────────────────────────────────────────────────
    @Column(name = "estado", length = 30, nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "comentario_validacion", columnDefinition = "TEXT")
    private String comentarioValidacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    // ── Auditoría ──────────────────────────────────────────────
    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        this.fechaSubida   = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}