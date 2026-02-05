package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comision_miembro")
public class ComisionMiembro {
    @EmbeddedId
    private comisionmiembroid id;

    @ManyToOne(optional = false)
    @MapsId("idComision")
    @JoinColumn(name = "id_comision")
    private ComisionFormativa idcomision;

    @ManyToOne(optional = false)
    @MapsId("idDocente")
    @JoinColumn(name = "id_docente")
    private Docente docente;

    @Column(name = "cargo", length = 20, nullable = false)
    private String cargo; // PRESIDENTE/VOCAL/SECRETARIO

    @Column(name = "fecha_designacion", nullable = false)
    private LocalDate fechaDesignacion = LocalDate.now();
}

