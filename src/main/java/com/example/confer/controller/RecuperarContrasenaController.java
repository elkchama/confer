package com.example.confer.controller;

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

    // Muestra el formulario HTML
    @GetMapping("/recuperar")
    public String mostrarFormularioRecuperar() {
        return "recuperar"; // busca templates/recuperar.html
    }

    // Procesa el formulario
    @PostMapping("/recuperar")
    public String recuperarContrasena(@RequestParam String correo, Model model) {
        boolean resultado = recuperarContrasenaService.recuperar(correo);

        if (resultado) {
            model.addAttribute("mensaje", "✅ Se ha enviado una nueva contraseña a tu correo.");
            model.addAttribute("exito", true);
        } else {
            model.addAttribute("mensaje", "❌ No se encontró un usuario con ese correo.");
            model.addAttribute("exito", false);
        }

        return "recuperar"; // vuelve a la misma vista con el mensaje
    }
}
