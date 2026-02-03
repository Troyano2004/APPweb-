package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tribunal_proyecto",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_proyecto","id_docente"}))
public class tribunalproyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tribunal")
    private Integer idTribunal;

    @ManyToOne
    @JoinColumn(name = "id_proyecto")
    private Proyectotitulacion proyecto;

    @ManyToOne
    @JoinColumn(name = "id_docente")
    private Docente docente;

    @Column(name = "cargo", length = 20)
    private String cargo; // PRESIDENTE/VOCAL/SUPLENTE

}
