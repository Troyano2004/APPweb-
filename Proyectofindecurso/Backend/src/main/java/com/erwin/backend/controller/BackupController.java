package com.erwin.backend.controller;

import com.erwin.backend.dtos.BackupDto;
import com.erwin.backend.service.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Controller del módulo de respaldos.
 * Todos los endpoints son accesibles solo para ADMIN
 * (la verificación de rol se hace en el frontend y aquí se asume que
 * el JWT ya está validado por DbSessionFilter).
 *
 * Rutas:
 *  GET    /admin/backup/configuracion          → obtener config
 *  PUT    /admin/backup/configuracion          → guardar config
 *  GET    /admin/backup/historial              → listar historial
 *  POST   /admin/backup/ejecutar               → backup manual ahora
 *  GET    /admin/backup/descargar/{id}         → descargar archivo
 *  DELETE /admin/backup/historial/{id}         → eliminar registro
 */
@RestController
@RequestMapping("/admin/backup")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    // ── Configuración ─────────────────────────────────────────────────────────

    @GetMapping("/configuracion")
    public ResponseEntity<BackupDto.ConfiguracionDto> obtenerConfiguracion() {
        return ResponseEntity.ok(backupService.obtenerConfiguracion());
    }

    @PutMapping("/configuracion")
    public ResponseEntity<?> guardarConfiguracion(@RequestBody BackupDto.ConfiguracionDto dto) {
        try {
            return ResponseEntity.ok(backupService.guardarConfiguracion(dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Historial ─────────────────────────────────────────────────────────────

    @GetMapping("/historial")
    public ResponseEntity<List<BackupDto.HistorialDto>> listarHistorial() {
        return ResponseEntity.ok(backupService.listarHistorial());
    }

    @DeleteMapping("/historial/{id}")
    public ResponseEntity<?> eliminarHistorial(@PathVariable Integer id) {
        try {
            backupService.eliminarHistorial(id);
            return ResponseEntity.ok(Map.of("mensaje", "Registro eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Ejecución manual ──────────────────────────────────────────────────────

    @PostMapping("/ejecutar")
    public ResponseEntity<BackupDto.EjecucionResultDto> ejecutarBackup() {
        BackupDto.EjecucionResultDto resultado = backupService.ejecutarRespaldo();
        HttpStatus status = resultado.isExitoso() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(resultado);
    }

    // ── Descarga de archivo ───────────────────────────────────────────────────

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        try {
            Path ruta     = backupService.obtenerRutaParaDescarga(id);
            Resource res  = new FileSystemResource(ruta);
            String nombre = ruta.getFileName().toString();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(nombre.endsWith(".zip")
                            ? MediaType.parseMediaType("application/zip")
                            : MediaType.TEXT_PLAIN)
                    .body(res);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}