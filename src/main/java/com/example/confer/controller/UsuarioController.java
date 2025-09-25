package com.example.confer.controller;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.confer.model.Usuario;
import com.example.confer.service.EmailService;
import com.example.confer.service.ProductoService;
import com.example.confer.service.ReportePDFService;
import com.example.confer.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ReportePDFService reportePDFService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;


    @GetMapping("/")
    public String mostrarPaginaInicio() {
        return "login";
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin(@RequestParam(value = "registrado", required = false) String registrado,
                                         Model model) {
        if (registrado != null) {
            model.addAttribute("msgSuccess", "Registro exitoso. ¡Ahora puedes iniciar sesión!");
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                 @RequestParam String password,
                                 Model model,
                                 HttpSession session) {
        try {
            return usuarioService.autenticar(correo, password)
                    .map(usuarios -> {
                        Integer rol = usuarios.getIdRol();
                        if (rol == null) {
                            model.addAttribute("error", "Tu cuenta no tiene un rol asignado.");
                            return "login";
                        }

                        session.setAttribute("usuario", usuarios);

                        if (rol == 2) {
                            model.addAttribute("usuario", usuarios);
                            return "bienvenida";
                        } else if (rol == 3) {
                            return "redirect:/vendedor/index";
                        } else {
                            return "redirect:/admin";
                        }
                    })
                    .orElseGet(() -> {
                        model.addAttribute("error", "Credenciales inválidas. Intenta nuevamente.");
                        return "login";
                    });

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Ocurrió un error inesperado: " + e.getMessage());
            return "login";
        }
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario,
                                   @RequestParam String tipoUsuario,
                                   @RequestParam(required = false) String empresa,
                                   @RequestParam(required = false) String nit,
                                   @RequestParam(required = false) String direccion,
                                   Model model) {
        try {
            int rol = Integer.parseInt(tipoUsuario);
            usuario.setIdRol(rol);

            if (rol == 3) {
                usuario.setEmpresa(empresa);
                usuario.setNit(nit);
                usuario.setDireccion(direccion);
            }

            usuarioService.guardar(usuario);
            
            // Enviar notificación al correo del usuario
            emailService.enviarCorreoRegistroExitoso(usuario.getCorreo(), usuario.getNombre());

            return "redirect:/login?registrado";

        } catch (NumberFormatException e) {
            model.addAttribute("error", "Tipo de usuario inválido");
            return "registro";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/vendedor/inicio")
    public String vistaVendedor() {
        return "indexVendedor";
    }

    @GetMapping("/admin")
    public String vistaAdmin() {
        return "admin";
    }

    @GetMapping("/admin/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios";
    }

    @GetMapping("/admin/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "admin/usuario-form";
    }

    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        usuarioService.guardar(usuario);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        model.addAttribute("usuario", usuario);
        return "admin/usuario-form";
    }

    @GetMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios/reporte")
    public ResponseEntity<InputStreamResource> generarReportePDF(@RequestParam(value = "rol", required = false) String rol) {
        List<Usuario> usuarios = usuarioService.listarTodos();
        ByteArrayInputStream bis = reportePDFService.generarReporteUsuarios(usuarios, rol);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=usuarios.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
