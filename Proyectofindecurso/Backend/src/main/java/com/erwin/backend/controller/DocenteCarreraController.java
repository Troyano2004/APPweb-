package com.erwin.backend.controller;

import com.erwin.backend.dtos.AsignarCarreraDocenteRequest;
import com.erwin.backend.dtos.DocenteCarreraResponse;
import com.erwin.backend.service.DocenteCarreraService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/docentes")
@CrossOrigin(origins = "http://localhost:4200")
public class DocenteCarreraController {

    private final DocenteCarreraService service;

    public DocenteCarreraController(DocenteCarreraService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocenteCarreraResponse> listar() {
        return service.listarDocentes();
    }

    @PostMapping("/asignar-carrera")
    public DocenteCarreraResponse asignar(@RequestBody AsignarCarreraDocenteRequest req) {
        return service.asignarCarrera(req);
    }

    @PatchMapping("/{id}/estado")
    public DocenteCarreraResponse cambiarEstado(
            @PathVariable Integer id,
            @RequestParam boolean activo) {
        return service.cambiarEstado(id, activo);
    }
    @GetMapping("/filtrar")
    public List<DocenteCarreraResponse> filtrarPorCarrera(@RequestParam Integer idCarrera) {
        return service.filtrarPorCarrera(idCarrera);
    }
}