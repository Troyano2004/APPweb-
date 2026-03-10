package com.erwin.backend.service;

import jakarta.mail.MessagingException;
import com.erwin.backend.entities.ConfiguracionCorreo;
import com.erwin.backend.repository.ConfiguracionCorreoRepository;
import com.erwin.backend.security.CryptoUtil;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ConfiguracionCorreoRepository configRepo;

    private static final Map<String, String> HOSTS = Map.of(
            "GMAIL",   "smtp.gmail.com",
            "YAHOO",   "smtp.mail.yahoo.com",
            "OUTLOOK", "smtp.office365.com"
    );

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailService(JavaMailSender mailSender, ConfiguracionCorreoRepository configRepo) {
        this.mailSender = mailSender;
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
        sender.setPassword(CryptoUtil.decrypt(config.getPassword()));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return sender;
    }

    // =========================================================
    // NOTIFICACIONES A DOCENTES
    // =========================================================
    @Async
    public void notificarAsignacionDocenteDt2(String toEmail,
                                              String nombreDocente,
                                              String tituloProyecto,
                                              String nombreEstudiante,
                                              String periodo) {
        String asunto = "Asignacion como Docente DT2 - " + abreviar(tituloProyecto, 60);
        String html   = buildEmailDocenteDt2(nombreDocente, tituloProyecto, nombreEstudiante, periodo);
        enviar(toEmail, asunto, html);
    }

    @Async
    public void notificarAsignacionDirector(String toEmail,
                                            String nombreDocente,
                                            String tituloProyecto,
                                            String nombreEstudiante,
                                            String periodo) {
        String asunto = "Asignacion como Director TIC - " + abreviar(tituloProyecto, 60);
        String html   = buildEmailDirector(nombreDocente, tituloProyecto, nombreEstudiante, periodo);
        enviar(toEmail, asunto, html);
    }

    @Async
    public void notificarAsignacionTribunal(String toEmail,
                                            String nombreDocente,
                                            String cargo,
                                            String tituloProyecto,
                                            String nombreEstudiante,
                                            String periodo) {
        String asunto = "Asignacion como " + cargo + " del Tribunal - " + abreviar(tituloProyecto, 50);
        String html   = buildEmailTribunal(nombreDocente, cargo, tituloProyecto, nombreEstudiante, periodo);
        enviar(toEmail, asunto, html);
    }

    // =========================================================
    // NOTIFICACIONES AL ESTUDIANTE
    // =========================================================
    @Async
    public void notificarPropuestaAprobada(String toEmail,
                                           String nombreEstudiante,
                                           String tituloProyecto,
                                           String periodo) {
        String asunto = "Tu propuesta fue aprobada - " + abreviar(tituloProyecto, 60);
        String html   = buildEmailPropuestaAprobada(nombreEstudiante, tituloProyecto, periodo);
        enviar(toEmail, asunto, html);
    }

    @Async
    public void notificarAsignacionEquipo(String toEmail,
                                          String nombreEstudiante,
                                          String tituloProyecto,
                                          String nombreDirector,
                                          String nombreDt2,
                                          String periodo) {
        String asunto = "Tu equipo de titulacion ha sido asignado - " + abreviar(tituloProyecto, 50);
        String html   = buildEmailAsignacionEquipo(nombreEstudiante, tituloProyecto, nombreDirector, nombreDt2, periodo);
        enviar(toEmail, asunto, html);
    }

    @Async
    public void notificarDocumentoAprobado(String toEmail,
                                           String nombreEstudiante,
                                           String tituloProyecto,
                                           String nombreDirector,
                                           String periodo) {
        String asunto = "Tu documento fue aprobado por el Director - " + abreviar(tituloProyecto, 50);
        String html   = buildEmailDocumentoAprobado(nombreEstudiante, tituloProyecto, nombreDirector, periodo);
        enviar(toEmail, asunto, html);
    }

    // =========================================================
    // ENVÍO DE CREDENCIALES Y CÓDIGO DE VERIFICACIÓN
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
    // NOTIFICACIÓN DE REUNIÓN / TUTORÍA
    // =========================================================
    public void noticarReunion(String correo, String motivo, String linkReunion,
                               String idreunion, String fecha, String hora) {
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
    // MÉTODO INTERNO DE ENVÍO
    // =========================================================
    private void enviar(String to, String asunto, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(asunto);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("Correo enviado a: " + to + " | " + asunto);
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("Error al enviar correo a " + to + ": " + e.getMessage());
        }
    }

    // =========================================================
    // BUILDERS DE EMAIL (HTML)
    // =========================================================
    private String buildEmailPropuestaAprobada(String nombre, String titulo, String periodo) {
        return baseTemplate(
                "Tu propuesta ha sido aprobada",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Nos complace informarte que tu <strong style='color:#1a365d'>propuesta de titulacion</strong> " +
                        "ha sido revisada y <strong style='color:#276749'>aprobada</strong> por la comision. " +
                        "A continuacion puedes ver los detalles:" +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",  titulo},
                        new String[]{"Periodo",   periodo},
                        new String[]{"Estado",    "APROBADO"}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "El siguiente paso es aguardar la asignacion de tu Director TIC y Docente DT2. " +
                        "Te notificaremos cuando esto ocurra. Puedes revisar el estado de tu proceso " +
                        "en cualquier momento desde el sistema." +
                        "</p>",
                "#f0fff4", "#276749", "PROPUESTA APROBADA"
        );
    }

    private String buildEmailAsignacionEquipo(String nombre, String titulo,
                                              String director, String dt2, String periodo) {
        return baseTemplate(
                "Tu equipo de titulacion ha sido asignado",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Se ha completado la configuracion de tu equipo de titulacion. " +
                        "A partir de ahora puedes comenzar el proceso formal de elaboracion " +
                        "de tu documento de titulacion." +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",      titulo},
                        new String[]{"Director TIC",  director != null ? director : "Por asignar"},
                        new String[]{"Docente DT2",   dt2 != null ? dt2 : "Por asignar"},
                        new String[]{"Periodo",       periodo}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "Coordina con tu Director TIC para iniciar las asesorias de seguimiento. " +
                        "Recuerda que necesitas un minimo de asesorias para avanzar a la siguiente etapa." +
                        "</p>",
                "#ebf8ff", "#2b6cb0", "EQUIPO ASIGNADO"
        );
    }

    private String buildEmailDocumentoAprobado(String nombre, String titulo,
                                               String director, String periodo) {
        return baseTemplate(
                "Tu documento ha sido aprobado",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Tu <strong style='color:#1a365d'>documento de titulacion</strong> ha sido revisado " +
                        "y <strong style='color:#276749'>aprobado</strong> por tu Director TIC. " +
                        "El siguiente paso es la certificacion antiplagio." +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",      titulo},
                        new String[]{"Director TIC",  director},
                        new String[]{"Periodo",       periodo},
                        new String[]{"Siguiente paso","Certificacion Antiplagio (COMPILATIO)"}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "Tu documento sera sometido al proceso de verificacion antiplagio con COMPILATIO. " +
                        "El porcentaje de similitud debe ser menor al 10%% para continuar con la predefensa." +
                        "</p>",
                "#f0fff4", "#276749", "DOCUMENTO APROBADO"
        );
    }

    private String buildEmailDocenteDt2(String nombre, String titulo,
                                        String estudiante, String periodo) {
        return baseTemplate(
                "Ha sido asignado como Docente DT2",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Ha sido designado como <strong style='color:#1a365d'>Docente DT2</strong> " +
                        "del siguiente proyecto de titulacion:" +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",   titulo},
                        new String[]{"Estudiante", estudiante},
                        new String[]{"Periodo",    periodo},
                        new String[]{"Rol",        "Docente DT2"}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "Como Docente DT2 sera responsable del seguimiento de asesorias, la certificacion " +
                        "antiplagio (COMPILATIO), la coordinacion de la predefensa y el acompanamiento " +
                        "en la sustentacion final del estudiante." +
                        "</p>",
                "#ebf8ff", "#2b6cb0", "DOCENTE DT2"
        );
    }

    private String buildEmailDirector(String nombre, String titulo,
                                      String estudiante, String periodo) {
        return baseTemplate(
                "Ha sido asignado como Director TIC",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Ha sido designado como <strong style='color:#1a365d'>Director TIC</strong> " +
                        "del siguiente proyecto de titulacion:" +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",   titulo},
                        new String[]{"Estudiante", estudiante},
                        new String[]{"Periodo",    periodo},
                        new String[]{"Rol",        "Director TIC"}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "Como Director TIC estara a cargo de revisar y aprobar el documento de titulacion, " +
                        "registrar asesorias de seguimiento y emitir su calificacion en la predefensa y " +
                        "sustentacion final." +
                        "</p>",
                "#f0fff4", "#276749", "DIRECTOR TIC"
        );
    }

    private String buildEmailTribunal(String nombre, String cargo, String titulo,
                                      String estudiante, String periodo) {
        return baseTemplate(
                "Ha sido asignado al Tribunal de Titulacion",
                nombre,
                "<p style='margin:0 0 16px;color:#4a5568;font-size:15px;line-height:1.7'>" +
                        "Ha sido designado como <strong style='color:#1a365d'>" + cargo + " del Tribunal</strong> " +
                        "en el siguiente proceso de titulacion:" +
                        "</p>",
                List.of(
                        new String[]{"Proyecto",   titulo},
                        new String[]{"Estudiante", estudiante},
                        new String[]{"Periodo",    periodo},
                        new String[]{"Cargo",      cargo}
                ),
                "<p style='margin:20px 0 0;color:#4a5568;font-size:14px;line-height:1.6'>" +
                        "Como miembro del tribunal debera evaluar la predefensa y la sustentacion final, " +
                        "registrando su calificacion y observaciones directamente en el sistema." +
                        "</p>",
                "#fffff0", "#744210", cargo.toUpperCase()
        );
    }

    // =========================================================
    // TEMPLATE BASE HTML
    // =========================================================
    private String baseTemplate(String titulo,
                                String nombreDestinatario,
                                String descripcionHtml,
                                List<String[]> datos,
                                String notaHtml,
                                String badgeBg,
                                String badgeColor,
                                String badgeLabel) {

        String fecha = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "EC")));

        StringBuilder filas = new StringBuilder();
        for (String[] fila : datos) {
            filas.append(
                    "<tr>" +
                            "<td style='padding:11px 18px;font-size:12px;color:#718096;font-weight:700;" +
                            "text-transform:uppercase;letter-spacing:0.5px;white-space:nowrap;" +
                            "width:130px;border-bottom:1px solid #f0f4f8;background:#fafbfc'>"
                            + escapeHtml(fila[0]) + "</td>" +
                            "<td style='padding:11px 18px;font-size:14px;color:#1a202c;" +
                            "border-bottom:1px solid #f0f4f8'>"
                            + escapeHtml(fila[1]) + "</td>" +
                            "</tr>"
            );
        }

        return
                "<!DOCTYPE html>" +
                        "<html lang='es'>" +
                        "<head>" +
                        "<meta charset='UTF-8'/>" +
                        "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
                        "<title>" + escapeHtml(titulo) + "</title>" +
                        "</head>" +
                        "<body style='margin:0;padding:0;background:#edf2f7;" +
                        "font-family:Segoe UI,Helvetica Neue,Arial,sans-serif'>" +

                        "<table width='100%' cellpadding='0' cellspacing='0' border='0'" +
                        " style='background:#edf2f7;padding:48px 16px'>" +
                        "<tr><td align='center'>" +

                        "<table width='580' cellpadding='0' cellspacing='0' border='0'" +
                        " style='max-width:580px;width:100%'>" +

                        "<tr><td style='background:linear-gradient(135deg,#1a365d 0%,#2d6a4f 100%);" +
                        "border-radius:14px 14px 0 0;padding:40px 44px;text-align:center'>" +
                        "<div style='display:inline-block;background:rgba(255,255,255,0.18);" +
                        "border-radius:50%;width:68px;height:68px;line-height:68px;" +
                        "text-align:center;font-size:30px;margin-bottom:18px'>&#127891;</div>" +
                        "<h1 style='margin:0;color:#fff;font-size:21px;font-weight:700;" +
                        "letter-spacing:-0.3px'>Sistema de Titulaci&#xF3;n</h1>" +
                        "<p style='margin:6px 0 0;color:rgba(255,255,255,0.7);font-size:13px'>" +
                        "Universidad T&#xE9;cnica Estatal de Quevedo" +
                        "</p>" +
                        "</td></tr>" +

                        "<tr><td style='background:" + badgeBg + ";text-align:center;padding:12px 0'>" +
                        "<span style='display:inline-block;color:" + badgeColor + ";" +
                        "font-size:11px;font-weight:800;letter-spacing:1.5px;" +
                        "text-transform:uppercase;border:2px solid " + badgeColor + ";" +
                        "padding:5px 20px;border-radius:20px'>" +
                        escapeHtml(badgeLabel) +
                        "</span>" +
                        "</td></tr>" +

                        "<tr><td style='background:#ffffff;padding:38px 44px;" +
                        "border-left:1px solid #e2e8f0;border-right:1px solid #e2e8f0'>" +

                        "<h2 style='margin:0 0 6px;color:#1a365d;font-size:20px;font-weight:700'>" +
                        escapeHtml(titulo) +
                        "</h2>" +
                        "<p style='margin:0 0 24px;color:#718096;font-size:14px'>" +
                        "Estimado/a <strong style='color:#2d3748'>" + escapeHtml(nombreDestinatario) + "</strong>," +
                        "</p>" +

                        descripcionHtml +

                        "<table width='100%' cellpadding='0' cellspacing='0' border='0'" +
                        " style='border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;" +
                        "margin:8px 0 4px'>" +
                        filas +
                        "</table>" +

                        notaHtml +

                        "<hr style='border:none;border-top:1px solid #edf2f7;margin:28px 0'/>" +

                        "<div style='text-align:center'>" +
                        "<a href='http://localhost:4200/login'" +
                        " style='display:inline-block;" +
                        "background:linear-gradient(135deg,#1a365d 0%,#2d6a4f 100%);" +
                        "color:#ffffff;text-decoration:none;" +
                        "padding:14px 40px;border-radius:8px;" +
                        "font-size:15px;font-weight:600;letter-spacing:0.3px'>" +
                        "Acceder al Sistema &#8594;" +
                        "</a>" +
                        "</div>" +

                        "</td></tr>" +

                        "<tr><td style='background:#2d3748;border-radius:0 0 14px 14px;" +
                        "padding:22px 44px;text-align:center'>" +
                        "<p style='margin:0 0 4px;color:rgba(255,255,255,0.55);font-size:12px'>" +
                        "Correo generado autom&#xE1;ticamente el " + fecha +
                        "</p>" +
                        "<p style='margin:0;color:rgba(255,255,255,0.35);font-size:11px'>" +
                        "Universidad T&#xE9;cnica Estatal de Quevedo &mdash; GPT Titulaci&#xF3;n 2026" +
                        "</p>" +
                        "</td></tr>" +

                        "</table>" +
                        "</td></tr></table>" +
                        "</body></html>";
    }

    // =========================================================
    // UTILIDADES
    // =========================================================
    private String abreviar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max) + "..." : texto;
    }

    private String escapeHtml(String texto) {
        if (texto == null) return "";
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}