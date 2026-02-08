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
@Table(name = "comision_proyecto")
public class ComisionProyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comision_proyecto")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_comision")
    private ComisionFormativa comision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto")
    private ProyectoTitulacion proyecto;

    @Column(name = "fecha_conformacion", nullable = false)
    private LocalDate fechaConformacion;

    @Column(name = "resolucion_acta", length = 120)
    private String resolucionActa;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "estado", length = 25, nullable = false)
    private String estado; // COMISION_NO_CONFORMADA/COMISION_CONFORMADA
}
