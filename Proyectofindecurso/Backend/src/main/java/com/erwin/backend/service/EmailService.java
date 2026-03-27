package com.erwin.backend.service;

import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private final ConfiguracionCorreoRepository configRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Map<String, String> HOSTS = Map.of(
            "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp-mail.outlook.com"
    );

    public EmailService(ConfiguracionCorreoRepository configRepo) {
        this.configRepo = configRepo;
    }

    // =========================================================
    // Obtiene un access token fresco usando el refresh token
    // =========================================================
    private String obtenerAccessToken(ConfiguracionCorreo config) {
        String clientId     = config.getClientId();
        String clientSecret = CryptoUtil.decrypt(config.getClientSecret());
        String refreshToken = config.getRefreshToken();

        if (clientId == null || clientSecret == null || refreshToken == null) {
            throw new RuntimeException("OAUTH2_NO_CONFIGURADO: faltan client_id, client_secret o refresh_token");
        }

        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type",    "refresh_token");
        body.add("scope",         "https://outlook.office.com/SMTP.Send offline_access");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<?, ?> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("No se obtuvo access_token de Microsoft");
            }

            // Si Microsoft devuelve un nuevo refresh_token, lo actualizamos en BD
            if (responseBody.containsKey("refresh_token")) {
                config.setRefreshToken((String) responseBody.get("refresh_token"));
                configRepo.save(config);
            }

            return (String) responseBody.get("access_token");

        } catch (Exception e) {
            throw new RuntimeException("ERROR_OBTENIENDO_TOKEN_OUTLOOK: " + e.getMessage());
        }
    }

    // =========================================================
    // Construye JavaMailSender dinámicamente desde BD
    // =========================================================
    private JavaMailSender buildMailSender() {
        ConfiguracionCorreo config = configRepo.findFirstByActivoTrue()
                .orElseThrow(() -> new RuntimeException("NO_HAY_CONFIGURACION_CORREO_ACTIVA"));

        String proveedor = config.getProveedor().toUpperCase();
        String host = HOSTS.get(proveedor);

        System.out.println("=== CONFIG CORREO ACTIVA ===");
        System.out.println("Usuario: " + config.getUsuario());
        System.out.println("Proveedor: " + proveedor);
        System.out.println("Host: " + host);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(587);
        sender.setUsername(config.getUsuario());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        if (proveedor.equals("OUTLOOK")) {
            String accessToken = obtenerAccessToken(config);
            sender.setPassword(accessToken);
            props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            props.put("mail.smtp.sasl.enable", "true");
            props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
            props.put("mail.smtp.auth.login.disable", "true");
            props.put("mail.smtp.auth.plain.disable", "true");
        } else {
            String password = CryptoUtil.decrypt(config.getPassword());
            sender.setPassword(password);
        }

        return sender;
    }

    // =========================================================
    // Enviar código de verificación
    // =========================================================
    public void enviarCodigo(String correo, String codigo) {
        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setFrom(sender.getUsername());
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
    // Enviar notificación de tutoría
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

    // =========================================================
    // Enviar notificación asignación DT1
    // =========================================================
    public void notificarAsignacionDt1(String correo, String nombreDocente, String carrera, String periodo) {
        try {
            JavaMailSender sender = buildMailSender();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(correo);
            h.setSubject("Asignación como Director de Titulación I - UTEQ");
            h.setText(
                    "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto'>" +
                            "<h2 style='color:#27ae60'>Asignación como Director de Titulación I</h2>" +
                            "<p>Estimado/a <strong>" + nombreDocente + "</strong>,</p>" +
                            "<p>Le informamos que ha sido asignado/a como <strong>Director de Titulación I (DT1)</strong> " +
                            "en el Sistema de Titulación UTEQ.</p>" +
                            "<table style='width:100%;border-collapse:collapse;background:#f4f6f9;border-radius:8px'>" +
                            "<tr><td style='padding:10px;font-weight:bold'>Carrera:</td>" +
                            "<td style='padding:10px'>" + carrera + "</td></tr>" +
                            "<tr><td style='padding:10px;font-weight:bold'>Período académico:</td>" +
                            "<td style='padding:10px'>" + periodo + "</td></tr>" +
                            "</table>" +
                            "<p style='margin-top:1rem'>Como DT1, tendrá a su cargo la revisión y aprobación de los " +
                            "anteproyectos de titulación de los estudiantes asignados a su carrera.</p>" +
                            "<p>Ingrese al sistema para gestionar sus funciones:</p>" +
                            "<div style='text-align:center;margin:1.5rem 0'>" +
                            "<a href='http://localhost:4200' style='background:#27ae60;color:#fff;padding:12px 28px;" +
                            "border-radius:8px;text-decoration:none;font-weight:bold'>Ingresar al Sistema</a>" +
                            "</div>" +
                            "<p style='color:#7f8c8d;font-size:0.85rem'>Si tiene alguna consulta, contacte a la coordinación de su carrera.</p>" +
                            "</div>", true
            );
            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ENVIANDO_NOTIFICACION_DT1: " + e.getMessage());
        }
    }
}