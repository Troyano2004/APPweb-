package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documento_titulacion")
public class DocumentoTitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_titulacion")
    private Integer id;

    /**
     * Si tu sistema maneja "ProyectoTitulacion" como contenedor,
     * esta relación es la más correcta.
     * Si aún no quieres usar proyecto, la quitas y usas Estudiante directo.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private ProyectoTitulacion proyecto;

    /**
     * Estudiante dueño (redundante si el proyecto ya tiene estudiante,
     * pero útil para consultas rápidas).
     * Si te molesta la redundancia, puedes eliminar este campo y obtenerlo desde proyecto.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    /**
     * Director/revisor asignado (docente)
     */
    @ManyToOne
    @JoinColumn(name = "id_director")
    private Docente director;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    // ====== Portada / datos estructurados ======
    @Column(name = "titulo", length = 300, nullable = false)
    private String titulo;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "anio")
    private Integer anio;

    // ====== Secciones (texto largo) ======
    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    // No uses @Column(name="abstract") porque "abstract" puede dar lío.
    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "introduccion", columnDefinition = "TEXT")
    private String introduccion;

    @Column(name = "planteamiento_problema", columnDefinition = "TEXT")
    private String planteamientoProblema;

    @Column(name = "objetivo_general", columnDefinition = "TEXT")
    private String objetivoGeneral;

    @Column(name = "objetivos_especificos", columnDefinition = "TEXT")
    private String objetivosEspecificos;

    @Column(name = "justificacion", columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "marco_teorico", columnDefinition = "TEXT")
    private String marcoTeorico;

    @Column(name = "metodologia", columnDefinition = "TEXT")
    private String metodologia;

    @Column(name = "resultados", columnDefinition = "TEXT")
    private String resultados;

    @Column(name = "discusion", columnDefinition = "TEXT")
    private String discusion;

    @Column(name = "conclusiones", columnDefinition = "TEXT")
    private String conclusiones;

    @Column(name = "recomendaciones", columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "bibliografia", columnDefinition = "TEXT")
    private String bibliografia;

    @Column(name = "anexos", columnDefinition = "TEXT")
    private String anexos;

    // Auditoría
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
