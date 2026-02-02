package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "proyecto_documento",
        uniqueConstraints = @UniqueConstraint(name="uq_version_proyecto", columnNames = {"id_proyecto","numero_version"}))
public class proyectodocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyectotitulacion proyecto;

    @Column(name = "tipo", length = 30, nullable = false)
    private String tipo; // BORRADOR/DEFENSA/FINAL_BIBLIOTECA

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "url_archivo", length = 400, nullable = false)
    private String urlArchivo;

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "abstract", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "palabras_clave", columnDefinition = "TEXT")
    private String palabrasClave;

    @Column(name = "codigo_dublin", length = 100)
    private String codigoDublin;

    @Column(name = "formato", length = 20)
    private String formato;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida = LocalDateTime.now();

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
