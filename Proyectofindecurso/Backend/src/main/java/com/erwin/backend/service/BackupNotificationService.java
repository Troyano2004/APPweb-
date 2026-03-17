package com.erwin.backend.service;

import com.erwin.backend.entities.BackupExecution;
import com.erwin.backend.entities.BackupExecution.EstadoEjecucion;
import com.erwin.backend.entities.BackupJob;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupNotificationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JavaMailSender mailSender;

    public void notificar(BackupJob job, BackupExecution exec) {
        if (exec == null) return;
        boolean exitoso = exec.getEstado() == EstadoEjecucion.EXITOSO;
        String  emails  = exitoso ? job.getEmailExito() : job.getEmailFallo();
        if (emails == null || emails.isBlank()) return;

        String asunto = exitoso
                ? "[BACKUP OK] " + job.getNombre() + " - " + exec.getDatabaseNombre()
                : "[BACKUP FALLO] " + job.getNombre() + " - " + exec.getDatabaseNombre();

        for (String email : emails.split(",")) {
            try {
                enviar(email.trim(), asunto, construirHtml(job, exec, exitoso));
            } catch (Exception e) {
                log.error("Error enviando notificación a {}", email, e);
            }
        }
    }

    public void enviarPrueba(String email) {
        try {
            enviar(email,
                    "[BACKUP TEST] Configuración de correo correcta",
                    "<h3 style='color:#28a745'>La configuración de correo para notificaciones de backup es correcta.</h3>");
        } catch (Exception e) {
            throw new RuntimeException("Error enviando email de prueba: " + e.getMessage(), e);
        }
    }

    private void enviar(String para, String asunto, String htmlBody) throws Exception {
        MimeMessage       msg    = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
    }

    private String construirHtml(BackupJob job, BackupExecution exec, boolean exitoso) {
        String color  = exitoso ? "#28a745" : "#dc3545";
        String estado = exitoso ? "EXITOSO" : "FALLIDO";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px'>")
                .append("<h2 style='color:").append(color).append("'>Backup ").append(estado).append("</h2>")
                .append("<table style='border-collapse:collapse;width:100%'>")
                .append(fila("Job",           job.getNombre()))
                .append(fila("Base de datos", exec.getDatabaseNombre()))
                .append(fila("Tipo",          exec.getTipoBackup().name()))
                .append(fila("Inicio",        exec.getIniciadoEn()   != null ? exec.getIniciadoEn().format(FMT)   : "-"))
                .append(fila("Fin",           exec.getFinalizadoEn() != null ? exec.getFinalizadoEn().format(FMT) : "-"))
                .append(fila("Duración",      exec.getDuracionSegundos() != null ? exec.getDuracionSegundos() + " seg" : "-"))
                .append(fila("Tamaño",        exec.getTamanoBytes()  != null ? formatearTamano(exec.getTamanoBytes()) : "-"))
                .append(fila("Destino",       exec.getDestinoTipo()  != null ? exec.getDestinoTipo() : "-"))
                .append(fila("Archivo",       exec.getArchivoNombre() != null ? exec.getArchivoNombre() : "-"));

        if (!exitoso && exec.getErrorMensaje() != null) {
            sb.append(fila("Error", "<span style='color:#dc3545'>" + exec.getErrorMensaje() + "</span>"));
        }
        sb.append("</table></div>");
        return sb.toString();
    }

    private String fila(String label, String valor) {
        return "<tr>" +
                "<td style='padding:6px 12px;border:1px solid #dee2e6;background:#f8f9fa;font-weight:bold'>" + label + "</td>" +
                "<td style='padding:6px 12px;border:1px solid #dee2e6'>" + (valor != null ? valor : "-") + "</td>" +
                "</tr>";
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024)                return bytes + " B";
        if (bytes < 1024 * 1024)         return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}