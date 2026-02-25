package com.erwin.backend.controller;

import com.erwin.backend.dtos.FacultadDto;
import com.erwin.backend.service.FacultadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/facultad")
@CrossOrigin(origins = "http://localhost:4200")
public class FacultadController {

    private final FacultadService facultadService;

    public FacultadController(FacultadService facultadService) {
        this.facultadService = facultadService;
    }

    @GetMapping
    public ResponseEntity<List<FacultadDto>> listarTodas() {
        List<FacultadDto> facultades = facultadService.listarTodas();
        return ResponseEntity.ok(facultades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultadDto> obtenerPorId(@PathVariable Integer id) {
        FacultadDto facultad = facultadService.obtenerPorId(id);
        return ResponseEntity.ok(facultad);
    }

    @GetMapping("/universidad/{idUniversidad}")
    public ResponseEntity<List<FacultadDto>> listarPorUniversidad(@PathVariable Integer idUniversidad) {
        List<FacultadDto> facultades = facultadService.listarPorUniversidad(idUniversidad);
        return ResponseEntity.ok(facultades);
    }

    @PostMapping
    public ResponseEntity<FacultadDto> crear(@RequestBody FacultadDto dto) {
        FacultadDto creada = facultadService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacultadDto> actualizar(
            @PathVariable Integer id,
            @RequestBody FacultadDto dto) {
        FacultadDto actualizada = facultadService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        facultadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
