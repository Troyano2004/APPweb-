package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class BackupTimelineDtos {

    /**
     * Un nodo FULL con sus diferenciales hijos — estructura para el git-log visual
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BackupCadenaDto {
        private BackupNodoDto full;
        private List<BackupNodoDto> diferenciales;
        private int totalDiferenciales;
        private long tamanoTotalBytes;  // FULL + todos sus diferenciales
    }

    /**
     * Un nodo individual (FULL o DIFERENCIAL)
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BackupNodoDto {
        private Long          idExecution;
        private String        tipo;           // FULL / DIFERENCIAL
        private String        estado;
        private String        databaseNombre;
        private LocalDateTime iniciadoEn;
        private LocalDateTime finalizadoEn;
        private Long          duracionSegundos;
        private Long          tamanoBytes;
        private String        archivoNombre;
        private Boolean       archivoDisponible;
        private String        destinoTipo;
        private String        tablasIncluidas;  // Solo en DIFERENCIAL
        private Long          idBackupPadre;    // Solo en DIFERENCIAL
        private Boolean       manual;
        private String        errorMensaje;
        private int           numeroDiferenciales; // Solo en FULL: cuántos diff tiene
    }

    /**
     * Respuesta completa de la línea de tiempo para un job
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TimelineResponseDto {
        private Long   idJob;
        private String nombreJob;
        private List<BackupCadenaDto> cadenas; // Ordenadas por fecha desc (más reciente primero)
        private int    totalFull;
        private int    totalDiferenciales;
        private long   tamanoTotalBytes;
    }
}