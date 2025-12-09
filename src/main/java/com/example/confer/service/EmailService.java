package com.example.confer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service

public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    @Async
    public void enviarCorreoRegistroExitoso(String destinatario, String nombreUsuario) {
        try {
            System.out.println("=== INICIO ENVÍO DE CORREO ===");
            System.out.println("Destinatario: " + destinatario);
            System.out.println("Nombre usuario: " + nombreUsuario);
            
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("confersysten@gmail.com");
            helper.setTo(destinatario);
            helper.setSubject("🎉 Registro exitoso en Cónfer");
            
            System.out.println("Mensaje configurado, intentando enviar...");

            // URL pública del banner (asegúrate que el nombre de archivo sea conferbanner.jpg)
            // URL pública del banner
            String bannerUrl = appUrl + "/img/conferbanner.jpg";

            // En pruebas locales podrías usar:
            // String bannerUrl = "http://localhost:8070/img/conferbanner.jpg";

            String contenidoHtml =
                "<div style='font-family: Arial, sans-serif; max-width:600px; margin:auto; " +
                "border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;'> " +

                // Banner
                "<div style='background-color:#f4f4f4; text-align:center; padding:20px;'>" +
                "<img src='" + bannerUrl + "' alt='Cónfer Banner' " +
                "style='max-width:100%; height:auto; border-radius:6px;'/>" +
                "</div>" +

                // Cuerpo
                "<div style='padding:20px;'>" +
                "<h2 style='color:#333;'>¡Bienvenido a <span style='color:#6A1B9A;'>Cónfer</span>, " + nombreUsuario + "!</h2>" +
                "<p style='font-size:16px; color:#555;'>Tu registro en nuestra plataforma se ha completado correctamente. " +
                "Ahora puedes explorar, conectarte con vendedores y descubrir los mejores productos en un solo lugar.</p>" +

                "<p style='font-size:16px; color:#555;'>Para comenzar, personaliza tu perfil " +
                "para que otros usuarios y vendedores puedan encontrarte fácilmente.</p>" +

                "<p style='font-size:14px; color:#777; margin-top:30px;'>Si tienes alguna pregunta, visita nuestro " +
                "<a href='https://tudominio.com/ayuda' style='color:#6A1B9A;'>centro de ayuda</a>.</p>" +
                "</div>" +

                // Pie
                "<div style='background-color:#f4f4f4; text-align:center; padding:10px; font-size:12px; color:#999;'>" +
                "© 2025 Cónfer. Este es un mensaje automático, por favor no respondas." +
                "</div>" +
                "</div>";

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            
            System.out.println("✅ Correo enviado exitosamente a: " + destinatario);

        } catch (MessagingException e) {
            System.err.println("❌ ERROR al enviar correo: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ ERROR GENERAL al enviar correo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
    try {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        helper.setFrom("confersysten@gmail.com");
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(cuerpo, true); // true permite contenido HTML

        mailSender.send(mensaje);
    } catch (MessagingException e) {
        e.printStackTrace();
    }
}

}