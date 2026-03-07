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
@Table(name ="asesoria_director")
public class AsesoriaDirector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asesoria")
    private Integer idAsesoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private ProyectoTitulacion proyecto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_director", nullable = false)
    private Docente director;

    @Column(name="fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "observaciones", nullable = false, columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;
    /** Porcentaje de avance del proyecto en esta asesoría (0-100). */
    @Column(name = "porcentaje_avance", precision = 5, scale = 2)
    private BigDecimal porcentajeAvance;
    /** Corte evaluativo al que pertenece: 1 o 2. */
    @Column(name = "numero_corte")
    private Integer numeroCorte;
    /** Calificación asignada por el director en esta asesoría (sobre 10, opcional). */
    @Column(name = "calificacion", precision = 4, scale = 2)
    private BigDecimal calificacion;
}
