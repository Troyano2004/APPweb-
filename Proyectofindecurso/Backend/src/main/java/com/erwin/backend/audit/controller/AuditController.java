package com.erwin.backend.audit.controller;
import com.erwin.backend.audit.dto.AuditStatsDto;
import com.erwin.backend.audit.entity.*;
import com.erwin.backend.audit.repository.*;
import com.erwin.backend.audit.service.AuditSseService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class AuditController {
    private final AuditLogRepository    logRepo;
    private final AuditConfigRepository configRepo;
    private final AuditSseService       sseService;

    public AuditController(AuditLogRepository logRepo, AuditConfigRepository configRepo,
                           AuditSseService sseService) {
        this.logRepo = logRepo; this.configRepo = configRepo;
        this.sseService = sseService;
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

    // ── Catálogos de filtros ─────────────────────────────────────────────────

    @GetMapping("/entidades")
    public List<String> getEntidades() {
        return configRepo.findAll().stream()
                .map(AuditConfig::getEntidad)
                .distinct().sorted().toList();
    }

    @GetMapping("/acciones")
    public List<String> getAcciones() {
        return logRepo.findAll().stream()
                .map(AuditLog::getAccion)
                .filter(a -> a != null && !a.isBlank())
                .distinct().sorted().toList();
    }

    @GetMapping("/acciones-config")
    public List<String> getAccionesConfig() {
        return configRepo.findAll().stream()
                .map(AuditConfig::getAccion)
                .distinct().sorted().toList();
    }

    // ── SSE: stream en vivo ──────────────────────────────────────────────────

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.registrar();
    }

    @GetMapping("/stream/clientes")
    public ResponseEntity<Map<String, Integer>> clientesConectados() {
        return ResponseEntity.ok(Map.of("conectados", sseService.clientesConectados()));
    }

    // ── Logs paginados ───────────────────────────────────────────────────────

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

    @GetMapping("/logs/export/pdf")
    public void exportPdf(
            @RequestParam(required=false) String entidad,
            @RequestParam(required=false) String accion,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=Auditoria_" + LocalDate.now() + ".pdf");

        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23, 59, 59) : null;
        List<AuditLog> logs = logRepo.findAll(
                buildSpec(entidad, accion, null, d, h),
                Sort.by("timestampEvento").descending());

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        com.itextpdf.text.Font cellFont   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);

        // ── Encabezado con fondo verde ──────────────────────────────
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 3f});
        header.setSpacingAfter(15);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(new BaseColor(27, 94, 32));
        logoCell.setPadding(12);
        logoCell.setBorder(0);
        com.itextpdf.text.Font logoFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 22, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        Paragraph logoText = new Paragraph("GPT", logoFont);
        logoText.setAlignment(Element.ALIGN_CENTER);
        logoCell.addElement(logoText);
        com.itextpdf.text.Font subLogoFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 7, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);
        Paragraph subLogo = new Paragraph("Gestion del Proceso\nde Titulacion", subLogoFont);
        subLogo.setAlignment(Element.ALIGN_CENTER);
        logoCell.addElement(subLogo);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(new BaseColor(27, 94, 32));
        titleCell.setPadding(12);
        titleCell.setBorder(0);
        com.itextpdf.text.Font mainTitleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        titleCell.addElement(new Paragraph("REPORTE DE AUDITORIA", mainTitleFont));
        com.itextpdf.text.Font subTitleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);
        titleCell.addElement(new Paragraph("Sistema de Titulacion UTEQ", subTitleFont));
        com.itextpdf.text.Font infoFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.NORMAL, new BaseColor(200, 230, 201));
        Paragraph info = new Paragraph("Fecha de generacion: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                "   |   Total de registros: " + logs.size(), infoFont);
        info.setSpacingBefore(6);
        titleCell.addElement(info);

        header.addCell(logoCell);
        header.addCell(titleCell);
        document.add(header);

        // ── Línea separadora ────────────────────────────────────────
        com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
        line.setLineColor(new BaseColor(27, 94, 32));
        line.setLineWidth(2f);
        document.add(new Chunk(line));
        document.add(Chunk.NEWLINE);

        // ── Filtros aplicados (si hay) ───────────────────────────────
        if (entidad != null || accion != null || desde != null || hasta != null) {
            PdfPTable filtrosTable = new PdfPTable(1);
            filtrosTable.setWidthPercentage(100);
            filtrosTable.setSpacingAfter(8);
            com.itextpdf.text.Font filtroFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, new BaseColor(80, 80, 80));
            StringBuilder filtrosTxt = new StringBuilder("Filtros aplicados: ");
            if (entidad != null) filtrosTxt.append("Entidad=").append(entidad).append("  ");
            if (accion  != null) filtrosTxt.append("Accion=").append(accion).append("  ");
            if (desde   != null) filtrosTxt.append("Desde=").append(desde).append("  ");
            if (hasta   != null) filtrosTxt.append("Hasta=").append(hasta);
            PdfPCell filtroCell = new PdfPCell(new Phrase(filtrosTxt.toString(), filtroFont));
            filtroCell.setBorder(0);
            filtroCell.setBackgroundColor(new BaseColor(245, 245, 245));
            filtroCell.setPadding(5);
            filtrosTable.addCell(filtroCell);
            document.add(filtrosTable);
        }

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2.5f, 1f, 2f, 2f, 2f, 1.5f});

        BaseColor headerColor = new BaseColor(27, 94, 32);
        String[] headers = {"Fecha/Hora","Entidad","ID","Accion","Usuario","IP","Severidad"};
        for (String hdr : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(hdr, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (AuditLog log : logs) {
            String sev = log.getConfig() != null ? log.getConfig().getSeveridad() : "-";
            BaseColor rowColor = switch (sev) {
                case "CRITICAL" -> new BaseColor(255, 235, 235);
                case "HIGH"     -> new BaseColor(255, 248, 235);
                case "MEDIUM"   -> new BaseColor(235, 245, 255);
                default         -> BaseColor.WHITE;
            };
            String[] vals = {
                log.getTimestampEvento() != null ? log.getTimestampEvento().format(fmt) : "-",
                log.getEntidad()   != null ? log.getEntidad()   : "-",
                log.getEntidadId() != null ? log.getEntidadId() : "-",
                log.getAccion()    != null ? log.getAccion()    : "-",
                log.getUsername()  != null ? log.getUsername()  : "-",
                log.getIpAddress() != null ? log.getIpAddress() : "-",
                sev
            };
            for (String val : vals) {
                PdfPCell cell = new PdfPCell(new Phrase(val, cellFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(4);
                table.addCell(cell);
            }
        }
        document.add(table);
        document.close();
    }

    @GetMapping("/logs/export/excel")
    public void exportExcel(
            @RequestParam(required=false) String entidad,
            @RequestParam(required=false) String accion,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=Auditoria_" + LocalDate.now() + ".xlsx");

        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23, 59, 59) : null;
        List<AuditLog> logs = logRepo.findAll(
                buildSpec(entidad, accion, null, d, h),
                Sort.by("timestampEvento").descending());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Auditoria");

            // ── Estilo título principal ──────────────────────────────
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font titleFont2 = workbook.createFont();
            titleFont2.setBold(true);
            titleFont2.setColor(IndexedColors.WHITE.getIndex());
            titleFont2.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont2);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(35);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE AUDITORIA - Sistema de Titulacion UTEQ");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // ── Estilo subtítulo ─────────────────────────────────────
            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            subStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subStyle.setAlignment(HorizontalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 10);
            subStyle.setFont(subFont);

            Row infoRow = sheet.createRow(1);
            infoRow.setHeightInPoints(20);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(
                    "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                    "   |   Total registros: " + logs.size());
            infoCell.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            sheet.createRow(2); // fila separadora vacía

            // ── Estilo encabezados de columna ────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font hFont = workbook.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] cols = {"Fecha/Hora","Entidad","ID Registro","Accion","Usuario","IP","Severidad"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle criticalStyle = workbook.createCellStyle();
            criticalStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            criticalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle highStyle = workbook.createCellStyle();
            highStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            highStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle mediumStyle = workbook.createCellStyle();
            mediumStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            mediumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            int rowNum = 4;
            for (AuditLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                String sev = log.getConfig() != null ? log.getConfig().getSeveridad() : "-";
                CellStyle rowStyle = switch (sev) {
                    case "CRITICAL" -> criticalStyle;
                    case "HIGH"     -> highStyle;
                    case "MEDIUM"   -> mediumStyle;
                    default         -> null;
                };
                String[] vals = {
                    log.getTimestampEvento() != null ? log.getTimestampEvento().format(fmt) : "-",
                    log.getEntidad()   != null ? log.getEntidad()   : "-",
                    log.getEntidadId() != null ? log.getEntidadId() : "-",
                    log.getAccion()    != null ? log.getAccion()    : "-",
                    log.getUsername()  != null ? log.getUsername()  : "-",
                    log.getIpAddress() != null ? log.getIpAddress() : "-",
                    sev
                };
                for (int i = 0; i < vals.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(vals[i]);
                    if (rowStyle != null) cell.setCellStyle(rowStyle);
                }
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
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

    // ── Control de Cambios ───────────────────────────────────────────────────

    @GetMapping("/cambios")
    public Page<AuditLog> getCambios(
            @RequestParam(required=false) String entidad,
            @RequestParam(required=false) String accion,
            @RequestParam(required=false) String username,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="15") int size) {

        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23, 59, 59) : null;

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("accion").in(
                "CREATE", "UPDATE", "DELETE", "APROBAR", "RECHAZAR",
                "DECISION", "VALIDAR", "APROBAR_DIRECTOR", "DEVOLVER"
            ));
            if (entidad  != null && !entidad.isBlank())
                predicates.add(cb.equal(root.get("entidad"), entidad));
            if (accion   != null && !accion.isBlank())
                predicates.add(cb.equal(root.get("accion"), accion));
            if (username != null && !username.isBlank())
                predicates.add(cb.equal(root.get("username"), username));
            if (d != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestampEvento"), d));
            if (h != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("timestampEvento"), h));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logRepo.findAll(spec,
                PageRequest.of(page, size, Sort.by("timestampEvento").descending()));
    }

    @GetMapping("/usuarios-activos")
    public List<String> getUsuariosActivos() {
        return logRepo.findAll().stream()
                .map(AuditLog::getUsername)
                .filter(u -> u != null && !u.isBlank() && !u.startsWith("DB:"))
                .distinct().sorted().toList();
    }

    @GetMapping("/stats")
    public AuditStatsDto getStats() {
        LocalDateTime hoy = LocalDate.now(ZoneId.of("America/Guayaquil")).atStartOfDay();
        LocalDateTime sem = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime h24 = LocalDateTime.now().minusHours(24);
        LocalDateTime ultimo = logRepo.findUltimoTimestamp();
        String ultimoStr = ultimo != null
            ? ultimo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            : null;
        return AuditStatsDto.builder()
            .totalHoy(logRepo.countDesde(hoy))
            .totalSemana(logRepo.countDesde(sem))
            .eventosCriticos24h(logRepo.countCriticosDesde(h24))
            .totalCriticos(logRepo.countBySeveridad("CRITICAL"))
            .ultimoEvento(ultimoStr)
            .topEntidades(logRepo.topEntidades(sem).stream().limit(5).map(r -> new AuditStatsDto.EntidadCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .topAcciones(logRepo.topAcciones(sem).stream().limit(5).map(r -> new AuditStatsDto.AccionCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .topUsuarios(logRepo.topUsuarios(sem).stream().limit(5).map(r -> new AuditStatsDto.UsuarioCount((String) r[0], (Long) r[1])).collect(Collectors.toList()))
            .build();
    }
}
