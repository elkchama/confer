package com.example.confer.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.confer.service.RecuperarContrasenaService;
 
@Controller
@RequestMapping("/auth")
public class RecuperarContrasenaController {

    @Autowired
    private RecuperarContrasenaService recuperarContrasenaService;

    // Paso 1: mostrar formulario para ingresar correo
    @GetMapping("/recuperar")
    public String mostrarFormularioRecuperar() {
        return "recuperar"; // templates/recuperar.html
    }

    // Paso 1: procesar correo y enviar código
    @PostMapping("/recuperar")
    public String enviarCodigo(@RequestParam String correo, Model model) {
        boolean resultado = recuperarContrasenaService.enviarCodigo(correo);

        if (resultado) {
            model.addAttribute("mensaje", "✅ Se ha enviado un código de verificación a tu correo.");
            model.addAttribute("exito", true);
        } else {
            model.addAttribute("mensaje", "❌ No se encontró un usuario con ese correo.");
            model.addAttribute("exito", false);
        }

        return "recuperar";
    }

    // Paso 2: mostrar formulario para ingresar código + nueva contraseña
    @GetMapping("/recuperar/codigo")
    public String mostrarFormularioCodigo() {
        return "recuperar_codigo"; // templates/recuperar_codigo.html
    }

    // Paso 2: procesar código y nueva contraseña
    @PostMapping("/recuperar/codigo")
    public String confirmarCodigo(@RequestParam String correo,
                                  @RequestParam String codigo,
                                  @RequestParam String nuevaContrasena,
                                  Model model) {
        boolean resultado = recuperarContrasenaService.confirmarCodigo(correo, codigo, nuevaContrasena);

        if (resultado) {
            model.addAttribute("mensaje", "✅ Tu contraseña ha sido actualizada correctamente.");
            model.addAttribute("exito", true);
        } else {
            model.addAttribute("mensaje", "❌ Código incorrecto o expirado.");
            model.addAttribute("exito", false);
        }

        return "recuperar_codigo";
    }
}
