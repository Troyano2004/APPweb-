package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EstadoEstudianteDto {

    private String nombreCompleto;
    private String carrera;
    private String modalidad;
    private String etapaActual;
    private int    porcentajeAvance;
    private List<EtapaDto> etapas;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EtapaDto {
        private String    clave;
        private String    titulo;
        private String    descripcion;
        private String    estado;   // COMPLETADO | EN_CURSO | PENDIENTE | RECHAZADO
        private LocalDate fecha;
    }
}