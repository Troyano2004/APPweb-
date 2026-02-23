package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aval_director")
public class AvalDirector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aval")
    private Integer idAval;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_documento_titulacion", nullable = false, unique = true)
    private DocumentoTitulacion documento;

    @Column(name = "url_pdf", length = 300, nullable = false)
    private String urlPdf;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}