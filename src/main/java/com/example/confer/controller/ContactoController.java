package com.example.confer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.confer.model.Usuario;
import com.example.confer.repository.UsuarioRepository;

@Controller
public class ContactoController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/contactos")
    public String listarVendedores(Model model) {
        List<Usuario> vendedores = usuarioRepository.findByIdRol(3);
        model.addAttribute("vendedores", vendedores);
        return "contactos";
    }

    @GetMapping("/contactos/vendedores")
    public String listarVendedoresPorIdRol(Model model) {
        List<Usuario> vendedores = usuarioRepository.findByIdRol(3);
        model.addAttribute("vendedores", vendedores);
        return "contactos";
    }
}
