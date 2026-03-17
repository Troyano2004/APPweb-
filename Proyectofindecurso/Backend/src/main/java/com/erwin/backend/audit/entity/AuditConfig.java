package com.erwin.backend.audit.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "audit_config", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuditConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 100) private String entidad;
    @Column(nullable = false, length = 50)  private String accion;
    @Column(nullable = false) private Boolean activo = true;
    @Column(name = "notificar_email", nullable = false) private Boolean notificarEmail = false;
    @Column(columnDefinition = "TEXT[]")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    private List<String> destinatarios;
    @Column(nullable = false, length = 20) private String severidad = "LOW";
    @Column(length = 255) private String descripcion;
    @Column(name = "fecha_creacion") private LocalDateTime fechaCreacion;
    @Column(name = "fecha_actualiz") private LocalDateTime fechaActualiz;
    @PrePersist public void prePersist() { fechaCreacion = fechaActualiz = LocalDateTime.now(); }
    @PreUpdate  public void preUpdate()  { fechaActualiz = LocalDateTime.now(); }
}
