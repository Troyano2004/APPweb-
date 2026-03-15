package com.erwin.backend.controller;

import com.erwin.backend.dtos.ConfiguracionCorreoDto;
import com.erwin.backend.service.ConfiguracionCorreoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion-correo")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class ConfiguracionCorreoController {

    private final ConfiguracionCorreoService service;

    public ConfiguracionCorreoController(ConfiguracionCorreoService service) {
        this.service = service;
    }
    @GetMapping
    public List<ConfiguracionCorreoDto> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/activa")
    public ConfiguracionCorreoDto obtener() {
        return service.obtener();
    }

    @PostMapping
    public ConfiguracionCorreoDto crear(@RequestBody ConfiguracionCorreoDto dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    public ConfiguracionCorreoDto editar(
            @PathVariable Integer id,
            @RequestBody ConfiguracionCorreoDto dto) {
        return service.editar(id, dto);
    }
    @PatchMapping("/{id}/activar")
    public ConfiguracionCorreoDto activar(@PathVariable Integer id) {
        return service.activar(id);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}