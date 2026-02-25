package com.erwin.backend.dtos;

public class TipoTrabajoTitulacionDto {
    private Integer idTipoTrabajo;
    private String nombre;
    private Integer idModalidad;
    private String nombreModalidad;

    public TipoTrabajoTitulacionDto() {}

    public TipoTrabajoTitulacionDto(Integer idTipoTrabajo, String nombre, Integer idModalidad, String nombreModalidad) {
        this.idTipoTrabajo = idTipoTrabajo;
        this.nombre = nombre;
        this.idModalidad = idModalidad;
        this.nombreModalidad = nombreModalidad;
    }

    public Integer getIdTipoTrabajo() { return idTipoTrabajo; }
    public void setIdTipoTrabajo(Integer idTipoTrabajo) { this.idTipoTrabajo = idTipoTrabajo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdModalidad() { return idModalidad; }
    public void setIdModalidad(Integer idModalidad) { this.idModalidad = idModalidad; }
    public String getNombreModalidad() { return nombreModalidad; }
    public void setNombreModalidad(String nombreModalidad) { this.nombreModalidad = nombreModalidad; }
}
