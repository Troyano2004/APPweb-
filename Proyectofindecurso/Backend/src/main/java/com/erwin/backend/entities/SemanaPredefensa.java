package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "semana_predefensa")
public class SemanaPredefensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_semana")
    private Integer idSemana;

    // Rango de fechas de la semana de predefensas
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    // Horario diario de atención
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // Duración de cada predefensa en minutos
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // Lugar por defecto (puede sobreescribirse por slot)
    @Column(name = "lugar_defecto", length = 200)
    private String lugarDefecto;

    // Observaciones generales
    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // Período académico al que pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo")
    private PeriodoTitulacion periodo;

    @Column(name = "activo")
    private Boolean activo = true;
}