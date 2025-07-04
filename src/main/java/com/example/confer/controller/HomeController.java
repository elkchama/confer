package com.example.confer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Ruta raíz (http://localhost:8070/)
    @GetMapping("/")
    public String mostrarInicio() {
        return "bienvenida"; // nombre del archivo sin .html
    }
}
