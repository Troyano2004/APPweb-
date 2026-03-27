package com.erwin.backend.controller;

import com.erwin.backend.dtos.ConfiguracionCorreoDto;
import com.erwin.backend.service.ConfiguracionCorreoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/correo")
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
    public ConfiguracionCorreoDto editar(@PathVariable Integer id,
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

    // =========================================================
    // OAuth2 Outlook — genera el link y redirige al navegador
    // =========================================================
    @GetMapping("/{id}/outlook/autorizar")
    public ResponseEntity<Map<String, String>> autorizarOutlook(@PathVariable Integer id) {
        String url = service.generarUrlAutorizacion(id);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // =========================================================
    // OAuth2 Outlook — Microsoft redirige aquí con el code
    // =========================================================
    @GetMapping("/outlook/callback")
    public void outlookCallback(@RequestParam String code,
                                @RequestParam String state,
                                HttpServletResponse response) throws IOException {
        try {
            Integer configId = Integer.parseInt(state);
            service.procesarCallback(code, configId);
            response.sendRedirect("http://localhost:4200/admin/correo?outlook=autorizado");
        } catch (Exception e) {
            response.sendRedirect("http://localhost:4200/admin/correo?outlook=error");
        }
    }
}