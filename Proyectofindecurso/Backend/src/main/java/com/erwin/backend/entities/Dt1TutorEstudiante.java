package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "dt1_tutor_estudiante",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_estudiante", "id_periodo"})
)
public class Dt1TutorEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor_estudiante")
    private Integer idTutorEstudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}