package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CoordinadorDtos {
    public static class SeguimientoProyectoDto {
        private Integer idProyecto;
        private Integer idEstudiante;
        private String estudiante;
        private String tituloProyecto;
        private String director;
        private String estado;
        private LocalDateTime ultimaRevision;
        private Integer avance;

        public Integer getIdProyecto() {
            return idProyecto;
        }

        public void setIdProyecto(Integer idProyecto) {
            this.idProyecto = idProyecto;
        }

        public String getEstudiante() {
            return estudiante;
        }

        public Integer getIdEstudiante() {
            return idEstudiante;
        }

        public void setIdEstudiante(Integer idEstudiante) {
            this.idEstudiante = idEstudiante;
        }

        public void setEstudiante(String estudiante) {
            this.estudiante = estudiante;
        }

        public String getTituloProyecto() {
            return tituloProyecto;
        }

        public void setTituloProyecto(String tituloProyecto) {
            this.tituloProyecto = tituloProyecto;
        }

        public String getDirector() {
            return director;
        }

        public void setDirector(String director) {
            this.director = director;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public LocalDateTime getUltimaRevision() {
            return ultimaRevision;
        }

        public void setUltimaRevision(LocalDateTime ultimaRevision) {
            this.ultimaRevision = ultimaRevision;
        }

        public Integer getAvance() {
            return avance;
        }

        public void setAvance(Integer avance) {
            this.avance = avance;
        }
    }

    public static class EstudianteSinDirectorDto {
        private Integer idDocumento;
        private String estudiante;
        private String carrera;
        private String proyecto;

        public Integer getIdDocumento() {
            return idDocumento;
        }

        public void setIdDocumento(Integer idDocumento) {
            this.idDocumento = idDocumento;
        }

        public String getEstudiante() {
            return estudiante;
        }

        public void setEstudiante(String estudiante) {
            this.estudiante = estudiante;
        }

        public String getCarrera() {
            return carrera;
        }

        public void setCarrera(String carrera) {
            this.carrera = carrera;
        }

        public String getProyecto() {
            return proyecto;
        }

        public void setProyecto(String proyecto) {
            this.proyecto = proyecto;
        }
    }

    public static class DirectorCargaDto {
        private Integer idDocente;
        private String director;
        private long proyectosAsignados;

        public Integer getIdDocente() {
            return idDocente;
        }

        public void setIdDocente(Integer idDocente) {
            this.idDocente = idDocente;
        }

        public String getDirector() {
            return director;
        }

        public void setDirector(String director) {
            this.director = director;
        }

        public long getProyectosAsignados() {
            return proyectosAsignados;
        }

        public void setProyectosAsignados(long proyectosAsignados) {
            this.proyectosAsignados = proyectosAsignados;
        }
    }

    public static class AsignarDirectorRequest {
        private Integer idDocumento;
        private Integer idDocente;
        private String motivo;

        public Integer getIdDocumento() {
            return idDocumento;
        }

        public void setIdDocumento(Integer idDocumento) {
            this.idDocumento = idDocumento;
        }

        public Integer getIdDocente() {
            return idDocente;
        }

        public void setIdDocente(Integer idDocente) {
            this.idDocente = idDocente;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }

    public static class ObservacionAdministrativaDto {
        private Integer id;
        private Integer idProyecto;
        private String proyecto;
        private String tipo;
        private String detalle;
        private String creadoPor;
        private LocalDateTime creadoEn;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getIdProyecto() {
            return idProyecto;
        }

        public void setIdProyecto(Integer idProyecto) {
            this.idProyecto = idProyecto;
        }

        public String getProyecto() {
            return proyecto;
        }

        public void setProyecto(String proyecto) {
            this.proyecto = proyecto;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getDetalle() {
            return detalle;
        }

        public void setDetalle(String detalle) {
            this.detalle = detalle;
        }

        public String getCreadoPor() {
            return creadoPor;
        }

        public void setCreadoPor(String creadoPor) {
            this.creadoPor = creadoPor;
        }

        public LocalDateTime getCreadoEn() {
            return creadoEn;
        }

        public void setCreadoEn(LocalDateTime creadoEn) {
            this.creadoEn = creadoEn;
        }
    }

    public static class CrearObservacionAdministrativaRequest {
        private Integer idProyecto;
        private String tipo;
        private String detalle;
        private String creadoPor;

        public Integer getIdProyecto() {
            return idProyecto;
        }

        public void setIdProyecto(Integer idProyecto) {
            this.idProyecto = idProyecto;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getDetalle() {
            return detalle;
        }

        public void setDetalle(String detalle) {
            this.detalle = detalle;
        }

        public String getCreadoPor() {
            return creadoPor;
        }

        public void setCreadoPor(String creadoPor) {
            this.creadoPor = creadoPor;
        }
    }

    public static class ComisionFormativaDto {
        private Integer idComision;
        private Integer idCarrera;
        private String carrera;
        private String periodoAcademico;
        private String estado;
        private List<ComisionMiembroDto> miembros = new ArrayList<>();

        public Integer getIdComision() {
            return idComision;
        }

        public void setIdComision(Integer idComision) {
            this.idComision = idComision;
        }

        public Integer getIdCarrera() {
            return idCarrera;
        }

        public void setIdCarrera(Integer idCarrera) {
            this.idCarrera = idCarrera;
        }

        public String getCarrera() {
            return carrera;
        }

        public void setCarrera(String carrera) {
            this.carrera = carrera;
        }

        public String getPeriodoAcademico() {
            return periodoAcademico;
        }

        public void setPeriodoAcademico(String periodoAcademico) {
            this.periodoAcademico = periodoAcademico;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public List<ComisionMiembroDto> getMiembros() {
            return miembros;
        }

        public void setMiembros(List<ComisionMiembroDto> miembros) {
            this.miembros = miembros;
        }
    }

    public static class ComisionMiembroDto {
        private Integer idDocente;
        private String docente;
        private String cargo;

        public Integer getIdDocente() {
            return idDocente;
        }

        public void setIdDocente(Integer idDocente) {
            this.idDocente = idDocente;
        }

        public String getDocente() {
            return docente;
        }

        public void setDocente(String docente) {
            this.docente = docente;
        }

        public String getCargo() {
            return cargo;
        }

        public void setCargo(String cargo) {
            this.cargo = cargo;
        }
    }

    public static class CrearComisionRequest {
        private Integer idCarrera;
        private String periodoAcademico;
        private String estado;

        public Integer getIdCarrera() {
            return idCarrera;
        }

        public void setIdCarrera(Integer idCarrera) {
            this.idCarrera = idCarrera;
        }

        public String getPeriodoAcademico() {
            return periodoAcademico;
        }

        public void setPeriodoAcademico(String periodoAcademico) {
            this.periodoAcademico = periodoAcademico;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    public static class AsignarMiembrosRequest {
        private List<ComisionMiembroDto> miembros = new ArrayList<>();

        public List<ComisionMiembroDto> getMiembros() {
            return miembros;
        }

        public void setMiembros(List<ComisionMiembroDto> miembros) {
            this.miembros = miembros;
        }
    }

    public static class CatalogoCarreraDto {
        private Integer idCarrera;
        private String nombre;

        public Integer getIdCarrera() {
            return idCarrera;
        }

        public void setIdCarrera(Integer idCarrera) {
            this.idCarrera = idCarrera;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }

    public static class AsignarComisionProyectoRequest {
        private Integer idComision;
        private Integer idProyecto;
        private String resolucionActa;
        private String observacion;
        private String estado;
        private java.time.LocalDate fechaConformacion;

        public Integer getIdComision() {
            return idComision;
        }

        public void setIdComision(Integer idComision) {
            this.idComision = idComision;
        }

        public Integer getIdProyecto() {
            return idProyecto;
        }

        public void setIdProyecto(Integer idProyecto) {
            this.idProyecto = idProyecto;
        }

        public String getResolucionActa() {
            return resolucionActa;
        }

        public void setResolucionActa(String resolucionActa) {
            this.resolucionActa = resolucionActa;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public java.time.LocalDate getFechaConformacion() {
            return fechaConformacion;
        }

        public void setFechaConformacion(java.time.LocalDate fechaConformacion) {
            this.fechaConformacion = fechaConformacion;
        }
    }
}
