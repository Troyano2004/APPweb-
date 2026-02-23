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
@Table(name = "sustentacion_reprogramacion")
public class SustentacionReprogramacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reprogramacion")
    private Integer idReprogramacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sustentacion", nullable = false)
    private Sustentacion sustentacion;

    @Column(name = "fecha_anterior", nullable = false, length = 10)
    private String fechaAnterior;

    @Column(name = "hora_anterior", nullable = false, length = 8)
    private String horaAnterior;

    @Column(name = "lugar_anterior", nullable = false, length = 150)
    private String lugarAnterior;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
    }
}