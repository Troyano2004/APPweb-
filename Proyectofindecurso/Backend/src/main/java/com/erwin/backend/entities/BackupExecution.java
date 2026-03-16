package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backup_executions")
public class BackupExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_execution")
    private Long idExecution;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_job", nullable = false)
    private BackupJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEjecucion estado = EstadoEjecucion.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_backup", nullable = false, length = 20)
    private TipoBackup tipoBackup = TipoBackup.FULL;

    @Column(name = "database_nombre", length = 200)
    private String databaseNombre;

    @Column(name = "archivo_ruta", length = 1000)
    private String archivoRuta;

    @Column(name = "archivo_nombre", length = 300)
    private String archivoNombre;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "destino_tipo", length = 20)
    private String destinoTipo;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "log_detalle", columnDefinition = "TEXT")
    private String logDetalle;

    @Column(name = "intento_numero")
    private Integer intentoNumero = 1;

    @Column(name = "iniciado_en")
    private LocalDateTime iniciadoEn;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    @Column(name = "manual")
    private Boolean manual = false;

    @PrePersist
    public void prePersist() {
        this.iniciadoEn = LocalDateTime.now();
    }

    public enum EstadoEjecucion {
        PENDIENTE, EN_PROCESO, EXITOSO, FALLIDO
    }

    public enum TipoBackup {
        FULL, DIFERENCIAL
    }
}