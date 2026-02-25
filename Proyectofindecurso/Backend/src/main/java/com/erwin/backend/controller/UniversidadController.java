package com.erwin.backend.controller;

import com.erwin.backend.dtos.UniversidadDto;
import com.erwin.backend.service.UniversidadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/universidad")
@CrossOrigin(origins = "http://localhost:4200")
public class UniversidadController {

    private final UniversidadService universidadService;

    public UniversidadController(UniversidadService universidadService) {
        this.universidadService = universidadService;
    }

    @GetMapping
    public ResponseEntity<List<UniversidadDto>> listarTodas() {
        List<UniversidadDto> universidades = universidadService.listarTodas();
        return ResponseEntity.ok(universidades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversidadDto> obtenerPorId(@PathVariable Integer id) {
        UniversidadDto universidad = universidadService.obtenerPorId(id);
        return ResponseEntity.ok(universidad);
    }

    @PostMapping
    public ResponseEntity<UniversidadDto> crear(@RequestBody UniversidadDto dto) {
        UniversidadDto creada = universidadService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UniversidadDto> actualizar(
            @PathVariable Integer id,
            @RequestBody UniversidadDto dto) {
        UniversidadDto actualizada = universidadService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        universidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
