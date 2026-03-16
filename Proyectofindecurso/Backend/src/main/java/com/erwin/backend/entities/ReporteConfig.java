package com.erwin.backend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte_config", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReporteConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String tipo = "TEXT";

    @Column(name = "fecha_actualiz")
    private LocalDateTime fechaActualiz;

    @PrePersist @PreUpdate
    public void actualizar() { this.fechaActualiz = LocalDateTime.now(); }
}
