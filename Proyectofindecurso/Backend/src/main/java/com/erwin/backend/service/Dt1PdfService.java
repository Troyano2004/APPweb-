package com.erwin.backend.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class Dt1PdfService {

    public String leerHtml(String ruta) {
        try (InputStream is = new ClassPathResource(ruta).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el HTML", e);
        }
    }

    public byte[] aPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF", e);
        }
    }

    public String seguro(Object valor) {
        if (valor == null) return "";
        return valor.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}