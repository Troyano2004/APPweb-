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
@Table(name = "dt1_revision")
public class Dt1Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision")
    private Integer idRevision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_anteproyecto", nullable = false)
    private AnteproyectoTitulacion anteproyecto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_version", nullable = false)
    private Anteproyectotitulacionversion version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @Column(name = "decision", length = 20, nullable = false)
    private String decision;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_revision", insertable = false, updatable = false)
    private LocalDateTime fechaRevision;
}