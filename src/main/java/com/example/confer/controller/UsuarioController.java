package com.example.confer.controller;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.confer.model.Usuario;
import com.example.confer.service.EmailService;
import com.example.confer.service.ProductoService;
import com.example.confer.service.ReportePDFService;
import com.example.confer.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ProductoService productoService;
    private final ReportePDFService reportePDFService;
    private final EmailService emailService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService, 
                            ProductoService productoService,
                            ReportePDFService reportePDFService, 
                            EmailService emailService) {
        this.usuarioService = usuarioService;
        this.productoService = productoService;
        this.reportePDFService = reportePDFService;
        this.emailService = emailService;
    }

    // ===================== LOGOUT Y BIENVENIDA ======================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/bienvenida";
    }

    @GetMapping({"/", "/bienvenida"})
    public String mostrarBienvenida(HttpSession session, Model model) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "bienvenida";
    }

    // ===================== LOGIN ======================
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
                    .map(usuario -> {
                        if (usuario.getIdRol() == null) {
                            model.addAttribute("error", "Tu cuenta no tiene un rol asignado.");
                            return "login";
                        }

                        session.setAttribute("usuario", usuario);
                        return redirigirSegunRol(usuario.getIdRol(), model, usuario);
                    })
                    .orElseGet(() -> {
                        model.addAttribute("error", "Credenciales inválidas. Intenta nuevamente.");
                        return "login";
                    });
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error inesperado. Por favor, intenta de nuevo.");
            return "login";
        }
    }

    private String redirigirSegunRol(Integer rol, Model model, Usuario usuario) {
        switch (rol) {
            case 1:
                return "redirect:/admin";
            case 2:
                model.addAttribute("usuario", usuario);
                return "bienvenida";
            case 3:
                return "redirect:/vendedor/inicio";
            default:
                return "redirect:/bienvenida";
        }
    }

    // ===================== REGISTRO ======================
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
                                   RedirectAttributes redirectAttributes,
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
            emailService.enviarCorreoRegistroExitoso(usuario.getCorreo(), usuario.getNombre());

            return "redirect:/login?registrado";
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Tipo de usuario inválido.");
            return "registro";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar usuario. Por favor, intenta de nuevo.");
            return "registro";
        }
    }

    // ===================== VISTAS PRINCIPALES ======================
    @GetMapping("/vendedor/inicio")
    public String vistaVendedor(HttpSession session, Model model) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "indexVendedor";
    }

    @GetMapping("/admin")
    public String vistaAdmin(HttpSession session, Model model) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "admin";
    }

    // ===================== ADMIN USUARIOS ======================
    @GetMapping("/admin/usuarios")
    public String listarUsuarios(HttpSession session, Model model) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios";
    }

    @GetMapping("/admin/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(HttpSession session, Model model) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", new Usuario());
        return "admin/usuario-form";
    }

    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, 
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        try {
            usuarioService.guardar(usuario);
            redirectAttributes.addFlashAttribute("msgSuccess", "Usuario guardado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar usuario.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable Long id, 
                               HttpSession session, 
                               Model model) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario == null) {
            return "redirect:/admin/usuarios";
        }
        model.addAttribute("usuario", usuario);
        return "admin/usuario-form";
    }

    @GetMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, 
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (!validarSesion(session)) {
            return "redirect:/login";
        }
        try {
            usuarioService.eliminar(id);
            redirectAttributes.addFlashAttribute("msgSuccess", "Usuario eliminado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar usuario.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios/reporte")
    public ResponseEntity<InputStreamResource> generarReportePDF(
            @RequestParam(value = "rol", required = false) String rol,
            HttpSession session) {
        if (!validarSesion(session)) {
            return ResponseEntity.status(401).build();
        }

        List<Usuario> usuarios = usuarioService.listarTodos();
        ByteArrayInputStream bis = reportePDFService.generarReporteUsuarios(usuarios, rol);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=usuarios.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    // ===================== PERFIL ======================
    @GetMapping("/perfil")
    public String verPerfil(HttpSession session, Model model) {
        Usuario usuario = obtenerUsuarioSesion(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute Usuario datos, 
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioSesion(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            usuarioService.actualizarPerfil(usuario.getId(), datos);
            Usuario actualizado = usuarioService.obtenerPorId(usuario.getId());
            session.setAttribute("usuario", actualizado);
            redirectAttributes.addFlashAttribute("msgSuccess", "Perfil actualizado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar perfil.");
        }

        return "redirect:/perfil";
    }

    @PostMapping("/perfil/foto")
    public String subirFotoPerfil(@RequestParam("foto") MultipartFile archivo,
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioSesion(session);
        if (usuario == null) {
            return "redirect:/login";
        }

        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor selecciona una imagen.");
            return "redirect:/perfil";
        }

        try {
            String nombreArchivo = usuarioService.guardarImagen(usuario.getId(), archivo);
            usuario.setFotoPerfil(nombreArchivo);
            session.setAttribute("usuario", usuario);
            redirectAttributes.addFlashAttribute("msgSuccess", "Foto actualizada exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al subir imagen.");
        }

        return "redirect:/perfil";
    }

    // ===================== MÉTODOS AUXILIARES ======================
    private boolean validarSesion(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    private Usuario obtenerUsuarioSesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuario");
    }
}