package com.erwin.backend.dtos;

import java.time.LocalDateTime;

public class DashboardItemDto {
    private String mensaje;
    private LocalDateTime fecha;

    public DashboardItemDto() {
    }

    public DashboardItemDto(String mensaje, LocalDateTime fecha) {
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
