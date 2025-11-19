package com.example.confer.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    // Paso 1: enviar código de recuperación
    public boolean enviarCodigo(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Generar código de 6 dígitos
            String codigo = generarCodigo();

            // Guardar código y fecha de expiración (10 minutos)
            usuario.setCodigoRecuperacion(codigo);
            usuario.setFechaCodigoExpira(LocalDateTime.now().plusMinutes(10));
            usuarioRepository.save(usuario);

            // Enviar correo
            String asunto = "Código de recuperación - Confer";
            String cuerpo = "Hola " + usuario.getNombre() + ",\n\n"
                    + "Tu código de recuperación es: " + codigo + "\n"
                    + "El código expirará en 10 minutos.\n\n"
                    + "Atentamente,\nEl equipo de Confer";

            emailService.enviarCorreo(correo, asunto, cuerpo);
            return true;
        }

        return false;
    }

    // Paso 2: confirmar código y actualizar contraseña
    public boolean confirmarCodigo(String correo, String codigo, String nuevaContrasena) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Validar código y expiración
            if (usuario.getCodigoRecuperacion() != null
                    && usuario.getCodigoRecuperacion().equals(codigo)
                    && usuario.getFechaCodigoExpira() != null
                    && usuario.getFechaCodigoExpira().isAfter(LocalDateTime.now())) {

                // Guardar nueva contraseña encriptada
                usuario.setPassword(passwordEncoder.encode(nuevaContrasena));

                // Limpiar código y fecha
                usuario.setCodigoRecuperacion(null);
                usuario.setFechaCodigoExpira(null);

                usuarioRepository.save(usuario);
                return true;
            }
        }

        return false;
    }

    // Genera código aleatorio de 6 dígitos
    private String generarCodigo() {
        Random random = new Random();
        int numero = 100000 + random.nextInt(900000); // 100000-999999
        return String.valueOf(numero);
    }
}