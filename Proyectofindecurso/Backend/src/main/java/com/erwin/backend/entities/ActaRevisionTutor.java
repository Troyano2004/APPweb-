package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="acta_revision_director")
public class ActaRevisionTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_acta")
    private Integer idActa;

    @OneToOne(optional=false)
    @JoinColumn(name="id_tutoria", nullable=false, unique=true)
    private TutoriaAnteproyecto tutoria;

    @Column(name="director_nombre", length=200, nullable=false)
    private String directorNombre;

    @Column(name="director_cargo", length=200, nullable=false)
    private String directorCargo;

    @Column(name="director_firma", length=200)
    private String directorFirma;

    @Column(name="estudiante_nombre", length=200, nullable=false)
    private String estudianteNombre;

    @Column(name="estudiante_cargo", length=200, nullable=false)
    private String estudianteCargo;

    @Column(name="estudiante_firma", length=200)
    private String estudianteFirma;

    @Column(name="titulo_proyecto", length=600, nullable=false)
    private String tituloProyecto;

    @Column(name="objetivo", columnDefinition="TEXT", nullable=false)
    private String objetivo;

    @Column(name="detalle_revision", columnDefinition="TEXT", nullable=false)
    private String detalleRevision;

    @Column(name="cumplimiento", length=20, nullable=false)
    private String cumplimiento; // NINGUNO/PARCIAL/COMPLETO

    @Column(name="conclusion", columnDefinition="TEXT", nullable=false)
    private String conclusion;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="observaciones", columnDefinition="TEXT")
    private String observaciones;
}