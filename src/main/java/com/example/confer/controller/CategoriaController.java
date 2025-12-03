package com.example.confer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoriaController {
    @GetMapping("/categorias")
    public String mostrarCategorias() {
        return "categorias";
    }
}

