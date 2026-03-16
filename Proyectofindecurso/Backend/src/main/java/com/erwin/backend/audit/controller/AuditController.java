package com.erwin.backend.audit.controller;
import com.erwin.backend.audit.dto.AuditStatsDto;
import com.erwin.backend.audit.entity.*;
import com.erwin.backend.audit.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuditController {
    private final AuditLogRepository    logRepo;
    private final AuditConfigRepository configRepo;

    public AuditController(AuditLogRepository logRepo, AuditConfigRepository configRepo) {
        this.logRepo = logRepo; this.configRepo = configRepo;
    }

    private Specification<AuditLog> buildSpec(String entidad, String accion, Integer idUsuario,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entidad   != null) predicates.add(cb.equal(root.get("entidad"),   entidad));
            if (accion    != null) predicates.add(cb.equal(root.get("accion"),    accion));
            if (idUsuario != null) predicates.add(cb.equal(root.get("idUsuario"), idUsuario));
            if (desde     != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestampEvento"), desde));
            if (hasta     != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestampEvento"), hasta));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @GetMapping("/logs")
    public Page<AuditLog> getLogs(
            @RequestParam(required=false) String entidad,
            @RequestParam(required=false) String accion,
            @RequestParam(required=false) Integer idUsuario,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23, 59, 59) : null;
        return logRepo.findAll(buildSpec(entidad, accion, idUsuario, d, h),
                PageRequest.of(page, size, Sort.by("timestampEvento").descending()));
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLog> getLog(@PathVariable Long id) {
        return logRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs/export/csv")
    public void exportCsv(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response) throws Exception {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
            "attachment; filename=Auditoria_" + LocalDate.now() + ".csv");

        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23, 59, 59) : null;

        List<AuditLog> logs = logRepo.findAll(buildSpec(entidad, accion, null, d, h),
                Sort.by("timestampEvento").descending());

        java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
            response.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8);

        writer.write('\uFEFF');
        writer.write("sep=;\n");
        writer.write("REPORTE DE AUDITORIA - Sistema de Titulacion UTEQ\n");
        writer.write("Fecha de generacion:;" + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
        writer.write("Entidad filtrada:;" + (entidad != null ? entidad : "Todas") + "\n");
        writer.write("Accion filtrada:;" + (accion != null ? accion : "Todas") + "\n");
        writer.write("Desde:;" + (desde != null ? desde.format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sin filtro") + "\n");
        writer.write("Hasta:;" + (hasta != null ? hasta.format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sin filtro") + "\n");
        writer.write("Total de registros:;" + logs.size() + "\n\n");

        writer.write("No.;Fecha y Hora;Entidad;ID Registro;Accion;Usuario;Correo;Direccion IP;Severidad;Origen;Estado Anterior;Estado Nuevo\n");

        int numero = 1;
        for (AuditLog l : logs) {
            String severidad = l.getConfig() != null ? l.getConfig().getSeveridad() : "N/A";
            String origen = l.getIpAddress() != null && l.getIpAddress().contains("acceso-directo")
                ? "Base de Datos" : "Aplicacion Web";
            String usuario = l.getUsername() != null
                ? l.getUsername().replace("DB:", "") : "—";

            writer.write(
                numero++ + ";" +
                escapeCsv(l.getTimestampEvento() != null
                    ? l.getTimestampEvento().format(java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm:ss")) : "—") + ";" +
                escapeCsv(l.getEntidad()) + ";" +
                escapeCsv(l.getEntidadId()) + ";" +
                escapeCsv(l.getAccion()) + ";" +
                escapeCsv(usuario) + ";" +
                escapeCsv(l.getCorreoUsuario()) + ";" +
                escapeCsv(l.getIpAddress()) + ";" +
                escapeCsv(severidad) + ";" +
                escapeCsv(origen) + ";" +
                escapeCsv(resumirJson(l.getEstadoAnterior())) + ";" +
                escapeCsv(resumirJson(l.getEstadoNuevo())) + "\n"
            );
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    private String resumirJson(String json) {
        if (json == null || json.isBlank()) return "-";
        String limpio = json.replaceAll("[\"{}]", "")
                            .replaceAll(",", " | ")
                            .replaceAll("\\s+", " ").trim();
        return limpio.length() > 150 ? limpio.substring(0, 150) + "..." : limpio;
    }

    @GetMapping("/config")
    public List<AuditConfig> getConfigs() { return configRepo.findAll(); }

    @PostMapping("/config")
    public AuditConfig createConfig(@RequestBody AuditConfig config) {
        config.setId(null);
        return configRepo.save(config);
    }

    @PutMapping("/config/{id}")
    public ResponseEntity<AuditConfig> updateConfig(@PathVariable Integer id, @RequestBody AuditConfig body) {
        return configRepo.findById(id).map(c -> {
            c.setEntidad(body.getEntidad()); c.setAccion(body.getAccion()); c.setActivo(body.getActivo());
            c.setNotificarEmail(body.getNotificarEmail()); c.setDestinatarios(body.getDestinatarios());
            c.setSeveridad(body.getSeveridad()); c.setDescripcion(body.getDescripcion());
            return ResponseEntity.ok(configRepo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/config/{id}/toggle")
    public ResponseEntity<AuditConfig> toggleConfig(@PathVariable Integer id) {
        return configRepo.findById(id).map(c -> {
            c.setActivo(!Boolean.TRUE.equals(c.getActivo()));
            return ResponseEntity.ok(configRepo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Integer id) {
        if (!configRepo.existsById(id)) return ResponseEntity.notFound().build();
        configRepo.deleteById(id); return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public AuditStatsDto getStats() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        LocalDateTime sem = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime h24 = LocalDateTime.now().minusHours(24);
        return AuditStatsDto.builder()
            .totalHoy(logRepo.countDesde(hoy))
            .totalSemana(logRepo.countDesde(sem))
            .eventosCriticos24h(logRepo.count(buildSpec(null, null, null, h24, null)))
            .topEntidades(logRepo.topEntidades(sem).stream().limit(5).map(r -> new AuditStatsDto.EntidadCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .topAcciones(logRepo.topAcciones(sem).stream().limit(5).map(r -> new AuditStatsDto.AccionCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .topUsuarios(logRepo.topUsuarios(sem).stream().limit(5).map(r -> new AuditStatsDto.UsuarioCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .build();
    }
}
