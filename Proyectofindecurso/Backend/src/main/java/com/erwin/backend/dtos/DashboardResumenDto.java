package com.erwin.backend.dtos;

public class DashboardResumenDto {
    private long propuestasPendientes;
    private long tutoriasActivas;
    private long proyectosAprobados;
    private long documentosPendientes;

    public DashboardResumenDto() {
    }

    public DashboardResumenDto(long propuestasPendientes,
                               long tutoriasActivas,
                               long proyectosAprobados,
                               long documentosPendientes) {
        this.propuestasPendientes = propuestasPendientes;
        this.tutoriasActivas = tutoriasActivas;
        this.proyectosAprobados = proyectosAprobados;
        this.documentosPendientes = documentosPendientes;
    }

    public long getPropuestasPendientes() {
        return propuestasPendientes;
    }

    public void setPropuestasPendientes(long propuestasPendientes) {
        this.propuestasPendientes = propuestasPendientes;
    }

    public long getTutoriasActivas() {
        return tutoriasActivas;
    }

    public void setTutoriasActivas(long tutoriasActivas) {
        this.tutoriasActivas = tutoriasActivas;
    }

    public long getProyectosAprobados() {
        return proyectosAprobados;
    }

    public void setProyectosAprobados(long proyectosAprobados) {
        this.proyectosAprobados = proyectosAprobados;
    }

    public long getDocumentosPendientes() {
        return documentosPendientes;
    }

    public void setDocumentosPendientes(long documentosPendientes) {
        this.documentosPendientes = documentosPendientes;
    }
}
