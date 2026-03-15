
package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.time.LocalTime;

// ─────────────────────────────────────────────────────────────
// DTO de configuración — usado tanto para GET como para PUT
// ─────────────────────────────────────────────────────────────
public class BackupDto {

    // ── Configuración ─────────────────────────────────────────────────────────

    public static class ConfiguracionDto {
        private Integer id;
        private String  rutaLocal;
        private Integer cantidadMaxima;
        private Boolean comprimir;
        private Boolean guardarEnDrive;
        private String  driveFolderId;
        private String  tipoRespaldo;
        private Boolean programadoActivo;
        private String  horaProgramada; // "HH:mm" — más fácil para Angular

        public Integer getId()                  { return id; }
        public void    setId(Integer id)        { this.id = id; }

        public String  getRutaLocal()           { return rutaLocal; }
        public void    setRutaLocal(String v)   { this.rutaLocal = v; }

        public Integer getCantidadMaxima()             { return cantidadMaxima; }
        public void    setCantidadMaxima(Integer v)    { this.cantidadMaxima = v; }

        public Boolean getComprimir()            { return comprimir; }
        public void    setComprimir(Boolean v)   { this.comprimir = v; }

        public Boolean getGuardarEnDrive()              { return guardarEnDrive; }
        public void    setGuardarEnDrive(Boolean v)     { this.guardarEnDrive = v; }

        public String  getDriveFolderId()                { return driveFolderId; }
        public void    setDriveFolderId(String v)        { this.driveFolderId = v; }

        public String  getTipoRespaldo()                 { return tipoRespaldo; }
        public void    setTipoRespaldo(String v)         { this.tipoRespaldo = v; }

        public Boolean getProgramadoActivo()             { return programadoActivo; }
        public void    setProgramadoActivo(Boolean v)    { this.programadoActivo = v; }

        public String  getHoraProgramada()               { return horaProgramada; }
        public void    setHoraProgramada(String v)       { this.horaProgramada = v; }
    }

    // ── Historial (respuesta) ──────────────────────────────────────────────────

    public static class HistorialDto {
        private Integer       id;
        private String        nombreArchivo;
        private String        rutaCompleta;
        private String        tipoRespaldo;
        private Long          tamanioBytes;
        private Boolean       enDrive;
        private String        driveFileId;
        private String        estado;
        private String        mensajeError;
        private LocalDateTime fechaCreacion;

        public Integer       getId()                  { return id; }
        public void          setId(Integer id)        { this.id = id; }

        public String        getNombreArchivo()        { return nombreArchivo; }
        public void          setNombreArchivo(String v){ this.nombreArchivo = v; }

        public String        getRutaCompleta()         { return rutaCompleta; }
        public void          setRutaCompleta(String v) { this.rutaCompleta = v; }

        public String        getTipoRespaldo()         { return tipoRespaldo; }
        public void          setTipoRespaldo(String v) { this.tipoRespaldo = v; }

        public Long          getTamanioBytes()         { return tamanioBytes; }
        public void          setTamanioBytes(Long v)   { this.tamanioBytes = v; }

        public Boolean       getEnDrive()              { return enDrive; }
        public void          setEnDrive(Boolean v)     { this.enDrive = v; }

        public String        getDriveFileId()          { return driveFileId; }
        public void          setDriveFileId(String v)  { this.driveFileId = v; }

        public String        getEstado()               { return estado; }
        public void          setEstado(String v)       { this.estado = v; }

        public String        getMensajeError()         { return mensajeError; }
        public void          setMensajeError(String v) { this.mensajeError = v; }

        public LocalDateTime getFechaCreacion()        { return fechaCreacion; }
        public void          setFechaCreacion(LocalDateTime v){ this.fechaCreacion = v; }
    }

    // ── Respuesta del backup ejecutado ────────────────────────────────────────

    public static class EjecucionResultDto {
        private boolean exitoso;
        private String  mensaje;
        private HistorialDto historial;

        public EjecucionResultDto() {}
        public EjecucionResultDto(boolean exitoso, String mensaje, HistorialDto h) {
            this.exitoso   = exitoso;
            this.mensaje   = mensaje;
            this.historial = h;
        }

        public boolean      isExitoso()              { return exitoso; }
        public void         setExitoso(boolean v)    { this.exitoso = v; }

        public String       getMensaje()             { return mensaje; }
        public void         setMensaje(String v)     { this.mensaje = v; }

        public HistorialDto getHistorial()           { return historial; }
        public void         setHistorial(HistorialDto v){ this.historial = v; }
    }
}