package com.erwin.backend.dtos;

public class CarreraDto {
    private Integer idCarrera;
    private String nombre;
    private Integer idFacultad;
    private String nombreFacultad;

    public CarreraDto() {}

    public CarreraDto(Integer idCarrera, String nombre, Integer idFacultad, String nombreFacultad) {
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.idFacultad = idFacultad;
        this.nombreFacultad = nombreFacultad;
    }

    public Integer getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Integer idCarrera) { this.idCarrera = idCarrera; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdFacultad() { return idFacultad; }
    public void setIdFacultad(Integer idFacultad) { this.idFacultad = idFacultad; }
    public String getNombreFacultad() { return nombreFacultad; }
    public void setNombreFacultad(String nombreFacultad) { this.nombreFacultad = nombreFacultad; }
}
