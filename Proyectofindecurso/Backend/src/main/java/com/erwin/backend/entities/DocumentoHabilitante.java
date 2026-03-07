package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Documentos habilitantes para la sustentación del Trabajo de Integración Curricular.
 * Arts. 10, 11, 57 num.2 y 59 del Reglamento UIC-UTEQ (sept-2024).
 *
 * ✅ NO necesitas SQL manual.
 *    Hibernate crea/actualiza esta tabla automáticamente al arrancar
 *    porque tienes: spring.jpa.hibernate.ddl-auto=update
 *    Solo copia esta entidad a tu carpeta entities/ y levanta el backend.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "documentos_habilitantes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_proyecto_tipo",
                columnNames = {"id_proyecto", "tipo_documento"}
        )
)
public class DocumentoHabilitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ── Relaciones ─────────────────────────────────────────────────────────────
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    /** Docente que valida (Director del TIC para INFORME_DIRECTOR) */
    @ManyToOne
    @JoinColumn(name = "id_validado_por")
    private Docente validadoPor;

    // ── Tipo ──────────────────────────────────────────────────────────────────
    /**
     * Catálogo fijo según reglamento:
     * INFORME_DIRECTOR, CERTIFICADO_ANTIPLAGIO, TRABAJO_FINAL_PDF,
     * CERTIFICADO_PENSUM, CERTIFICADO_DEUDAS, CERTIFICADO_IDIOMA, CERTIFICADO_PRACTICAS
     */
    @Column(name = "tipo_documento", length = 60, nullable = false)
    private String tipoDocumento;

    // ── Archivo ───────────────────────────────────────────────────────────────
    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    /** URL pública en Azure Blob Storage */
    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "formato", length = 10)
    private String formato = "PDF";

    // ── Antiplagio (solo para CERTIFICADO_ANTIPLAGIO) ─────────────────────────
    /** Porcentaje de coincidencia reportado por COMPILATIO */
    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    /** Umbral permitido por reglamento: 10% (Art. 57 num.2) */
    @Column(name = "umbral_permitido", precision = 5, scale = 2)
    private BigDecimal umbralPermitido = new BigDecimal("10.00");

    /** APROBADO si porcentaje ≤ umbral, RECHAZADO si supera */
    @Column(name = "resultado_antiplagio", length = 20)
    private String resultadoAntiplagio;

    // ── Flujo ─────────────────────────────────────────────────────────────────
    /** PENDIENTE → ENVIADO → APROBADO | RECHAZADO */
    @Column(name = "estado", length = 30, nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "comentario_validacion", columnDefinition = "TEXT")
    private String comentarioValidacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    // ── Auditoría ─────────────────────────────────────────────────────────────
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