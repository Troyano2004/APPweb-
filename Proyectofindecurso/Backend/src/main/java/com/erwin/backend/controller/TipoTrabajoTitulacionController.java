package com.erwin.backend.controller;

import com.erwin.backend.dtos.TipoTrabajoTitulacionDto;
import com.erwin.backend.service.TipoTrabajoTitulacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/tipo-trabajo")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class TipoTrabajoTitulacionController {

    private final TipoTrabajoTitulacionService tipoTrabajoService;

    public TipoTrabajoTitulacionController(TipoTrabajoTitulacionService tipoTrabajoService) {
        this.tipoTrabajoService = tipoTrabajoService;
    }

    @GetMapping
    public ResponseEntity<List<TipoTrabajoTitulacionDto>> listarTodos() {
        List<TipoTrabajoTitulacionDto> tipos = tipoTrabajoService.listarTodos();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoTrabajoTitulacionDto> obtenerPorId(@PathVariable Integer id) {
        TipoTrabajoTitulacionDto tipo = tipoTrabajoService.obtenerPorId(id);
        return ResponseEntity.ok(tipo);
    }

    @GetMapping("/modalidad/{idModalidad}")
    public ResponseEntity<List<TipoTrabajoTitulacionDto>> listarPorModalidad(@PathVariable Integer idModalidad) {
        List<TipoTrabajoTitulacionDto> tipos = tipoTrabajoService.listarPorModalidad(idModalidad);
        return ResponseEntity.ok(tipos);
    }

    @PostMapping
    public ResponseEntity<TipoTrabajoTitulacionDto> crear(@RequestBody TipoTrabajoTitulacionDto dto) {
        TipoTrabajoTitulacionDto creado = tipoTrabajoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoTrabajoTitulacionDto> actualizar(
            @PathVariable Integer id,
            @RequestBody TipoTrabajoTitulacionDto dto) {
        TipoTrabajoTitulacionDto actualizado = tipoTrabajoService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        tipoTrabajoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
