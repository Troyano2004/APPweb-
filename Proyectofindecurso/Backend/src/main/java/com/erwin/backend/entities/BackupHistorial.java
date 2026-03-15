
package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_historial")
public class BackupHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_archivo", nullable = false, length = 500)
    private String nombreArchivo;

    @Column(name = "ruta_completa", length = 1000)
    private String rutaCompleta;

    @Column(name = "tipo_respaldo", nullable = false, length = 20)
    private String tipoRespaldo;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "en_drive", nullable = false)
    private Boolean enDrive = false;

    @Column(name = "drive_file_id", length = 255)
    private String driveFileId;

    @Column(nullable = false, length = 20)
    private String estado = "EXITOSO";

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

    // ── Getters y Setters ──────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaCompleta() { return rutaCompleta; }
    public void setRutaCompleta(String rutaCompleta) { this.rutaCompleta = rutaCompleta; }

    public String getTipoRespaldo() { return tipoRespaldo; }
    public void setTipoRespaldo(String tipoRespaldo) { this.tipoRespaldo = tipoRespaldo; }

    public Long getTamanioBytes() { return tamanioBytes; }
    public void setTamanioBytes(Long tamanioBytes) { this.tamanioBytes = tamanioBytes; }

    public Boolean getEnDrive() { return enDrive; }
    public void setEnDrive(Boolean enDrive) { this.enDrive = enDrive; }

    public String getDriveFileId() { return driveFileId; }
    public void setDriveFileId(String driveFileId) { this.driveFileId = driveFileId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}