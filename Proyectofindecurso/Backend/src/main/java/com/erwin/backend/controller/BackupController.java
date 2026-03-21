package com.erwin.backend.controller;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.BackupJobRequest;
import com.erwin.backend.dtos.BackupTestConexionRequest;
import com.erwin.backend.dtos.BackupTestDestinoRequest;
import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupJob;
import com.erwin.backend.service.BackupNotificationService;
import com.erwin.backend.service.BackupSchedulerService;
import com.erwin.backend.service.BackupService;
import com.erwin.backend.service.BackupStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService             backupService;
    private final BackupSchedulerService    schedulerService;
    private final BackupStorageService      storageService;
    private final BackupNotificationService notificationService;

    // ── Jobs ───────────────────────────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<List<BackupJob>> listarJobs() {
        return ResponseEntity.ok(backupService.listarJobs());
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<BackupJob> obtenerJob(@PathVariable Long id) {
        return ResponseEntity.ok(backupService.obtenerJob(id));
    }

    @Auditable(entidad = "Backup", accion = "CREATE", capturarArgs = false)
    @PostMapping("/jobs")
    public ResponseEntity<BackupJob> crearJob(@RequestBody BackupJobRequest req) {
        BackupJob job = backupService.crearJob(req);
        schedulerService.programarJob(job);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<BackupJob> actualizarJob(@PathVariable Long id,
                                                   @RequestBody BackupJobRequest req) {
        BackupJob job = backupService.actualizarJob(id, req);
        schedulerService.programarJob(job);
        return ResponseEntity.ok(job);
    }

    @Auditable(entidad = "Backup", accion = "DELETE", capturarArgs = false)
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> eliminarJob(@PathVariable Long id) {
        schedulerService.cancelarJob(id);
        backupService.eliminarJob(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/jobs/{id}/estado")
    public ResponseEntity<BackupJob> toggleEstado(@PathVariable Long id,
                                                  @RequestBody Map<String, Boolean> body) {
        BackupJobRequest req = new BackupJobRequest();
        req.setActivo(body.get("activo"));
        BackupJob job = backupService.actualizarJob(id, req);
        if (Boolean.TRUE.equals(body.get("activo"))) {
            schedulerService.programarJob(job);
        } else {
            schedulerService.cancelarJob(id);
        }
        return ResponseEntity.ok(job);
    }

    // ── Ejecutar ahora ─────────────────────────────────────────────────────────

    @Auditable(entidad = "Backup", accion = "CREATE", capturarArgs = false)
    @PostMapping("/jobs/{id}/run")
    public ResponseEntity<BackupExecution> ejecutarAhora(@PathVariable Long id) {
        return ResponseEntity.ok(backupService.ejecutarJob(id, true));
    }

    // ── Historial ──────────────────────────────────────────────────────────────

    @GetMapping("/jobs/{id}/historial")
    public ResponseEntity<List<BackupExecution>> historialJob(@PathVariable Long id) {
        return ResponseEntity.ok(backupService.historialJob(id));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<BackupExecution>> historialGeneral() {
        return ResponseEntity.ok(
                backupService.listarJobs().stream()
                        .flatMap(j -> backupService.historialJob(j.getIdJob()).stream())
                        .sorted((a, b) -> {
                            if (a.getIniciadoEn() == null) return 1;
                            if (b.getIniciadoEn() == null) return -1;
                            return b.getIniciadoEn().compareTo(a.getIniciadoEn());
                        })
                        .limit(200)
                        .toList());
    }

    // ── Pruebas ────────────────────────────────────────────────────────────────

    @PostMapping("/test/postgres")
    public ResponseEntity<Map<String, Object>> probarPostgres(@RequestBody BackupTestConexionRequest req) {
        boolean ok = backupService.probarConexionPg(req.getHost(), req.getPort(), req.getUsuario(), req.getPassword());
        return ResponseEntity.ok(Map.of(
                "exitoso", ok,
                "mensaje", ok ? "Conexión exitosa" : "No se pudo conectar al servidor PostgreSQL"));
    }

    // ── Listar bases de datos del servidor PG ─────────────────────────────────

    @PostMapping("/test/databases")
    public ResponseEntity<Map<String, Object>> listarDatabases(@RequestBody BackupTestConexionRequest req) {
        try {
            List<String> dbs = backupService.listarDatabases(
                    req.getHost(), req.getPort(), req.getUsuario(), req.getPassword());
            return ResponseEntity.ok(Map.of("exitoso", true, "databases", dbs));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exitoso", false, "mensaje", e.getMessage(), "databases", List.of()));
        }
    }

    @PostMapping("/test/destino")
    public ResponseEntity<Map<String, Object>> probarDestino(@RequestBody BackupTestDestinoRequest req) {
        boolean ok;
        String  mensaje;
        try {
            ok = switch (req.getTipo()) {
                case LOCAL -> storageService.probarLocal(req.getRutaLocal());
                case AZURE -> storageService.probarAzure(req.getAzureAccount(), req.getAzureKey(), req.getAzureContainer());
                case S3    -> storageService.probarS3(req.getS3Bucket(), req.getS3Region(), req.getS3AccessKey(), req.getS3SecretKey());
                case GOOGLE_DRIVE -> throw new UnsupportedOperationException("Google Drive requiere flujo OAuth2");
            };
            mensaje = ok ? "Conexión al destino exitosa" : "No se pudo conectar al destino";
        } catch (Exception e) {
            ok      = false;
            mensaje = e.getMessage();
        }
        return ResponseEntity.ok(Map.of("exitoso", ok, "mensaje", mensaje));
    }

    @PostMapping("/test/email")
    public ResponseEntity<Map<String, Object>> probarEmail(@RequestBody Map<String, String> body) {
        try {
            notificationService.enviarPrueba(body.get("email"));
            return ResponseEntity.ok(Map.of("exitoso", true, "mensaje", "Email de prueba enviado"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exitoso", false, "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/zonas-horarias")
    public ResponseEntity<List<String>> zonasHorarias() {
        return ResponseEntity.ok(List.of(
                "America/Guayaquil", "America/Bogota", "America/Lima",
                "America/New_York", "America/Chicago", "America/Los_Angeles",
                "America/Sao_Paulo", "Europe/Madrid", "Europe/London", "UTC"));
    }
}