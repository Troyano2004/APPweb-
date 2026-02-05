package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "nota_director_corte",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_proyecto","numero_corte"}))
public class NotaDirectorCorte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota")
    private Integer idNota;

    @ManyToOne
    @JoinColumn(name = "id_proyecto")
    private ProyectoTitulacion proyecto;

    @Column(name = "numero_corte")
    private Integer numeroCorte; // 1 o 2

    @Column(name = "nota", precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(name = "fecha")
    private LocalDateTime fecha = LocalDateTime.now();

}
