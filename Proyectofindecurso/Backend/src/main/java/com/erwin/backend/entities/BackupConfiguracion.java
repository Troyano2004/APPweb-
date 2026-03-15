package com.erwin.backend.entities;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_configuracion")
public class BackupConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ruta_local", nullable = false, length = 500)
    private String rutaLocal;

    @Column(name = "cantidad_maxima", nullable = false)
    private Integer cantidadMaxima = 10;

    @Column(nullable = false)
    private Boolean comprimir = true;

    @Column(name = "guardar_en_drive", nullable = false)
    private Boolean guardarEnDrive = false;

    @Column(name = "drive_folder_id", length = 255)
    private String driveFolderId;

    @Column(name = "tipo_respaldo", nullable = false, length = 20)
    private String tipoRespaldo = "COMPLETO";

    @Column(name = "programado_activo", nullable = false)
    private Boolean programadoActivo = false;

    @Column(name = "hora_programada")
    private LocalTime horaProgramada;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    public void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // ── Getters y Setters ──────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getRutaLocal() { return rutaLocal; }
    public void setRutaLocal(String rutaLocal) { this.rutaLocal = rutaLocal; }

    public Integer getCantidadMaxima() { return cantidadMaxima; }
    public void setCantidadMaxima(Integer cantidadMaxima) { this.cantidadMaxima = cantidadMaxima; }

    public Boolean getComprimir() { return comprimir; }
    public void setComprimir(Boolean comprimir) { this.comprimir = comprimir; }

    public Boolean getGuardarEnDrive() { return guardarEnDrive; }
    public void setGuardarEnDrive(Boolean guardarEnDrive) { this.guardarEnDrive = guardarEnDrive; }

    public String getDriveFolderId() { return driveFolderId; }
    public void setDriveFolderId(String driveFolderId) { this.driveFolderId = driveFolderId; }

    public String getTipoRespaldo() { return tipoRespaldo; }
    public void setTipoRespaldo(String tipoRespaldo) { this.tipoRespaldo = tipoRespaldo; }

    public Boolean getProgramadoActivo() { return programadoActivo; }
    public void setProgramadoActivo(Boolean programadoActivo) { this.programadoActivo = programadoActivo; }

    public LocalTime getHoraProgramada() { return horaProgramada; }
    public void setHoraProgramada(LocalTime horaProgramada) { this.horaProgramada = horaProgramada; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}