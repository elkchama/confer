package com.example.confer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.confer.model.Usuario;
import com.example.confer.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class ContactoController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/contactos")
    public String listarVendedores(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        List<Usuario> vendedores = usuarioRepository.findByIdRol(3);
        model.addAttribute("usuario", usuario);
        model.addAttribute("vendedores", vendedores);
        return "contactos";
    }

    @GetMapping("/contactos/vendedores")
    public String listarVendedoresPorIdRol(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        List<Usuario> vendedores = usuarioRepository.findByIdRol(3);
        model.addAttribute("usuario", usuario);
        model.addAttribute("vendedores", vendedores);
        return "contactos";
    }
}