package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoObservacion;
import com.erwin.backend.enums.SeccionDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observacion_documento")
public class ObservacionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observacion")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_documento_titulacion", nullable = false)
    private DocumentoTitulacion documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "seccion", length = 40, nullable = false)
    private SeccionDocumento seccion;

    @Column(name = "comentario", columnDefinition = "TEXT", nullable = false)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoObservacion estado = EstadoObservacion.PENDIENTE;

    /**
     * Autor de la observación (normalmente el director).
     * Si prefieres registrar por Usuario, cambia Docente -> Usuario.
     */
    @ManyToOne
    @JoinColumn(name = "id_autor")
    private Docente autor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}
