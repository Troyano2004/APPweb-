package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backup_jobs")
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_job")
    private Long idJob;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "pg_host", nullable = false, length = 200)
    private String pgHost;

    @Column(name = "pg_port", nullable = false)
    private Integer pgPort = 5432;

    @Column(name = "pg_dump_path", length = 500)
    private String pgDumpPath;

    @Column(name = "pg_usuario", nullable = false, length = 100)
    private String pgUsuario;

    @Column(name = "pg_password_enc", nullable = false, columnDefinition = "TEXT")
    private String pgPasswordEnc;

    @Column(name = "databases", nullable = false, length = 500)
    private String databases;

    @Column(name = "comprimir")
    private Boolean comprimir = true;

    @Column(name = "cron_full", nullable = false, length = 100)
    private String cronFull;

    @Column(name = "cron_diferencial", length = 100)
    private String cronDiferencial;

    @Column(name = "diferencial_activo")
    private Boolean diferencialActivo = false;

    @Column(name = "zona_horaria", length = 60)
    private String zonaHoraria = "America/Guayaquil";

    @Column(name = "ventana_excluir_inicio", length = 5)
    private String ventanaExcluirInicio;

    @Column(name = "ventana_excluir_fin", length = 5)
    private String ventanaExcluirFin;

    @Column(name = "max_reintentos")
    private Integer maxReintentos = 2;

    @Column(name = "email_exito", length = 500)
    private String emailExito;

    @Column(name = "email_fallo", length = 500)
    private String emailFallo;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "proxima_ejecucion")
    private LocalDateTime proximaEjecucion;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @JsonManagedReference("job-destinos")
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackupDestination> destinos = new ArrayList<>();

    @JsonManagedReference("job-ejecuciones")
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackupExecution> ejecuciones = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.creadoEn      = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}