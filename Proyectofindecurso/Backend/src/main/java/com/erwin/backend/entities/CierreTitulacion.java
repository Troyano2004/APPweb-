package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cierre_titulacion")
public class CierreTitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cierre")
    private Integer idCierre;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_documento_titulacion", nullable = false, unique = true)
    private DocumentoTitulacion documento;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_sustentacion", nullable = false, unique = true)
    private Sustentacion sustentacion;

    @Column(name = "nota_docente", precision = 5, scale = 2, nullable = false)
    private BigDecimal notaDocente;

    @Column(name = "nota_tribunal", precision = 5, scale = 2, nullable = false)
    private BigDecimal notaTribunal;

    @Column(name = "nota_final", precision = 5, scale = 2, nullable = false)
    private BigDecimal notaFinal;

    @Column(name = "resultado", length = 20, nullable = false)
    private String resultado;

    @Column(name = "acta_url", length = 300, nullable = false)
    private String actaUrl;

    @Column(name = "acta_firmada_url", length = 300)
    private String actaFirmadaUrl;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "cerrado", nullable = false)
    private Boolean cerrado = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}