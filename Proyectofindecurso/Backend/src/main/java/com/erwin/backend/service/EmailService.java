package com.erwin.backend.service;

import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private final ConfiguracionCorreoRepository configRepo;

    private static final Map<String, String> HOSTS = Map.of(
            "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp.office365.com"
    );

    public EmailService(ConfiguracionCorreoRepository configRepo) {
        this.configRepo = configRepo;
    }

    // =========================================================
    // Construye JavaMailSender dinámicamente desde BD
    // =========================================================
    private JavaMailSender buildMailSender() {
        ConfiguracionCorreo config = configRepo.findFirstByActivoTrue()
                .orElseThrow(() -> new RuntimeException("NO_HAY_CONFIGURACION_CORREO_ACTIVA"));

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(HOSTS.getOrDefault(config.getProveedor().toUpperCase(), "smtp.gmail.com"));
        sender.setPort(587);
        sender.setUsername(config.getUsuario());
        sender.setPassword(CryptoUtil.decrypt(config.getPassword())); // desencriptar al usar

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return sender;
    }

    // =========================================================
    // Enviar código de verificación
    // =========================================================
    public void enviarCodigo(String correo, String codigo) {
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setSubject("Código de verificación - Sistema de Titulación UTEQ");
            h.setText(
                    "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto'>" +
                            "<h2 style='color:#27ae60'>Verificación de correo</h2>" +
                            "<p>Tu código de verificación es:</p>" +
                            "<div style='font-size:2rem;font-weight:bold;color:#2c3e50;letter-spacing:8px;" +
                            "padding:1rem;background:#f4f6f9;border-radius:8px;text-align:center'>" +
                            codigo + "</div>" +
                            "<p style='color:#7f8c8d;font-size:0.85rem'>Este código expira en 10 minutos.</p>" +
                            "</div>", true
            );
            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ENVIANDO_CODIGO: " + e.getMessage());
        }
    }

    // =========================================================
    // Enviar credenciales al estudiante aprobado
    // =========================================================
    public void enviarCredenciales(String correo, String username, String password) {
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setSubject("Credenciales de ingreso al sistema - UTEQ");
            h.setText(
                    "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto'>" +
                            "<h2 style='color:#27ae60'>Bienvenido al Sistema de Titulación UTEQ</h2>" +
                            "<p>Tus credenciales de acceso:</p>" +
                            "<table style='width:100%;border-collapse:collapse'>" +
                            "<tr><td style='padding:8px;font-weight:bold'>Usuario:</td>" +
                            "<td style='padding:8px'>" + username + "</td></tr>" +
                            "<tr><td style='padding:8px;font-weight:bold'>Contraseña:</td>" +
                            "<td style='padding:8px'>" + password + "</td></tr>" +
                            "</table>" +
                            "<p style='color:#e74c3c;font-weight:bold'>Por seguridad, cambia la contraseña al ingresar.</p>" +
                            "</div>", true
            );
            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ENVIANDO_CREDENCIALES: " + e.getMessage());
        }
    }

    // =========================================================
    // Enviar rechazo
    // =========================================================
    public void enviarRechazo(String correo, String motivo) {
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setSubject("Solicitud de registro rechazada - UTEQ");
            h.setText(
                    "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto'>" +
                            "<h2 style='color:#e74c3c'>Solicitud rechazada</h2>" +
                            "<p>Tu solicitud de registro fue rechazada por el siguiente motivo:</p>" +
                            "<div style='padding:1rem;background:#fdf3f2;border-left:4px solid #e74c3c;border-radius:4px'>" +
                            motivo + "</div>" +
                            "<p>Si tienes dudas, contacta a la coordinación.</p>" +
                            "</div>", true
            );
            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ENVIANDO_RECHAZO: " + e.getMessage());
        }
    }

    // =========================================================
    // Enviar Email de reunion
    // =========================================================
    public void noticarReunion(String correo, String motivo, String linkReunion, String idreunion, String fecha, String hora) {
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setSubject("Tutoría programada - Sistema de Titulación UTEQ");

            String linkHtml = (linkReunion != null && !linkReunion.isBlank())
                    ? "<tr><td style='padding:8px;font-weight:bold'>Link reunión:</td>" +
                    "<td style='padding:8px'><a href='" + linkReunion + "' style='color:#2980b9'>" + linkReunion + "</a></td></tr>"
                    : "";

            h.setText(
                    "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto'>" +
                            "<h2 style='color:#2980b9'>Tutoría Programada</h2>" +
                            "<p>" + (motivo != null ? motivo : "Se ha programado una tutoría para tu anteproyecto.") + "</p>" +
                            "<table style='width:100%;border-collapse:collapse;background:#f4f6f9;border-radius:8px'>" +
                            "<tr><td style='padding:8px;font-weight:bold'>N° Tutoría:</td>" +
                            "<td style='padding:8px'>#" + idreunion + "</td></tr>" +
                            "<tr><td style='padding:8px;font-weight:bold'>Fecha:</td>" +
                            "<td style='padding:8px'>" + fecha + "</td></tr>" +
                            "<tr><td style='padding:8px;font-weight:bold'>Hora:</td>" +
                            "<td style='padding:8px'>" + (hora != null ? hora : "Por confirmar") + "</td></tr>" +
                            linkHtml +
                            "</table>" +
                            "<p style='color:#7f8c8d;font-size:0.85rem'>Por favor confirma tu asistencia con tu director.</p>" +
                            "</div>", true
            );

            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ENVIANDO_NOTIFICACION_TUTORIA: " + e.getMessage());
        }
    }

}
