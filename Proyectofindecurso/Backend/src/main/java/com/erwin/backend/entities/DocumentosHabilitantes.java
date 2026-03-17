package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla documentos_habilitantes.
 *
 * Cuando se recrea la BD desde cero con ddl-auto=update o create,
 * Hibernate crea automáticamente la columna aplica_modalidad
 * con el CHECK constraint incluido.
 *
 * Relaciones:
 *   id_estudiante → estudiante (ManyToOne)
 *   id_proyecto   → proyecto_titulacion (ManyToOne)
 *   id_validado_por → docente (ManyToOne, nullable)
 */
@Entity
@Table(
        name = "documentos_habilitantes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_proyecto_tipo",
                        columnNames = { "id_proyecto", "tipo_documento" }
                )
        },
        indexes = {
                @Index(name = "idx_doc_hab_proyecto",   columnList = "id_proyecto"),
                @Index(name = "idx_doc_hab_estudiante",  columnList = "id_estudiante")
        }
)
@Getter
@Setter
public class DocumentosHabilitantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ─── Timestamps ────────────────────────────────────────────────────────

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    // ─── Tipo y estado del documento ───────────────────────────────────────

    /**
     * Tipo de documento. Valores válidos:
     *   CERTIFICADO_PENSUM, CERTIFICADO_DEUDAS, CERTIFICADO_IDIOMA,
     *   CERTIFICADO_PRACTICAS                  → aplican a AMBAS modalidades
     *   CERTIFICADO_ANTIPLAGIO, INFORME_DIRECTOR, TRABAJO_FINAL_PDF
     *                                          → solo TRABAJO_INTEGRACION
     *   INFORME_PRACTICO_COMPLEXIVO            → solo EXAMEN_COMPLEXIVO
     */
    @Column(name = "tipo_documento", nullable = false, length = 60)
    private String tipoDocumento;

    /**
     * Indica a qué modalidad aplica este documento.
     * Valores válidos: TRABAJO_INTEGRACION | EXAMEN_COMPLEXIVO | AMBAS
     *
     * Esta columna garantiza que, si la BD se recrea desde cero,
     * Hibernate la crea automáticamente desde esta entidad.
     * El trigger fn_validar_documento_por_modalidad la puebla
     * automáticamente en cada INSERT si viene NULL.
     */
    @Column(name = "aplica_modalidad", length = 30)
    private String aplicaModalidad;

    /**
     * Estado del documento.
     * Valores usados: PENDIENTE | EN_REVISION | APROBADO | RECHAZADO
     */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    // ─── Datos del archivo ─────────────────────────────────────────────────

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "formato", length = 10)
    private String formato;

    // ─── Datos de antiplagio (solo para CERTIFICADO_ANTIPLAGIO) ───────────

    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    @Column(name = "umbral_permitido", precision = 5, scale = 2)
    private BigDecimal umbralPermitido;

    @Column(name = "resultado_antiplagio", length = 20)
    private String resultadoAntiplagio;

    // ─── Validación ────────────────────────────────────────────────────────

    @Column(name = "comentario_validacion")
    private String comentarioValidacion;

    // ─── Relaciones ────────────────────────────────────────────────────────

    /**
     * Estudiante propietario del documento.
     * FK: id_estudiante → estudiante.id_estudiante
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false,
            foreignKey = @ForeignKey(name = "fk31a7sxlsw3t0a2t91gvfrvg83"))
    private Estudiante estudiante;

    /**
     * Proyecto de titulación al que pertenece el documento.
     * FK: id_proyecto → proyecto_titulacion.id_proyecto
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false,
            foreignKey = @ForeignKey(name = "fk28v2a4tk6kut57m5deud6j6m0"))
    private ProyectoTitulacion proyecto;

    /**
     * Docente que validó/aprobó el documento. Puede ser null si aún no se valida.
     * FK: id_validado_por → docente.id_docente
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_validado_por", nullable = true,
            foreignKey = @ForeignKey(name = "fkii16y2rcliunf6gdlw1jttvfi"))
    private Docente validadoPor;
}