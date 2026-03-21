package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class SemanaPredefensaDtos {

    // ── Request para guardar/actualizar la semana ──────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class GuardarSemanaRequest {
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Integer   duracionMinutos;
        private String    lugarDefecto;
        private String    observaciones;
        private Integer   idPeriodo;
    }

    // ── Response de la semana configurada ─────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SemanaPredefensaDto {
        private Integer   idSemana;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Integer   duracionMinutos;
        private String    lugarDefecto;
        private String    observaciones;
        private String    periodoCodigo;
        private Boolean   activo;
        private int       totalSlots;
        private int       slotsOcupados;
        private int       slotsLibres;
    }

    // ── Un slot de tiempo individual ───────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SlotDto {
        private LocalDate fechaSlot;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private boolean   ocupado;

        // Si está ocupado, datos del proyecto asignado
        private Integer idSustentacion;
        private Integer idProyecto;
        private String  tituloProyecto;
        private String  estudiante;
        private String  lugar;
    }

    // ── Un día con sus slots ───────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class DiaCalendarioDto {
        private LocalDate   fecha;
        private String      diaSemana;   // "Lunes", "Martes", etc.
        private List<SlotDto> slots;
        private int         totalSlots;
        private int         ocupados;
        private int         libres;
    }

    // ── Calendario completo de la semana ──────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CalendarioSemanaDto {
        private SemanaPredefensaDto semana;
        private List<DiaCalendarioDto> dias;
    }

    // ── Request para asignar un slot a un proyecto ─────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AsignarSlotRequest {
        private Integer   idProyecto;
        private LocalDate fecha;
        private LocalTime hora;
        private String    lugar;
        private String    observaciones;
        private Integer   idRealizadoPor;
    }
}