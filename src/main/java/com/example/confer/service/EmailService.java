package com.example.confer.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreoRegistroExitoso(String destinatario, String nombreUsuario) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Registro exitoso en Cónfer");
            helper.setText(
                "<h3>¡Hola, " + nombreUsuario + "!</h3>" +
                "<p>Tu registro en <strong>Cónfer</strong> se ha completado exitosamente.</p>" +
                "<p>Ya puedes iniciar sesión y comenzar a disfrutar de nuestra plataforma.</p>" +
                "<hr><p style='font-size: 12px;'>Este es un mensaje automático. No respondas a este correo.</p>",
                true
            );

            mailSender.send(mensaje);
        } catch (MessagingException e) {
            e.printStackTrace(); // Puedes también registrar el error
        }
    }
}
