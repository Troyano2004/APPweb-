package com.erwin.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @Async
    public void enviarCodigo(String correo, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(correo);
        message.setSubject("Código de verificación - Sistema de Titulación");
        message.setText(
                "Tu código de verificación es: " + codigo +
                        "\n\nIngresa este código en el sistema para continuar."
        );
        mailSender.send(message);
    }

    public void enviarCredenciales(String correo, String username, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(correo);
        message.setSubject("Credenciales de ingreso al sistema");
        message.setText(
                "Tus credenciales de acceso:\n\n" +
                        "Usuario: " + username + "\n" +
                        "Contraseña: " + password + "\n\n" +
                        "Por seguridad, cambia la contraseña al ingresar."
        );

        // ✅ TE FALTABA ESTO
        mailSender.send(message);
    }

    public void enviarRechazo(String correo, String motivo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(correo);
        mensaje.setSubject("Solicitud de registro rechazada");

        String texto = "Estimado usuario,\n\n" +
                "Su solicitud de registro ha sido rechazada por el administrador.\n\n";

        if (motivo != null && !motivo.trim().isEmpty()) {
            texto += "Motivo: " + motivo.trim() + "\n\n";
        }

        texto += "Si considera que esto es un error, puede intentar registrarse nuevamente.\n\n" +
                "Saludos.";

        mensaje.setText(texto);
        mailSender.send(mensaje);
    }
}