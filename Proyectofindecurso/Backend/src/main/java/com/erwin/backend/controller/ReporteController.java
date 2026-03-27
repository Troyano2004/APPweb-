package com.erwin.backend.controller;

import com.erwin.backend.dtos.reporte.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.service.ReporteConfigService;
import com.erwin.backend.service.ReporteService;

// iText — imports explícitos (Font resuelto como com.itextpdf.text.Font)
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

// Apache POI — imports explícitos SIN Font (se usa como FQN: org.apache.poi.ss.usermodel.Font)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = {"http://localhost:4200", "http://26.102.176.187:4200", "http://26.122.106.219:4200"}, allowCredentials = "true")
public class ReporteController {

    private final ReporteService svc;
    private final ReporteConfigService configSvc;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String UNIVERSIDAD = "Universidad Tecnica Estatal de Quevedo";
    private static final String SISTEMA = "Sistema de Gestion de Titulacion - GPT";

    public ReporteController(ReporteService svc, ReporteConfigService configSvc) {
        this.svc = svc;
        this.configSvc = configSvc;
    }

    // ── PERIODOS Y ESTUDIANTES (para selects del frontend) ────────────────

    @GetMapping("/periodos")
    public List<PeriodoTitulacion> getPeriodos() { return svc.getPeriodos(); }

    @GetMapping("/estudiantes")
    public List<Estudiante> getEstudiantes() { return svc.getEstudiantes(); }

    // ── EXPEDIENTE PDF ────────────────────────────────────────────────────

    @GetMapping("/expediente/{idEstudiante}/pdf")
    public void expedientePdf(@PathVariable Integer idEstudiante,
                               HttpServletResponse response) throws Exception {
        ExpedienteEstudianteDto dto = svc.getExpediente(idEstudiante);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=Expediente_" + dto.getCedula() + ".pdf");

        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        // Fuentes
        Font fUnivNombre  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   new BaseColor(0, 100, 0));
        Font fUnivSlogan  = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC, new BaseColor(60, 60, 60));
        Font fUnivDir     = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, new BaseColor(80, 80, 80));
        Font fFechaLabel  = new Font(Font.FontFamily.HELVETICA, 7,  Font.NORMAL, new BaseColor(120, 120, 120));
        Font fFechaValor  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(40, 40, 40));
        Font fTitDoc      = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   new BaseColor(30, 30, 30));
        Font fSubNombre   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(80, 80, 80));
        Font fSeccion     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(30, 30, 30));
        Font fLabelCelda  = new Font(Font.FontFamily.HELVETICA, 8,  Font.BOLD,   new BaseColor(100, 100, 100));
        Font fValorCelda  = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, new BaseColor(30, 30, 30));
        Font fFooter      = new Font(Font.FontFamily.HELVETICA, 7,  Font.NORMAL, new BaseColor(150, 150, 150));
        Font fFirmaLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   new BaseColor(30, 30, 30));
        Font fFirmaSub    = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, new BaseColor(80, 80, 80));

        // ── ENCABEZADO ────────────────────────────────────────────────────
        PdfPTable encabezado = new PdfPTable(new float[]{65f, 35f});
        encabezado.setWidthPercentage(100);
        encabezado.setSpacingAfter(2);

        // Columna izquierda — datos universidad
        PdfPCell celdaUniv = new PdfPCell();
        celdaUniv.setBorder(Rectangle.NO_BORDER);
        celdaUniv.setPaddingBottom(6);
        Paragraph pUniv = new Paragraph();
        pUniv.add(new Chunk("Universidad Tecnica Estatal de Quevedo", fUnivNombre));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("La primera universidad agropecuaria del Ecuador", fUnivSlogan));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("Campus Central - Campus La Maria", fUnivDir));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("Av. Carlos J. Arosemena 38, Quevedo, Los Rios, Ecuador", fUnivDir));
        celdaUniv.addElement(pUniv);
        encabezado.addCell(celdaUniv);

        // Columna derecha — fecha
        PdfPCell celdaFecha = new PdfPCell();
        celdaFecha.setBorder(Rectangle.NO_BORDER);
        celdaFecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaFecha.setPaddingBottom(6);
        Paragraph pFecha = new Paragraph();
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        pFecha.add(new Chunk("FECHA DE EMISION", fFechaLabel));
        pFecha.add(Chunk.NEWLINE);
        String fechaFormateada = LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("es", "EC")));
        pFecha.add(new Chunk(fechaFormateada, fFechaValor));
        celdaFecha.addElement(pFecha);
        encabezado.addCell(celdaFecha);
        doc.add(encabezado);

        // Línea separadora verde
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingAfter(12);
        PdfPCell lineaCell = new PdfPCell(new Phrase(" "));
        lineaCell.setBorderWidthBottom(2f);
        lineaCell.setBorderColorBottom(new BaseColor(0, 120, 0));
        lineaCell.setBorderWidthTop(0); lineaCell.setBorderWidthLeft(0); lineaCell.setBorderWidthRight(0);
        lineaCell.setPaddingBottom(2);
        linea.addCell(lineaCell);
        doc.add(linea);

        // ── TÍTULO DEL DOCUMENTO ──────────────────────────────────────────
        Paragraph titDoc = new Paragraph("Expediente de Titulacion", fTitDoc);
        titDoc.setSpacingAfter(2);
        doc.add(titDoc);
        Paragraph subNombre = new Paragraph(dto.getNombres() + " " + dto.getApellidos(), fSubNombre);
        subNombre.setSpacingAfter(14);
        doc.add(subNombre);

        // ── DATOS PERSONALES ──────────────────────────────────────────────
        doc.add(seccionTitulo("Datos personales", fSeccion));
        PdfPTable tDatos = tablaDosCols();
        agregarFila(tDatos, "CEDULA",        dto.getCedula(),         fLabelCelda, fValorCelda);
        agregarFila(tDatos, "NOMBRES",       dto.getNombres() + " " + dto.getApellidos(), fLabelCelda, fValorCelda);
        agregarFila(tDatos, "CORREO",        dto.getCorreo() != null ? dto.getCorreo() : "-", fLabelCelda, fValorCelda);
        agregarFila(tDatos, "CARRERA",       dto.getCarrera() != null ? dto.getCarrera() : "-", fLabelCelda, fValorCelda);
        agregarFila(tDatos, "PROMEDIO",      dto.getPromedioRecord() != null ? dto.getPromedioRecord().toString() : "-", fLabelCelda, fValorCelda);
        agregarFila(tDatos, "DISCAPACIDAD",  Boolean.TRUE.equals(dto.getDiscapacidad()) ? "Si" : "No", fLabelCelda, fValorCelda);
        doc.add(tDatos);
        doc.add(espaciado(10));

        // ── PROYECTO ──────────────────────────────────────────────────────
        if (dto.getTituloProyecto() != null) {
            doc.add(seccionTitulo("Datos del proyecto", fSeccion));
            PdfPTable tProy = tablaDosCols();
            agregarFila(tProy, "PROYECTO",  dto.getTituloProyecto(), fLabelCelda, fValorCelda);
            agregarFila(tProy, "TIPO",      dto.getTipoTrabajo() != null ? dto.getTipoTrabajo() : "-", fLabelCelda, fValorCelda);
            agregarFila(tProy, "DIRECTOR",  dto.getDirector() != null ? dto.getDirector() : "Sin asignar", fLabelCelda, fValorCelda);
            agregarFila(tProy, "ESTADO",    dto.getEstadoProyecto() != null ? dto.getEstadoProyecto() : "-", fLabelCelda, fValorCelda);
            agregarFila(tProy, "PERIODO",   dto.getPeriodo() != null ? dto.getPeriodo() : "-", fLabelCelda, fValorCelda);
            agregarFila(tProy, "ANTIPLAGIO",dto.getPorcentajeAntiplagio() != null ? dto.getPorcentajeAntiplagio() + "%" : "Pendiente", fLabelCelda, fValorCelda);
            doc.add(tProy);
            doc.add(espaciado(10));
        }

        // ── TUTORIAS ──────────────────────────────────────────────────────
        if (dto.getTutorias() != null && !dto.getTutorias().isEmpty()) {
            doc.add(seccionTitulo("Tutorias (" + dto.getTutoriasRealizadas() + " realizadas de " + dto.getTotalTutorias() + " programadas)", fSeccion));
            PdfPTable tTut = new PdfPTable(new float[]{25f, 25f, 25f, 25f});
            tTut.setWidthPercentage(100); tTut.setSpacingAfter(10);
            for (String h : new String[]{"FECHA","MODALIDAD","ESTADO","DOCENTE"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, fLabelCelda));
                c.setBackgroundColor(new BaseColor(245, 245, 245));
                c.setBorderColor(new BaseColor(200, 200, 200));
                c.setPadding(5); tTut.addCell(c);
            }
            for (ExpedienteEstudianteDto.TutoriaReporteDto t : dto.getTutorias()) {
                agregarCelda(tTut, t.getFecha() != null ? t.getFecha().format(FMT) : "-", fValorCelda);
                agregarCelda(tTut, t.getModalidad() != null ? t.getModalidad() : "-", fValorCelda);
                agregarCelda(tTut, t.getEstado() != null ? t.getEstado() : "-", fValorCelda);
                agregarCelda(tTut, t.getDocente() != null ? t.getDocente() : "-", fValorCelda);
            }
            doc.add(tTut);
        }

        // ── TRIBUNAL ──────────────────────────────────────────────────────
        if (dto.getTribunal() != null && !dto.getTribunal().isEmpty()) {
            doc.add(seccionTitulo("Tribunal de titulacion", fSeccion));
            PdfPTable tTrib = new PdfPTable(new float[]{40f, 20f, 40f});
            tTrib.setWidthPercentage(100); tTrib.setSpacingAfter(10);
            for (String h : new String[]{"DOCENTE","CARGO","TITULO 4TO NIVEL"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, fLabelCelda));
                c.setBackgroundColor(new BaseColor(245, 245, 245));
                c.setBorderColor(new BaseColor(200, 200, 200));
                c.setPadding(5); tTrib.addCell(c);
            }
            for (ExpedienteEstudianteDto.TribunalReporteDto t : dto.getTribunal()) {
                agregarCelda(tTrib, t.getDocente() != null ? t.getDocente() : "-", fValorCelda);
                agregarCelda(tTrib, t.getCargo() != null ? t.getCargo() : "-", fValorCelda);
                agregarCelda(tTrib, t.getTitulo4toNivel() != null ? t.getTitulo4toNivel() : "-", fValorCelda);
            }
            doc.add(tTrib);
        }

        // ── SUSTENTACIONES ────────────────────────────────────────────────
        if (dto.getSustentaciones() != null && !dto.getSustentaciones().isEmpty()) {
            doc.add(seccionTitulo("Sustentaciones", fSeccion));
            PdfPTable tSus = new PdfPTable(new float[]{20f, 20f, 25f, 15f, 20f});
            tSus.setWidthPercentage(100); tSus.setSpacingAfter(20);
            for (String h : new String[]{"TIPO","FECHA","LUGAR","NOTA","OBSERVACIONES"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, fLabelCelda));
                c.setBackgroundColor(new BaseColor(245, 245, 245));
                c.setBorderColor(new BaseColor(200, 200, 200));
                c.setPadding(5); tSus.addCell(c);
            }
            for (ExpedienteEstudianteDto.SustentacionReporteDto s : dto.getSustentaciones()) {
                agregarCelda(tSus, s.getTipo() != null ? s.getTipo() : "-", fValorCelda);
                agregarCelda(tSus, s.getFecha() != null ? s.getFecha().format(FMT) : "-", fValorCelda);
                agregarCelda(tSus, s.getLugar() != null ? s.getLugar() : "-", fValorCelda);
                agregarCelda(tSus, s.getNotaFinal() != null ? s.getNotaFinal().toString() : "-", fValorCelda);
                agregarCelda(tSus, s.getObservaciones() != null ? s.getObservaciones() : "-", fValorCelda);
            }
            doc.add(tSus);
        }

        // ── FIRMA ─────────────────────────────────────────────────────────
        doc.add(espaciado(30));
        PdfPTable tFirma = new PdfPTable(1);
        tFirma.setWidthPercentage(35);
        tFirma.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell lineaFirma = new PdfPCell(new Phrase(" "));
        lineaFirma.setBorderWidthTop(1f);
        lineaFirma.setBorderWidthBottom(0); lineaFirma.setBorderWidthLeft(0); lineaFirma.setBorderWidthRight(0);
        lineaFirma.setBorderColorTop(new BaseColor(60, 60, 60));
        lineaFirma.setHorizontalAlignment(Element.ALIGN_CENTER);
        tFirma.addCell(lineaFirma);
        String firmaNombre = configSvc.get("firma_nombre");
        String firmaCargo  = configSvc.get("firma_cargo");
        PdfPCell cFirmaLabel = new PdfPCell(new Phrase(
            firmaNombre != null ? firmaNombre : "Coordinador/a de Titulacion", fFirmaLabel));
        cFirmaLabel.setBorder(Rectangle.NO_BORDER);
        cFirmaLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        tFirma.addCell(cFirmaLabel);
        PdfPCell cFirmaSub = new PdfPCell(new Phrase(
            firmaCargo != null ? firmaCargo : "Coordinacion Academica", fFirmaSub));
        cFirmaSub.setBorder(Rectangle.NO_BORDER);
        cFirmaSub.setHorizontalAlignment(Element.ALIGN_CENTER);
        tFirma.addCell(cFirmaSub);
        doc.add(tFirma);

        // ── PIE DE PÁGINA ─────────────────────────────────────────────────
        doc.add(espaciado(20));
        Paragraph footer = new Paragraph(
            "Documento generado automaticamente por el Sistema de Gestion de Titulacion.", fFooter);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
    }

    // ── EXPEDIENTE EXCEL ──────────────────────────────────────────────────

    @GetMapping("/expediente/{idEstudiante}/excel")
    public void expedienteExcel(@PathVariable Integer idEstudiante,
                                 HttpServletResponse response) throws Exception {
        ExpedienteEstudianteDto dto = svc.getExpediente(idEstudiante);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=Expediente_" + dto.getCedula() + ".xlsx");

        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Expediente");

        CellStyle styleTitle = crearEstiloTitulo(wb, new byte[]{0, 100, 0});
        CellStyle styleHeader = crearEstiloHeader(wb, new byte[]{0, 100, 0});
        CellStyle styleSub = crearEstiloSubtitulo(wb);
        CellStyle styleNormal = crearEstiloNormal(wb);

        int row = 0;

        // Encabezado universidad
        Row r0 = sheet.createRow(row++);
        Cell c0 = r0.createCell(0); c0.setCellValue(UNIVERSIDAD); c0.setCellStyle(styleTitle);
        sheet.addMergedRegion(new CellRangeAddress(row-1, row-1, 0, 5));

        Row r1 = sheet.createRow(row++);
        Cell c1 = r1.createCell(0); c1.setCellValue(SISTEMA); c1.setCellStyle(styleNormal);
        sheet.addMergedRegion(new CellRangeAddress(row-1, row-1, 0, 5));
        row++;

        // Titulo
        Row rTit = sheet.createRow(row++);
        Cell cTit = rTit.createCell(0); cTit.setCellValue("EXPEDIENTE DE ESTUDIANTE"); cTit.setCellStyle(styleTitle);
        sheet.addMergedRegion(new CellRangeAddress(row-1, row-1, 0, 5));
        row++;

        // Datos personales
        Row rSub1 = sheet.createRow(row++);
        Cell cSub1 = rSub1.createCell(0); cSub1.setCellValue("DATOS PERSONALES"); cSub1.setCellStyle(styleSub);

        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Cedula", dto.getCedula());
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Nombre", dto.getNombres() + " " + dto.getApellidos());
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Correo", dto.getCorreo());
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Carrera", dto.getCarrera());
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Promedio", dto.getPromedioRecord() != null ? dto.getPromedioRecord().toString() : "-");
        row++;

        // Proyecto
        if (dto.getTituloProyecto() != null) {
            Row rSub2 = sheet.createRow(row++);
            Cell cSub2 = rSub2.createCell(0); cSub2.setCellValue("PROYECTO DE TITULACION"); cSub2.setCellStyle(styleSub);
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Titulo", dto.getTituloProyecto());
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Tipo", dto.getTipoTrabajo());
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Estado", dto.getEstadoProyecto());
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Periodo", dto.getPeriodo());
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Director", dto.getDirector());
            agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Antiplagio",
                dto.getPorcentajeAntiplagio() != null ? dto.getPorcentajeAntiplagio() + "%" : "Pendiente");
            row++;
        }

        // Tutorias
        if (dto.getTutorias() != null && !dto.getTutorias().isEmpty()) {
            Row rSub3 = sheet.createRow(row++);
            Cell cSub3 = rSub3.createCell(0); cSub3.setCellValue("TUTORIAS"); cSub3.setCellStyle(styleSub);
            Row rHead = sheet.createRow(row++);
            for (int i = 0; i < 4; i++) {
                Cell ch = rHead.createCell(i); ch.setCellStyle(styleHeader);
            }
            rHead.getCell(0).setCellValue("Fecha");
            rHead.getCell(1).setCellValue("Modalidad");
            rHead.getCell(2).setCellValue("Estado");
            rHead.getCell(3).setCellValue("Docente");
            for (ExpedienteEstudianteDto.TutoriaReporteDto t : dto.getTutorias()) {
                Row rt = sheet.createRow(row++);
                rt.createCell(0).setCellValue(t.getFecha() != null ? t.getFecha().format(FMT) : "-");
                rt.createCell(1).setCellValue(t.getModalidad() != null ? t.getModalidad() : "-");
                rt.createCell(2).setCellValue(t.getEstado() != null ? t.getEstado() : "-");
                rt.createCell(3).setCellValue(t.getDocente() != null ? t.getDocente() : "-");
            }
            row++;
        }

        for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
        wb.write(response.getOutputStream());
        wb.close();
    }

    // ── PERIODO PDF ───────────────────────────────────────────────────────

    @GetMapping("/periodo/{idPeriodo}/pdf")
    public void periodoPdf(@PathVariable Integer idPeriodo,
                            @RequestParam(required = false) Integer idCarrera,
                            @RequestParam(required = false) String estado,
                            HttpServletResponse response) throws Exception {
        ReportePeriodoDto dto = svc.getReportePeriodo(idPeriodo, idCarrera, estado);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=Reporte_Periodo_" + idPeriodo + ".pdf");

        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        Font fSub    = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(0, 100, 0));
        Font fNormal = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL);
        Font fHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font fCell   = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL);

        generarEncabezado(doc, "Reporte por Periodo: " + dto.getPeriodo());

        doc.add(new Paragraph("Periodo: " + (dto.getFechaInicio() != null ? dto.getFechaInicio().format(FMT) : "-")
            + " al " + (dto.getFechaFin() != null ? dto.getFechaFin().format(FMT) : "-"), fNormal));
        doc.add(Chunk.NEWLINE);

        // Estadisticas
        doc.add(new Paragraph("RESUMEN ESTADISTICO", fSub));
        PdfPTable tStats = new PdfPTable(new float[]{33f, 33f, 34f});
        tStats.setWidthPercentage(100);
        String[][] stats = {
            {"Total estudiantes", String.valueOf(dto.getTotalEstudiantes()), ""},
            {"Finalizados", String.valueOf(dto.getProyectosFinalizados()), ""},
            {"En desarrollo", String.valueOf(dto.getProyectosEnProceso()), ""},
            {"Anteproyecto", String.valueOf(dto.getProyectosAnteproyecto()), ""},
            {"Predefensa", String.valueOf(dto.getProyectosPredefensa()), ""},
            {"Defensa", String.valueOf(dto.getProyectosDefensa()), ""}
        };
        for (String[] s : stats) {
            PdfPCell sc = new PdfPCell(new Phrase(s[0] + ": " + s[1], fNormal));
            sc.setPadding(5); tStats.addCell(sc);
        }
        doc.add(tStats);
        doc.add(Chunk.NEWLINE);

        // Listado de proyectos
        doc.add(new Paragraph("LISTADO DE PROYECTOS", fSub));
        PdfPTable tProy = new PdfPTable(new float[]{18f, 12f, 28f, 12f, 12f, 10f, 8f});
        tProy.setWidthPercentage(100);
        for (String h : new String[]{"Estudiante","Cedula","Titulo","Tipo","Director","Carrera","Estado"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fHeader));
            c.setBackgroundColor(new BaseColor(0, 100, 0));
            c.setPadding(4); tProy.addCell(c);
        }
        for (ReportePeriodoDto.ProyectoPeriodoDto p : dto.getProyectos()) {
            tProy.addCell(new PdfPCell(new Phrase(p.getEstudiante() != null ? p.getEstudiante() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getCedula() != null ? p.getCedula() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getTitulo() != null ? p.getTitulo() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getTipoTrabajo() != null ? p.getTipoTrabajo() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getDirector() != null ? p.getDirector() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getCarrera() != null ? p.getCarrera() : "-", fCell)));
            tProy.addCell(new PdfPCell(new Phrase(p.getEstado() != null ? p.getEstado() : "-", fCell)));
        }
        doc.add(tProy);
        doc.close();
    }

    // ── PERIODO EXCEL ─────────────────────────────────────────────────────

    @GetMapping("/periodo/{idPeriodo}/excel")
    public void periodoExcel(@PathVariable Integer idPeriodo,
                              HttpServletResponse response) throws Exception {
        ReportePeriodoDto dto = svc.getReportePeriodo(idPeriodo, null, null);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=Periodo_" + idPeriodo + ".xlsx");

        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Periodo");

        CellStyle styleTitle  = crearEstiloTitulo(wb, new byte[]{0, 100, 0});
        CellStyle styleHeader = crearEstiloHeader(wb, new byte[]{0, 100, 0});
        CellStyle styleSub    = crearEstiloSubtitulo(wb);
        CellStyle styleNormal = crearEstiloNormal(wb);

        int row = 0;

        Row r0 = sheet.createRow(row++);
        Cell c0 = r0.createCell(0); c0.setCellValue(UNIVERSIDAD); c0.setCellStyle(styleTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row r1 = sheet.createRow(row++);
        r1.createCell(0).setCellValue("REPORTE POR PERIODO: " + dto.getPeriodo());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
        row++;

        // Stats
        Row rStat = sheet.createRow(row++);
        rStat.createCell(0).setCellValue("RESUMEN"); rStat.getCell(0).setCellStyle(styleSub);
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Total estudiantes", String.valueOf(dto.getTotalEstudiantes()));
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Finalizados", String.valueOf(dto.getProyectosFinalizados()));
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "En desarrollo", String.valueOf(dto.getProyectosEnProceso()));
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Anteproyecto", String.valueOf(dto.getProyectosAnteproyecto()));
        agregarFilaExcel(sheet, row++, styleHeader, styleNormal, "Predefensa", String.valueOf(dto.getProyectosPredefensa()));
        row++;

        // Tabla proyectos
        Row rH = sheet.createRow(row++);
        String[] heads = {"Estudiante","Cedula","Titulo","Tipo","Director","Carrera","Estado"};
        for (int i = 0; i < heads.length; i++) {
            Cell ch = rH.createCell(i); ch.setCellValue(heads[i]); ch.setCellStyle(styleHeader);
        }
        for (ReportePeriodoDto.ProyectoPeriodoDto p : dto.getProyectos()) {
            Row rp = sheet.createRow(row++);
            rp.createCell(0).setCellValue(p.getEstudiante() != null ? p.getEstudiante() : "-");
            rp.createCell(1).setCellValue(p.getCedula() != null ? p.getCedula() : "-");
            rp.createCell(2).setCellValue(p.getTitulo() != null ? p.getTitulo() : "-");
            rp.createCell(3).setCellValue(p.getTipoTrabajo() != null ? p.getTipoTrabajo() : "-");
            rp.createCell(4).setCellValue(p.getDirector() != null ? p.getDirector() : "-");
            rp.createCell(5).setCellValue(p.getCarrera() != null ? p.getCarrera() : "-");
            rp.createCell(6).setCellValue(p.getEstado() != null ? p.getEstado() : "-");
        }
        for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);
        wb.write(response.getOutputStream());
        wb.close();
    }

    // ── ACTAS PDF ─────────────────────────────────────────────────────────

    @GetMapping("/actas/tutoria/{idProyecto}/pdf")
    public void actasTutoriaPdf(@PathVariable Integer idProyecto,
                                 HttpServletResponse response) throws Exception {
        List<ActaConstanciaDto> actas = svc.getActasTutorias(idProyecto);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=Actas_Tutorias_" + idProyecto + ".pdf");

        Document doc = new Document(PageSize.A4, 50, 50, 60, 40);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        Font fSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(0, 100, 0));
        Font fNormal = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL);

        generarEncabezado(doc, "Actas de Tutorias");

        for (int idx = 0; idx < actas.size(); idx++) {
            ActaConstanciaDto acta = actas.get(idx);
            if (idx > 0) doc.newPage();

            PdfPTable td = new PdfPTable(new float[]{40f, 60f}); td.setWidthPercentage(100);
            agregarFilaTabla(td, "Estudiante:", acta.getEstudiante() != null ? acta.getEstudiante() : "-", fNormal);
            agregarFilaTabla(td, "Titulo del proyecto:", acta.getTituloProyecto() != null ? acta.getTituloProyecto() : "-", fNormal);
            agregarFilaTabla(td, "Director:", acta.getDirector() != null ? acta.getDirector() : "-", fNormal);
            agregarFilaTabla(td, "Fecha:", acta.getFecha() != null ? acta.getFecha().format(FMT) : "-", fNormal);
            agregarFilaTabla(td, "Hora:", acta.getHora() != null ? acta.getHora().toString() : "-", fNormal);
            agregarFilaTabla(td, "Modalidad:", acta.getModalidad() != null ? acta.getModalidad() : "-", fNormal);
            doc.add(td); doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("DETALLE DE LA TUTORIA", fSub));
            PdfPTable td2 = new PdfPTable(new float[]{30f, 70f}); td2.setWidthPercentage(100);
            agregarFilaTabla(td2, "Objetivo:", acta.getObjetivo() != null ? acta.getObjetivo() : "-", fNormal);
            agregarFilaTabla(td2, "Detalle de revision:", acta.getDetalleRevision() != null ? acta.getDetalleRevision() : "-", fNormal);
            agregarFilaTabla(td2, "Cumplimiento:", acta.getCumplimiento() != null ? acta.getCumplimiento() : "-", fNormal);
            agregarFilaTabla(td2, "Conclusion:", acta.getConclusion() != null ? acta.getConclusion() : "-", fNormal);
            agregarFilaTabla(td2, "Observaciones:", acta.getObservaciones() != null ? acta.getObservaciones() : "-", fNormal);
            doc.add(td2);
        }

        doc.close();
    }

    @GetMapping("/actas/sustentacion/{idProyecto}/pdf")
    public void actasSustentacionPdf(@PathVariable Integer idProyecto,
                                      HttpServletResponse response) throws Exception {
        List<ActaConstanciaDto> actas = svc.getActasSustentacion(idProyecto);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=Actas_Sustentacion_" + idProyecto + ".pdf");

        Document doc = new Document(PageSize.A4, 50, 50, 60, 40);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        Font fSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(0, 100, 0));
        Font fNormal = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL);
        Font fHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font fCell   = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL);

        generarEncabezado(doc, "Actas de Sustentacion");

        for (int idx = 0; idx < actas.size(); idx++) {
            ActaConstanciaDto acta = actas.get(idx);
            if (idx > 0) doc.newPage();

            PdfPTable td = new PdfPTable(new float[]{35f, 65f}); td.setWidthPercentage(100);
            agregarFilaTabla(td, "Titulo del proyecto:", acta.getTituloProyecto() != null ? acta.getTituloProyecto() : "-", fNormal);
            agregarFilaTabla(td, "Periodo:", acta.getPeriodo() != null ? acta.getPeriodo() : "-", fNormal);
            agregarFilaTabla(td, "Fecha:", acta.getFecha() != null ? acta.getFecha().format(FMT) : "-", fNormal);
            agregarFilaTabla(td, "Hora:", acta.getHora() != null ? acta.getHora().toString() : "-", fNormal);
            agregarFilaTabla(td, "Lugar:", acta.getLugar() != null ? acta.getLugar() : "-", fNormal);
            agregarFilaTabla(td, "Nota final promedio:", acta.getNotaFinalPromedio() != null ? acta.getNotaFinalPromedio().toString() : "Pendiente", fNormal);
            doc.add(td); doc.add(Chunk.NEWLINE);

            if (acta.getEvaluaciones() != null && !acta.getEvaluaciones().isEmpty()) {
                doc.add(new Paragraph("EVALUACIONES DEL TRIBUNAL", fSub));
                PdfPTable tEval = new PdfPTable(new float[]{25f, 12f, 12f, 12f, 12f, 12f, 15f});
                tEval.setWidthPercentage(100);
                for (String h : new String[]{"Docente","Cargo","Calidad","Original","Dominio","Preguntas","Nota Final"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, fHeader));
                    c.setBackgroundColor(new BaseColor(0, 100, 0)); c.setPadding(4); tEval.addCell(c);
                }
                for (ActaConstanciaDto.EvaluacionActaDto e : acta.getEvaluaciones()) {
                    tEval.addCell(new PdfPCell(new Phrase(e.getDocente() != null ? e.getDocente() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getCargo() != null ? e.getCargo() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getCalidadTrabajo() != null ? e.getCalidadTrabajo().toString() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getOriginalidad() != null ? e.getOriginalidad().toString() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getDominioTema() != null ? e.getDominioTema().toString() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getPreguntas() != null ? e.getPreguntas().toString() : "-", fCell)));
                    tEval.addCell(new PdfPCell(new Phrase(e.getNotaFinal() != null ? e.getNotaFinal().toString() : "-", fCell)));
                }
                doc.add(tEval);
            }
        }
        doc.close();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private void generarEncabezado(Document doc, String tituloReporte) throws Exception {
        Font fUnivNombre = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   new BaseColor(0, 100, 0));
        Font fUnivSlogan = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC, new BaseColor(60, 60, 60));
        Font fUnivDir    = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, new BaseColor(80, 80, 80));
        Font fFechaLabel = new Font(Font.FontFamily.HELVETICA, 7,  Font.NORMAL, new BaseColor(120, 120, 120));
        Font fFechaValor = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(40, 40, 40));
        Font fTitDoc     = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   new BaseColor(30, 30, 30));

        PdfPTable encabezado = new PdfPTable(new float[]{65f, 35f});
        encabezado.setWidthPercentage(100);
        encabezado.setSpacingAfter(2);

        PdfPCell celdaUniv = new PdfPCell();
        celdaUniv.setBorder(Rectangle.NO_BORDER);
        celdaUniv.setPaddingBottom(6);
        Paragraph pUniv = new Paragraph();
        pUniv.add(new Chunk("Universidad Tecnica Estatal de Quevedo", fUnivNombre));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("La primera universidad agropecuaria del Ecuador", fUnivSlogan));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("Campus Central - Campus La Maria", fUnivDir));
        pUniv.add(Chunk.NEWLINE);
        pUniv.add(new Chunk("Av. Carlos J. Arosemena 38, Quevedo, Los Rios, Ecuador", fUnivDir));
        celdaUniv.addElement(pUniv);
        encabezado.addCell(celdaUniv);

        PdfPCell celdaFecha = new PdfPCell();
        celdaFecha.setBorder(Rectangle.NO_BORDER);
        celdaFecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaFecha.setPaddingBottom(6);
        Paragraph pFecha = new Paragraph();
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        pFecha.add(new Chunk("FECHA DE EMISION", fFechaLabel));
        pFecha.add(Chunk.NEWLINE);
        String fechaFormateada = LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("es", "EC")));
        pFecha.add(new Chunk(fechaFormateada, fFechaValor));
        celdaFecha.addElement(pFecha);
        encabezado.addCell(celdaFecha);
        doc.add(encabezado);

        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingAfter(12);
        PdfPCell lineaCell = new PdfPCell(new Phrase(" "));
        lineaCell.setBorderWidthBottom(2f);
        lineaCell.setBorderColorBottom(new BaseColor(0, 120, 0));
        lineaCell.setBorderWidthTop(0); lineaCell.setBorderWidthLeft(0); lineaCell.setBorderWidthRight(0);
        lineaCell.setPaddingBottom(2);
        linea.addCell(lineaCell);
        doc.add(linea);

        Paragraph titDoc = new Paragraph(tituloReporte, fTitDoc);
        titDoc.setSpacingAfter(14);
        doc.add(titDoc);
    }

    private Paragraph seccionTitulo(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        return p;
    }

    private PdfPTable tablaDosCols() {
        PdfPTable t = new PdfPTable(new float[]{35f, 65f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(0);
        return t;
    }

    private void agregarFila(PdfPTable tabla, String label, String valor,
                              Font fLabel, Font fValor) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fLabel));
        c1.setBackgroundColor(new BaseColor(248, 248, 248));
        c1.setBorderColor(new BaseColor(210, 210, 210));
        c1.setPadding(5);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "-", fValor));
        c2.setBorderColor(new BaseColor(210, 210, 210));
        c2.setPadding(5);
        tabla.addCell(c2);
    }

    private void agregarCelda(PdfPTable tabla, String valor, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(valor != null ? valor : "-", font));
        c.setBorderColor(new BaseColor(210, 210, 210));
        c.setPadding(5);
        tabla.addCell(c);
    }

    private Paragraph espaciado(float puntos) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(puntos);
        return p;
    }

    private void agregarFilaTabla(PdfPTable table, String label, String value, Font font) {
        Font bold = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell c1 = new PdfPCell(new Phrase(label, bold));
        c1.setBackgroundColor(new BaseColor(240, 255, 240));
        c1.setPadding(5); table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", font));
        c2.setPadding(5); table.addCell(c2);
    }

    private void agregarFilaExcel(Sheet sheet, int rowNum, CellStyle keyStyle, CellStyle valStyle, String key, String value) {
        Row row = sheet.createRow(rowNum);
        Cell c0 = row.createCell(0); c0.setCellValue(key); c0.setCellStyle(keyStyle);
        Cell c1 = row.createCell(1); c1.setCellValue(value != null ? value : "-"); c1.setCellStyle(valStyle);
    }

    private CellStyle crearEstiloTitulo(XSSFWorkbook wb, byte[] rgb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 13);
        f.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER); return s;
    }

    private CellStyle crearEstiloHeader(XSSFWorkbook wb, byte[] rgb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f); return s;
    }

    private CellStyle crearEstiloSubtitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        s.setFont(f); return s;
    }

    private CellStyle crearEstiloNormal(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f); return s;
    }
}
