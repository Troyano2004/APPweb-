package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sustentacion_final")
public class SustentacionFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sustentacion_final")
    private Integer idSustentacionFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "lugar", nullable = false, length = 255)
    private String lugar;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "es_segunda_oportunidad", nullable = false)
    private Boolean esSegundaOportunidad = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_realizado_por")
    private Usuario realizadoPor;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
    }

    // ── Getters y Setters ──────────────────────────────────

    public Integer getIdSustentacionFinal()            { return idSustentacionFinal; }
    public void setIdSustentacionFinal(Integer v)      { this.idSustentacionFinal = v; }

    public ProyectoTitulacion getProyecto()            { return proyecto; }
    public void setProyecto(ProyectoTitulacion v)      { this.proyecto = v; }

    public LocalDate getFecha()                        { return fecha; }
    public void setFecha(LocalDate v)                  { this.fecha = v; }

    public LocalTime getHora()                         { return hora; }
    public void setHora(LocalTime v)                   { this.hora = v; }

    public String getLugar()                           { return lugar; }
    public void setLugar(String v)                     { this.lugar = v; }

    public String getObservaciones()                   { return observaciones; }
    public void setObservaciones(String v)             { this.observaciones = v; }

    public Boolean getEsSegundaOportunidad()           { return esSegundaOportunidad; }
    public void setEsSegundaOportunidad(Boolean v)     { this.esSegundaOportunidad = v; }

    public Usuario getRealizadoPor()                   { return realizadoPor; }
    public void setRealizadoPor(Usuario v)             { this.realizadoPor = v; }

    public LocalDateTime getFechaRegistro()            { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime v)      { this.fechaRegistro = v; }
}