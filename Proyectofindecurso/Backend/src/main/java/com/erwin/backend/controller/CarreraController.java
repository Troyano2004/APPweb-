package com.erwin.backend.controller;

import com.erwin.backend.dtos.CarreraDto;
import com.erwin.backend.service.CarreraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/carrera")
@CrossOrigin(origins = "http://localhost:4200")
public class CarreraController {

    private final CarreraService carreraService;

    public CarreraController(CarreraService carreraService) {
        this.carreraService = carreraService;
    }

    @GetMapping
    public ResponseEntity<List<CarreraDto>> listarTodas() {
        List<CarreraDto> carreras = carreraService.listarTodas();
        return ResponseEntity.ok(carreras);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarreraDto> obtenerPorId(@PathVariable Integer id) {
        CarreraDto carrera = carreraService.obtenerPorId(id);
        return ResponseEntity.ok(carrera);
    }

    @GetMapping("/facultad/{idFacultad}")
    public ResponseEntity<List<CarreraDto>> listarPorFacultad(@PathVariable Integer idFacultad) {
        List<CarreraDto> carreras = carreraService.listarPorFacultad(idFacultad);
        return ResponseEntity.ok(carreras);
    }

    @PostMapping
    public ResponseEntity<CarreraDto> crear(@RequestBody CarreraDto dto) {
        CarreraDto creada = carreraService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarreraDto> actualizar(
            @PathVariable Integer id,
            @RequestBody CarreraDto dto) {
        CarreraDto actualizada = carreraService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        carreraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
