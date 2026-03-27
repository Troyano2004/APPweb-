package com.erwin.backend.controller;

import com.erwin.backend.config.DataSourceConfig;
import com.erwin.backend.service.RecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
@Slf4j
public class RecoveryController {

    private final RecoveryService recoveryService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    // ── Status — usado por el frontend para detectar si la BD está caída ─────
    // NO requiere autenticación, se llama cada 15s desde Angular

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean bdDisponible = DataSourceConfig.baseDeDatosExiste(
                datasourceUrl, datasourceUsername, datasourcePassword);
        return ResponseEntity.ok(Map.of(
                "bdDisponible", bdDisponible,
                "mensaje", bdDisponible
                        ? "Base de datos disponible"
                        : "Base de datos no disponible — Recovery Mode activo"
        ));
    }

    // ── Autenticación por contraseña maestra ──────────────────────────────────

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> autenticar(@RequestBody Map<String, String> body) {
        String password = body.getOrDefault("password", "");
        boolean ok = recoveryService.validarPassword(password);
        if (ok) {
            log.warn("Recovery: acceso autenticado correctamente");
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Acceso autorizado"));
        }
        log.warn("Recovery: intento de acceso con contraseña incorrecta");
        return ResponseEntity.status(401)
                .body(Map.of("ok", false, "mensaje", "Contraseña incorrecta"));
    }

    // ── Listar jobs ───────────────────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<List<Map<String, Object>>> listarJobs() {
        return ResponseEntity.ok(recoveryService.listarJobs());
    }

    // ── Listar backups en Drive ───────────────────────────────────────────────

    @GetMapping("/backups/{idJob}")
    public ResponseEntity<?> listarBackups(@PathVariable Long idJob) {
        try {
            return ResponseEntity.ok(recoveryService.listarBackupsEnDrive(idJob));
        } catch (Exception e) {
            log.error("Recovery listarBackups: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // ── Listar backups en carpeta local ───────────────────────────────────────

    @GetMapping("/backups-local/{idJob}")
    public ResponseEntity<?> listarBackupsLocal(@PathVariable Long idJob) {
        try {
            return ResponseEntity.ok(recoveryService.listarBackupsLocales(idJob));
        } catch (Exception e) {
            log.error("Recovery listarBackupsLocal: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Ejecutar restauración ─────────────────────────────────────────────────

    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restore(@RequestBody Map<String, Object> body) {
        try {
            Long   idJob         = Long.valueOf(body.get("idJob").toString());
            String fileId        = body.get("fileId").toString();
            String nombreArchivo = body.get("nombreArchivo").toString();
            String modo          = body.getOrDefault("modo", "REEMPLAZAR").toString();
            String nombreBdNueva = body.getOrDefault("nombreBdNueva", "").toString();

            log.warn("Recovery: iniciando restauración job={} archivo={} modo={}",
                    idJob, nombreArchivo, modo);

            Map<String, Object> resultado = recoveryService.ejecutarRestore(
                    idJob, fileId, nombreArchivo, modo, nombreBdNueva);
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Recovery restore error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "exitoso", false,
                            "mensaje", e.getMessage(),
                            "log",     ""
                    ));
        }
    }

    // ── Verificar conexión PG ─────────────────────────────────────────────────

    @GetMapping("/ping/{idJob}")
    public ResponseEntity<Map<String, Object>> ping(@PathVariable Long idJob) {
        boolean ok = recoveryService.verificarConexionPg(idJob);
        return ResponseEntity.ok(Map.of(
                "ok",      ok,
                "mensaje", ok ? "PostgreSQL conectado" : "No se puede conectar a PostgreSQL"
        ));
    }
}