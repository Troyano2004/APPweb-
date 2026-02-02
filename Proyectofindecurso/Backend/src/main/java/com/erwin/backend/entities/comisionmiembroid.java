package com.erwin.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class comisionmiembroid {
    @Column(name = "id_comision")
    private Integer idComision;

    @Column(name = "id_docente")
    private Integer idDocente;
}
