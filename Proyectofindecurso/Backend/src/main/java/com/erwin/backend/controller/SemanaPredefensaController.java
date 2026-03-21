package com.erwin.backend.controller;

import com.erwin.backend.dtos.SemanaPredefensaDtos;
import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.dtos.ExtenderSemanaRequest;
import com.erwin.backend.service.SemanaPredefensaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dt2/semana-predefensa")
@RequiredArgsConstructor
public class SemanaPredefensaController {

    private final SemanaPredefensaService service;

    // ── Obtener semana activa ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<SemanaPredefensaDtos.SemanaPredefensaDto> obtenerSemana() {
        SemanaPredefensaDtos.SemanaPredefensaDto dto = service.obtenerSemanaActiva();
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    // ── Guardar / actualizar semana ────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public ResponseEntity<SemanaPredefensaDtos.SemanaPredefensaDto> guardarSemana(
            @RequestBody SemanaPredefensaDtos.GuardarSemanaRequest req) {
        return ResponseEntity.ok(service.guardarSemana(req));
    }

    // ── Extender / modificar semana sin borrar slots ───────────────────────────

    @PatchMapping("/extender")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public ResponseEntity<SemanaPredefensaDtos.SemanaPredefensaDto> extenderSemana(
            @RequestBody ExtenderSemanaRequest req) {
        return ResponseEntity.ok(service.extenderSemana(req));
    }

    // ── Obtener calendario completo con slots ──────────────────────────────────

    @GetMapping("/calendario")
    public ResponseEntity<SemanaPredefensaDtos.CalendarioSemanaDto> obtenerCalendario() {
        return ResponseEntity.ok(service.obtenerCalendario());
    }

    // ── Asignar un slot a un proyecto ─────────────────────────────────────────

    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public ResponseEntity<Dt2Dtos.MensajeDto> asignarSlot(
            @RequestBody SemanaPredefensaDtos.AsignarSlotRequest req) {
        return ResponseEntity.ok(service.asignarSlot(req));
    }
}