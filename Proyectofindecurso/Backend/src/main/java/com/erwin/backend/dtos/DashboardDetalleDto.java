package com.erwin.backend.dtos;

import java.util.ArrayList;
import java.util.List;

public class DashboardDetalleDto {
    private List<DashboardItemDto> alertas = new ArrayList<>();
    private List<DashboardItemDto> actividades = new ArrayList<>();

    public DashboardDetalleDto() {
    }

    public DashboardDetalleDto(List<DashboardItemDto> alertas, List<DashboardItemDto> actividades) {
        this.alertas = alertas;
        this.actividades = actividades;
    }

    public List<DashboardItemDto> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<DashboardItemDto> alertas) {
        this.alertas = alertas;
    }

    public List<DashboardItemDto> getActividades() {
        return actividades;
    }

    public void setActividades(List<DashboardItemDto> actividades) {
        this.actividades = actividades;
    }
}
