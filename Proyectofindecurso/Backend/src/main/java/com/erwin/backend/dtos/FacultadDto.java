package com.erwin.backend.dtos;

public class FacultadDto {
    private Integer idFacultad;
    private String nombre;
    private Integer idUniversidad;
    private String nombreUniversidad;

    public FacultadDto() {}

    public FacultadDto(Integer idFacultad, String nombre, Integer idUniversidad, String nombreUniversidad) {
        this.idFacultad = idFacultad;
        this.nombre = nombre;
        this.idUniversidad = idUniversidad;
        this.nombreUniversidad = nombreUniversidad;
    }

    public Integer getIdFacultad() { return idFacultad; }
    public void setIdFacultad(Integer idFacultad) { this.idFacultad = idFacultad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdUniversidad() { return idUniversidad; }
    public void setIdUniversidad(Integer idUniversidad) { this.idUniversidad = idUniversidad; }
    public String getNombreUniversidad() { return nombreUniversidad; }
    public void setNombreUniversidad(String nombreUniversidad) { this.nombreUniversidad = nombreUniversidad; }
}
