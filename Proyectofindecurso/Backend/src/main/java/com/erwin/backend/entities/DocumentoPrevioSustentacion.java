package com.erwin.backend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * Módulo 5 — Checklist de documentos previos requeridos antes de la sustentación final.
 * Todos los campos deben ser true para que el sistema habilite la sustentación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documento_previo_sustentacion",
        uniqueConstraints = @UniqueConstraint(name = "uq_doc_previo_proyecto",
                columnNames = "id_proyecto"))
public class DocumentoPrevioSustentacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doc_previo")
    private Integer idDocPrevio;
    @OneToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private ProyectoTitulacion proyecto;
    /** Ejemplar impreso y empastado entregado. */
    @Column(name = "ejemplar_impreso", nullable = false)
    private Boolean ejemplarImpreso = false;
    /** Copia digital para biblioteca entregada. */
    @Column(name = "copia_digital_biblioteca", nullable = false)
    private Boolean copiaDigitalBiblioteca = false;
    /** 4 copias digitales para tribunal y director entregadas. */
    @Column(name = "copias_digitales_tribunal", nullable = false)
    private Boolean copiasDigitalesTribunal = false;
    /** Informe COMPILATIO firmado entregado. */
    @Column(name = "informe_compilatio_firmado", nullable = false)
    private Boolean informeCompilatioFirmado = false;
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
    @ManyToOne
    @JoinColumn(name = "id_registrado_por")
    private Usuario registradoPor;
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
    /** true cuando los 4 documentos están entregados y la sustentación puede programarse. */
    @Column(name = "completo", nullable = false)
    private Boolean completo = false;
    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        recalcularCompleto();
    }
    @PreUpdate
    public void preUpdate() {
        recalcularCompleto();
    }
    public void recalcularCompleto() {
        this.completo = Boolean.TRUE.equals(ejemplarImpreso)
                && Boolean.TRUE.equals(copiaDigitalBiblioteca)
                && Boolean.TRUE.equals(copiasDigitalesTribunal)
                && Boolean.TRUE.equals(informeCompilatioFirmado);
    }
}