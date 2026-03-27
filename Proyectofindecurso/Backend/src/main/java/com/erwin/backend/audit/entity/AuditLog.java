package com.erwin.backend.audit.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "audit_log", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "config_id")
    private AuditConfig config;
    @Column(nullable = false, length = 100) private String entidad;
    @Column(name = "entidad_id", length = 100) private String entidadId;
    @Column(nullable = false, length = 50) private String accion;
    @Column(name = "id_usuario") private Integer idUsuario;
    @Column(length = 50)  private String username;
    @Column(name = "correo_usuario", length = 100) private String correoUsuario;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "estado_anterior")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String estadoAnterior;

    @Column(name = "estado_nuevo")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String estadoNuevo;

    @Column(name = "metadata")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;
    @Column(name = "timestamp_evento") private LocalDateTime timestampEvento;
    @Column(name = "duracion_ms") private Integer duracionMs;
    @PrePersist public void prePersist() { if (timestampEvento == null) timestampEvento = LocalDateTime.now(); }
}