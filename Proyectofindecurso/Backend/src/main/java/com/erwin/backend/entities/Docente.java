package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "docente")
public class Docente {
    @Id
    @Column(name = "id_docente")
    private Integer idDocente;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "id_docente")
    private Usuario usuario;

    @Column(name = "titulo_4to_nivel", length = 200)
    private String titulo4toNivel;
}