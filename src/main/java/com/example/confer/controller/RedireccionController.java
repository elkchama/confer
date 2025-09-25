package com.example.confer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedireccionController {
    @GetMapping("/bienvenida")
    public String redirigirBienvenidaAProductos() {
        return "redirect:/vendedor/indexProductos";
    }
}
