package com.erwin.backend.dtos;

public class UniversidadDto {
    private Integer idUniversidad;
    private String nombre;
    private String mision;
    private String vision;
    private String lema;
    private String campus;
    private String direccion;
    private String contactoInfo;

    public UniversidadDto() {}

    public UniversidadDto(Integer idUniversidad, String nombre, String mision, String vision, 
                          String lema, String campus, String direccion, String contactoInfo) {
        this.idUniversidad = idUniversidad;
        this.nombre = nombre;
        this.mision = mision;
        this.vision = vision;
        this.lema = lema;
        this.campus = campus;
        this.direccion = direccion;
        this.contactoInfo = contactoInfo;
    }

    public Integer getIdUniversidad() { return idUniversidad; }
    public void setIdUniversidad(Integer idUniversidad) { this.idUniversidad = idUniversidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getMision() { return mision; }
    public void setMision(String mision) { this.mision = mision; }
    public String getVision() { return vision; }
    public void setVision(String vision) { this.vision = vision; }
    public String getLema() { return lema; }
    public void setLema(String lema) { this.lema = lema; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getContactoInfo() { return contactoInfo; }
    public void setContactoInfo(String contactoInfo) { this.contactoInfo = contactoInfo; }
}
