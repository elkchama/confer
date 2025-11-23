package com.example.confer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8070}")
    private String baseUrl;

    @Value("${app.email.from:noreply@confer.com}")
    private String fromEmail;

    /**
     * Envía un correo de bienvenida al registrarse un nuevo usuario
     */
    public void enviarCorreoRegistroExitoso(String destinatario, String nombreUsuario) {
        try {
            String bannerUrl = baseUrl + "/img/conferbanner.jpg";
            
            String contenidoHtml = construirPlantillaRegistro(nombreUsuario, bannerUrl);
            
            enviarCorreo(destinatario, "🎉 Registro exitoso en Cónfer", contenidoHtml);
            
        } catch (Exception e) {
            System.err.println("Error al enviar correo de registro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método genérico para enviar correos con contenido HTML
     */
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true); // true permite contenido HTML

            mailSender.send(mensaje);
            
        } catch (MessagingException e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("No se pudo enviar el correo", e);
        }
    }

    /**
     * Envía un correo con código de recuperación de contraseña
     */
    public void enviarCodigoRecuperacion(String destinatario, String nombreUsuario, String codigo) {
        try {
            String contenidoHtml = construirPlantillaRecuperacion(nombreUsuario, codigo);
            enviarCorreo(destinatario, "🔐 Recuperación de contraseña - Cónfer", contenidoHtml);
        } catch (Exception e) {
            System.err.println("Error al enviar código de recuperación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envía notificación de cambio de contraseña exitoso
     */
    public void enviarConfirmacionCambioContrasena(String destinatario, String nombreUsuario) {
        try {
            String contenidoHtml = construirPlantillaCambioContrasena(nombreUsuario);
            enviarCorreo(destinatario, "✅ Contraseña actualizada - Cónfer", contenidoHtml);
        } catch (Exception e) {
            System.err.println("Error al enviar confirmación de cambio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== PLANTILLAS HTML ======================

    private String construirPlantillaRegistro(String nombreUsuario, String bannerUrl) {
        return construirPlantillaBase(
            "¡Bienvenido a <span style='color:#6A1B9A;'>Cónfer</span>, " + nombreUsuario + "!",
            "<p style='font-size:16px; color:#555;'>Tu registro en nuestra plataforma se ha completado correctamente. " +
            "Ahora puedes explorar, conectarte con vendedores y descubrir los mejores productos en un solo lugar.</p>" +
            "<p style='font-size:16px; color:#555;'>Para comenzar, personaliza tu perfil " +
            "para que otros usuarios y vendedores puedan encontrarte fácilmente.</p>" +
            "<p style='font-size:14px; color:#777; margin-top:30px;'>Si tienes alguna pregunta, visita nuestro " +
            "<a href='" + baseUrl + "/ayuda' style='color:#6A1B9A; text-decoration:none;'>centro de ayuda</a>.</p>",
            bannerUrl
        );
    }

    private String construirPlantillaRecuperacion(String nombreUsuario, String codigo) {
        return construirPlantillaBase(
            "Recuperación de contraseña",
            "<p style='font-size:16px; color:#555;'>Hola " + nombreUsuario + ",</p>" +
            "<p style='font-size:16px; color:#555;'>Has solicitado restablecer tu contraseña. " +
            "Usa el siguiente código para continuar:</p>" +
            "<div style='background-color:#f4f4f4; padding:20px; text-align:center; margin:20px 0; border-radius:8px;'>" +
            "<span style='font-size:32px; font-weight:bold; color:#6A1B9A; letter-spacing:5px;'>" + codigo + "</span>" +
            "</div>" +
            "<p style='font-size:14px; color:#777;'>Este código expirará en 15 minutos.</p>" +
            "<p style='font-size:14px; color:#777;'>Si no solicitaste este cambio, ignora este mensaje.</p>",
            null
        );
    }

    private String construirPlantillaCambioContrasena(String nombreUsuario) {
        return construirPlantillaBase(
            "Contraseña actualizada exitosamente",
            "<p style='font-size:16px; color:#555;'>Hola " + nombreUsuario + ",</p>" +
            "<p style='font-size:16px; color:#555;'>Tu contraseña ha sido cambiada exitosamente.</p>" +
            "<p style='font-size:14px; color:#777; margin-top:20px;'>Si no realizaste este cambio, " +
            "por favor contacta a nuestro equipo de soporte inmediatamente.</p>",
            null
        );
    }

    private String construirPlantillaBase(String titulo, String contenido, String bannerUrl) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div style='font-family: Arial, sans-serif; max-width:600px; margin:auto; ")
            .append("border:1px solid #e0e0e0; border-radius:8px; overflow:hidden;'>");

        // Banner (opcional)
        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            html.append("<div style='background-color:#f4f4f4; text-align:center; padding:20px;'>")
                .append("<img src='").append(bannerUrl).append("' alt='Cónfer Banner' ")
                .append("style='max-width:100%; height:auto; border-radius:6px;'/>")
                .append("</div>");
        }

        // Cuerpo
        html.append("<div style='padding:20px;'>")
            .append("<h2 style='color:#333;'>").append(titulo).append("</h2>")
            .append(contenido)
            .append("</div>");

        // Pie
        html.append("<div style='background-color:#f4f4f4; text-align:center; padding:10px; font-size:12px; color:#999;'>")
            .append("© 2025 Cónfer. Este es un mensaje automático, por favor no respondas.")
            .append("</div>")
            .append("</div>");

        return html.toString();
    }
}