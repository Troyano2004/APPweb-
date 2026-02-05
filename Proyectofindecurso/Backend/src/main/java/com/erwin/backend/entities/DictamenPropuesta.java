package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "dictamen_propuesta")
public class DictamenPropuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dictamen")
    private Integer idDictamen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_propuesta")
    private PropuestaTitulacion propuesta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente")
    private Docente docente;

    @Column(name = "decision", length = 15, nullable = false)
    private String decision; // APROBADA/RECHAZADA/OBSERVADA

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_dictamen", nullable = false)
    private LocalDateTime fechaDictamen = LocalDateTime.now();
}
