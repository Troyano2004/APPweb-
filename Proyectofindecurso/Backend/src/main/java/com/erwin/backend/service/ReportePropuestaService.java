package com.erwin.backend.service;

import com.erwin.backend.dtos.ReportePropuestaDto;
import com.erwin.backend.entities.DictamenPropuesta;
import com.erwin.backend.entities.PropuestaTitulacion;
import com.erwin.backend.repository.DictamenPropuestaRepository;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportePropuestaService {

    private final PropuestaTitulacionRepository propuestaRepo;
    private final DictamenPropuestaRepository dictamenRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportePropuestaService(PropuestaTitulacionRepository propuestaRepo,
                                   DictamenPropuestaRepository dictamenRepo) {
        this.propuestaRepo = propuestaRepo;
        this.dictamenRepo  = dictamenRepo;
    }

    // ─── Obtener datos ──────────────────────────────────────────────────────────

    public ReportePropuestaDto.RespuestaCompleta obtenerReporte(String estado) {
        List<PropuestaTitulacion> lista = estado != null && !estado.isBlank()
                ? propuestaRepo.findByEstado(estado)
                : propuestaRepo.findAll();

        long total       = lista.size();
        long enviadas    = lista.stream().filter(p -> "ENVIADA".equals(p.getEstado())).count();
        long enRevision  = lista.stream().filter(p -> "EN_REVISION".equals(p.getEstado())).count();
        long aprobadas   = lista.stream().filter(p -> "APROBADA".equals(p.getEstado())).count();
        long rechazadas  = lista.stream().filter(p -> "RECHAZADA".equals(p.getEstado())).count();
        double pct       = total > 0 ? Math.round((aprobadas * 100.0 / total) * 10.0) / 10.0 : 0.0;

        ReportePropuestaDto.ResumenReporte resumen =
                new ReportePropuestaDto.ResumenReporte(total, enviadas, enRevision, aprobadas, rechazadas, pct);

        List<ReportePropuestaDto.ItemReporte> items = lista.stream()
                .map(this::mapearItem)
                .collect(Collectors.toList());

        return new ReportePropuestaDto.RespuestaCompleta(resumen, items);
    }

    private ReportePropuestaDto.ItemReporte mapearItem(PropuestaTitulacion p) {
        String nombreEstudiante = "";
        String cedula = "";
        if (p.getEstudiante() != null && p.getEstudiante().getUsuario() != null) {
            var u = p.getEstudiante().getUsuario();
            nombreEstudiante = (u.getNombres() != null ? u.getNombres() : "") + " "
                    + (u.getApellidos() != null ? u.getApellidos() : "");
            cedula = u.getCedula() != null ? u.getCedula() : "";
        }
        String carrera = p.getCarrera() != null ? p.getCarrera().getNombre() : "";

        // Dictamen más reciente
        List<DictamenPropuesta> dictamenes = dictamenRepo.findByPropuesta_IdPropuesta(p.getIdPropuesta());
        Optional<DictamenPropuesta> ultimoDictamen = dictamenes.stream()
                .max(Comparator.comparing(DictamenPropuesta::getFechaDictamen));

        String decisionDir = "";
        String obsDir = "";
        String nombreDir = "";
        if (ultimoDictamen.isPresent()) {
            DictamenPropuesta d = ultimoDictamen.get();
            decisionDir = d.getDecision() != null ? d.getDecision() : "";
            obsDir = d.getObservaciones() != null ? d.getObservaciones() : "";
            if (d.getDocente() != null && d.getDocente().getUsuario() != null) {
                var u = d.getDocente().getUsuario();
                nombreDir = (u.getNombres() != null ? u.getNombres() : "") + " "
                        + (u.getApellidos() != null ? u.getApellidos() : "");
            }
        }

        return new ReportePropuestaDto.ItemReporte(
                p.getIdPropuesta(),
                nombreEstudiante.trim(),
                cedula,
                carrera,
                p.getTitulo(),
                p.getTemaInvestigacion(),
                p.getEstado(),
                p.getFechaEnvio(),
                p.getFechaRevision(),
                p.getObservacionesComision(),
                decisionDir,
                obsDir,
                nombreDir.trim()
        );
    }

    // ─── Generar PDF ────────────────────────────────────────────────────────────

    public byte[] generarPdf(String estado) {
        ReportePropuestaDto.RespuestaCompleta data = obtenerReporte(estado);
        String html = construirHtml(data);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de propuestas", e);
        }
    }

    private String construirHtml(ReportePropuestaDto.RespuestaCompleta data) {
        var r = data.getResumen();
        StringBuilder filas = new StringBuilder();

        for (ReportePropuestaDto.ItemReporte item : data.getPropuestas()) {
            String estadoBadge = badgeEstado(item.getEstado());
            String decisionBadge = item.getDecisionDirector().isBlank() ? "-" : badgeDecision(item.getDecisionDirector());

            filas.append("<tr>")
                    .append("<td>").append(s(item.getEstudiante())).append("<br/><small>").append(s(item.getCedula())).append("</small></td>")
                    .append("<td>").append(s(item.getCarrera())).append("</td>")
                    .append("<td class=\"titulo-cell\">").append(s(item.getTitulo())).append("</td>")
                    .append("<td>").append(estadoBadge).append("</td>")
                    .append("<td>").append(item.getFechaEnvio() != null ? item.getFechaEnvio().format(FMT) : "-").append("</td>")
                    .append("<td>").append(item.getFechaRevision() != null ? item.getFechaRevision().format(FMT) : "-").append("</td>")
                    .append("<td>").append(decisionBadge).append("</td>")
                    .append("<td class=\"obs-cell\">").append(s(item.getObservacionesComision())).append("</td>")
                    .append("</tr>\n");
        }

        String filtroTexto = data.getPropuestas().isEmpty() ? "(sin datos)" : "";

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta charset="UTF-8"/>
              <style>
                @page { size: A4 landscape; margin: 15mm 12mm; }
                body { font-family: Arial, sans-serif; font-size: 10px; color: #111; }
                .encabezado { text-align: center; margin-bottom: 14px; border-bottom: 2px solid #0f7a3a; padding-bottom: 8px; }
                .encabezado h1 { font-size: 15px; margin: 0 0 2px; color: #0f7a3a; }
                .encabezado p  { font-size: 10px; margin: 0; color: #555; }
                .kpis { display: table; width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
                .kpi  { display: table-cell; text-align: center; border: 1px solid #ddd; padding: 8px; width: 16%%; }
                .kpi .num  { font-size: 20px; font-weight: 700; }
                .kpi .lab  { font-size: 9px; color: #666; margin-top: 2px; }
                .kpi.verde  .num { color: #0f7a3a; }
                .kpi.rojo   .num { color: #c0392b; }
                .kpi.azul   .num { color: #2563eb; }
                .kpi.naranja.num { color: #d97706; }
                table.main { width: 100%%; border-collapse: collapse; margin-top: 6px; }
                table.main th { background: #0f7a3a; color: #fff; padding: 6px 5px; font-size: 9px; text-align: left; }
                table.main td { border-bottom: 1px solid #e5e7eb; padding: 5px 5px; vertical-align: top; font-size: 9px; }
                table.main tr:nth-child(even) td { background: #f9fafb; }
                .titulo-cell { max-width: 160px; }
                .obs-cell    { max-width: 140px; font-size: 8px; color: #555; }
                .badge { display: inline-block; padding: 2px 6px; border-radius: 999px; font-size: 8px; font-weight: 700; }
                .badge-aprobada  { background: #d1fae5; color: #065f46; }
                .badge-rechazada { background: #fee2e2; color: #991b1b; }
                .badge-revision  { background: #dbeafe; color: #1e40af; }
                .badge-enviada   { background: #fef9c3; color: #92400e; }
                .badge-observada { background: #ede9fe; color: #5b21b6; }
                .footer { margin-top: 10px; font-size: 8px; color: #999; text-align: right; border-top: 1px solid #e5e7eb; padding-top: 4px; }
                small { font-size: 8px; color: #888; }
              </style>
            </head>
            <body>
              <div class="encabezado">
                <h1>Reporte General — Módulo Propuesta y Anteproyecto</h1>
                <p>Sistema de Gestión de Titulación · Generado el %s</p>
              </div>

              <div class="kpis">
                <div class="kpi"><div class="num">%d</div><div class="lab">Total propuestas</div></div>
                <div class="kpi naranja"><div class="num">%d</div><div class="lab">Enviadas</div></div>
                <div class="kpi azul"><div class="num">%d</div><div class="lab">En revisión</div></div>
                <div class="kpi verde"><div class="num">%d</div><div class="lab">Aprobadas</div></div>
                <div class="kpi rojo"><div class="num">%d</div><div class="lab">Rechazadas</div></div>
                <div class="kpi verde"><div class="num">%.1f%%</div><div class="lab">%% Aprobación</div></div>
              </div>

              <table class="main">
                <thead>
                  <tr>
                    <th>Estudiante</th>
                    <th>Carrera</th>
                    <th>Título de la propuesta</th>
                    <th>Estado</th>
                    <th>Fecha envío</th>
                    <th>Fecha revisión</th>
                    <th>Dictamen</th>
                    <th>Observaciones</th>
                  </tr>
                </thead>
                <tbody>
                  %s
                  %s
                </tbody>
              </table>

              <div class="footer">
                Reporte generado automáticamente — Total de registros: %d
              </div>
            </body>
            </html>
            """.formatted(
                java.time.LocalDate.now().format(FMT),
                r.getTotal(), r.getEnviadas(), r.getEnRevision(),
                r.getAprobadas(), r.getRechazadas(), r.getPorcentajeAprobacion(),
                filas.toString(),
                filtroTexto,
                r.getTotal()
        );
    }

    private String badgeEstado(String estado) {
        if (estado == null) return "-";
        return switch (estado) {
            case "APROBADA"   -> "<span class=\"badge badge-aprobada\">APROBADA</span>";
            case "RECHAZADA"  -> "<span class=\"badge badge-rechazada\">RECHAZADA</span>";
            case "EN_REVISION"-> "<span class=\"badge badge-revision\">EN REVISIÓN</span>";
            case "ENVIADA"    -> "<span class=\"badge badge-enviada\">ENVIADA</span>";
            default           -> "<span class=\"badge\">" + estado + "</span>";
        };
    }

    private String badgeDecision(String decision) {
        if (decision == null || decision.isBlank()) return "-";
        return switch (decision) {
            case "APROBADA"  -> "<span class=\"badge badge-aprobada\">APROBADA</span>";
            case "RECHAZADA" -> "<span class=\"badge badge-rechazada\">RECHAZADA</span>";
            case "OBSERVADA" -> "<span class=\"badge badge-observada\">OBSERVADA</span>";
            default          -> "<span class=\"badge\">" + decision + "</span>";
        };
    }

    private String s(Object v) {
        if (v == null) return "";
        return v.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}