package com.erwin.backend.controller;

import com.erwin.backend.dtos.PeriodoTitulacionDto;
import com.erwin.backend.service.PeriodoTitulacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/periodo")
@CrossOrigin(origins = "http://localhost:4200")
public class PeriodoTitulacionController {

    private final PeriodoTitulacionService periodoService;

    public PeriodoTitulacionController(PeriodoTitulacionService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public ResponseEntity<List<PeriodoTitulacionDto>> listarTodos() {
        List<PeriodoTitulacionDto> periodos = periodoService.listarTodos();
        return ResponseEntity.ok(periodos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodoTitulacionDto> obtenerPorId(@PathVariable Integer id) {
        PeriodoTitulacionDto periodo = periodoService.obtenerPorId(id);
        return ResponseEntity.ok(periodo);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<PeriodoTitulacionDto>> listarActivos() {
        List<PeriodoTitulacionDto> periodos = periodoService.listarActivos();
        return ResponseEntity.ok(periodos);
    }

    @PostMapping
    public ResponseEntity<PeriodoTitulacionDto> crear(@RequestBody PeriodoTitulacionDto dto) {
        PeriodoTitulacionDto creado = periodoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodoTitulacionDto> actualizar(
            @PathVariable Integer id,
            @RequestBody PeriodoTitulacionDto dto) {
        PeriodoTitulacionDto actualizado = periodoService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<PeriodoTitulacionDto> activar(@PathVariable Integer id) {
        PeriodoTitulacionDto activado = periodoService.cambiarEstado(id, true);
        return ResponseEntity.ok(activado);
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<PeriodoTitulacionDto> desactivar(@PathVariable Integer id) {
        PeriodoTitulacionDto desactivado = periodoService.cambiarEstado(id, false);
        return ResponseEntity.ok(desactivado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        periodoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/activo")
    public PeriodoTitulacionDto obtenerActivo() {
        return periodoService.obtenerPeriodoActivo();
    }
}
