package com.example.confer.service;

import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.confer.model.Usuario;
import com.example.confer.repository.UsuarioRepository;



@Service
public class RecuperarContrasenaService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    public boolean recuperar(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Generar nueva contraseña temporal
            String nuevaContrasena = generarContrasenaTemporal();

            // Encriptar
            String contrasenaEncriptada = passwordEncoder.encode(nuevaContrasena);

            // Guardar nueva contraseña y marcar que requiere cambio
            usuario.setPassword(contrasenaEncriptada);
            usuario.setRequiereCambioContrasena(true);
            usuarioRepository.save(usuario);

            // Enviar correo
            String asunto = "Recuperación de contraseña - Confer";
            String cuerpo = "Hola " + usuario.getNombre() + ",\n\n"
                    + "Tu nueva contraseña temporal es: " + nuevaContrasena + "\n\n"
                    + "Por seguridad, cámbiala la próxima vez que inicies sesión.\n\n"
                    + "Atentamente,\nEl equipo de Confer";

            emailService.enviarCorreo(correo, asunto, cuerpo);

            return true;
        }

        return false;
    }

    private String generarContrasenaTemporal() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }
}
