// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/dtos/ProyectoTitulacionCreateRequest.java
package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProyectoTitulacionCreateRequest {
    private Integer idPropuesta;
    private Integer idPeriodo;
    private Integer idDirector;
    private Integer idTipoTrabajo;
    private Integer idEleccion;
    private String titulo;
    private String estado;
    private BigDecimal porcentajeAntiplagio;
    private LocalDate fechaVerificacionAntiplagio;
    private String urlInformeAntiplagio;

    public Integer getIdPropuesta() { return idPropuesta; }
    public void setIdPropuesta(Integer idPropuesta) { this.idPropuesta = idPropuesta; }

    public Integer getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Integer idPeriodo) { this.idPeriodo = idPeriodo; }

    public Integer getIdDirector() { return idDirector; }
    public void setIdDirector(Integer idDirector) { this.idDirector = idDirector; }

    public Integer getIdTipoTrabajo() { return idTipoTrabajo; }
    public void setIdTipoTrabajo(Integer idTipoTrabajo) { this.idTipoTrabajo = idTipoTrabajo; }

    public Integer getIdEleccion() { return idEleccion; }
    public void setIdEleccion(Integer idEleccion) { this.idEleccion = idEleccion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getPorcentajeAntiplagio() { return porcentajeAntiplagio; }
    public void setPorcentajeAntiplagio(BigDecimal porcentajeAntiplagio) { this.porcentajeAntiplagio = porcentajeAntiplagio; }

    public LocalDate getFechaVerificacionAntiplagio() { return fechaVerificacionAntiplagio; }
    public void setFechaVerificacionAntiplagio(LocalDate fechaVerificacionAntiplagio) { this.fechaVerificacionAntiplagio = fechaVerificacionAntiplagio; }

    public String getUrlInformeAntiplagio() { return urlInformeAntiplagio; }
    public void setUrlInformeAntiplagio(String urlInformeAntiplagio) { this.urlInformeAntiplagio = urlInformeAntiplagio; }
}
