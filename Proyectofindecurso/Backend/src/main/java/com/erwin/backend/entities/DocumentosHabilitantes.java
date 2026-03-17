
package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
                @Index(name = "idx_doc_hab_proyecto", columnList = "id_proyecto"),
                @Index(name = "idx_doc_hab_estudiante", columnList = "id_estudiante")
        }
)
@Getter
@Setter
public class DocumentosHabilitantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "tipo_documento", nullable = false, length = 60)
    private String tipoDocumento;

    @Column(name = "aplica_modalidad", length = 30)
    private String aplicaModalidad;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "formato", length = 10)
    private String formato;

    @Column(name = "porcentaje_coincidencia", precision = 5, scale = 2)
    private BigDecimal porcentajeCoincidencia;

    @Column(name = "umbral_permitido", precision = 5, scale = 2)
    private BigDecimal umbralPermitido;

    @Column(name = "resultado_antiplagio", length = 20)
    private String resultadoAntiplagio;

    @Column(name = "comentario_validacion")
    private String comentarioValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false,
            foreignKey = @ForeignKey(name = "fk31a7sxlsw3t0a2t91gvfrvg83"))
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false,
            foreignKey = @ForeignKey(name = "fk28v2a4tk6kut57m5deud6j6m0"))
    private ProyectoTitulacion proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_validado_por", nullable = true,
            foreignKey = @ForeignKey(name = "fkii16y2rcliunf6gdlw1jttvfi"))
    private Docente validadoPor;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();

        if (this.fechaSubida == null) {
            this.fechaSubida = ahora;
        }

        if (this.actualizadoEn == null) {
            this.actualizadoEn = ahora;
        }

        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "PENDIENTE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}