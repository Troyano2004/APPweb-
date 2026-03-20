package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackupStatsDto {

    // Jobs
    private long jobsActivos;
    private long jobsTotal;

    // Ejecuciones último mes
    private long totalEjecucionesMes;
    private long exitososMes;
    private long fallidosMes;
    private double tasaExitoMes;        // 0-100

    // Tamaños
    private long tamanoAcumuladoBytes;  // total de todos los backups exitosos
    private long tamanoUltimoBackup;

    // Tiempo
    private LocalDateTime ultimoBackupFecha;
    private String         ultimoBackupEstado;
    private String         ultimoBackupJob;
    private LocalDateTime  proximaEjecucion;
    private String         proximaEjecucionJob;

    // Integridad
    private long archivosVerificados;
    private long archivosCorruptos;
}