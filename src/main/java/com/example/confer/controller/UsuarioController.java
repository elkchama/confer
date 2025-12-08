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
import org.springframework.web.multipart.MultipartFile;

import com.example.confer.model.Usuario;
import com.example.confer.service.EmailService;
import com.example.confer.service.ProductoService;
import com.example.confer.service.ReportePDFService;
import com.example.confer.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/bienvenida";
    }
    @GetMapping("/bienvenida")
    public String mostrarBienvenida(HttpSession session, Model model) {
        Object usuario = session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("usuario", usuario);
        } else {
            model.addAttribute("usuario", null);
        }
        return "bienvenida";
    }

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
    public String mostrarPaginaInicio(HttpSession session, Model model) {
        Object usuario = session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("usuario", usuario);
        } else {
            model.addAttribute("usuario", null);
        }
        return "bienvenida";
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

                        if (rol == 3) {
                            // Vendedor: redirige a /vendedor/index
                            return "redirect:/vendedor/index";
                        } else if (rol == 2) {
                            // Cliente: bienvenida
                            model.addAttribute("usuario", usuarios);
                            return "bienvenida";
                        } else {
                            // Otros roles (ejemplo: admin)
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
    public String vistaVendedor(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        // Cargar productos del vendedor autenticado
        model.addAttribute("productos", productoService.listarProductosPorVendedor(usuario));
        model.addAttribute("nuevoProducto", new com.example.confer.model.Producto());
        return "indexVendedor";
    }

    @GetMapping("/admin")
    public String vistaAdmin(Model model) {
        // Totales básicos
        long totalUsuarios = usuarioService.listarTodos().size();
        long totalProductos = productoService.listarProductos().size();
        long totalVendedores = usuarioService.listarVendedores().size();
        long totalCategorias = productoService.listarCategorias().size();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalVendedores", totalVendedores);
        model.addAttribute("totalCategorias", totalCategorias);

        // Roles (Clientes, Vendedores, Administradores)
        long clientesCount = usuarioService.listarClientes().size();
        long vendedoresCount = usuarioService.listarVendedores().size();
        long adminsCount = usuarioService.listarPorRol(1) != null ? usuarioService.listarPorRol(1).size() : 0;
        model.addAttribute("rolesLabels", java.util.Arrays.asList("Clientes", "Vendedores", "Administradores"));
        model.addAttribute("rolesCounts", java.util.Arrays.asList(clientesCount, vendedoresCount, adminsCount));

        // Categorías y conteos por categoría
        java.util.List<String> categorias = productoService.listarCategorias();
        java.util.List<Long> categoriasCounts = new java.util.ArrayList<>();
        for (String c : categorias) {
            categoriasCounts.add((long) productoService.listarProductosPorCategoria(c).size());
        }
        model.addAttribute("categoriesLabels", categorias);
        model.addAttribute("categoriesCounts", categoriasCounts);

        // Top categorías (máximo 5)
        java.util.List<java.util.Map.Entry<String, Long>> catPairs = new java.util.ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            catPairs.add(new java.util.AbstractMap.SimpleEntry<>(categorias.get(i), categoriasCounts.get(i)));
        }
        catPairs.sort((a,b) -> Long.compare(b.getValue(), a.getValue()));
        java.util.List<String> topCats = new java.util.ArrayList<>();
        java.util.List<Long> topCatsCounts = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(5, catPairs.size()); i++) {
            topCats.add(catPairs.get(i).getKey());
            topCatsCounts.add(catPairs.get(i).getValue());
        }
        model.addAttribute("topCategoriesLabels", topCats);
        model.addAttribute("topCategoriesCounts", topCatsCounts);

        // Market share (usar top 4 o pad con 'Otros')
        java.util.List<String> marketLabels = new java.util.ArrayList<>();
        java.util.List<Long> marketCounts = new java.util.ArrayList<>();
        long otherSum = 0;
        for (int i = 0; i < catPairs.size(); i++) {
            if (i < 3) {
                marketLabels.add(catPairs.get(i).getKey());
                marketCounts.add(catPairs.get(i).getValue());
            } else {
                otherSum += catPairs.get(i).getValue();
            }
        }
        marketLabels.add("Otros");
        marketCounts.add(otherSum);
        model.addAttribute("marketShareLabels", marketLabels);
        model.addAttribute("marketShareCounts", marketCounts);

        // Productos por vendedor (top 5)
        java.util.List<com.example.confer.model.Usuario> vendedores = usuarioService.listarVendedores();
        java.util.List<String> vendorLabels = new java.util.ArrayList<>();
        java.util.List<Long> vendorCounts = new java.util.ArrayList<>();
        for (com.example.confer.model.Usuario v : vendedores) {
            vendorLabels.add(v.getEmpresa() != null && !v.getEmpresa().isEmpty() ? v.getEmpresa() : (v.getNombre() != null ? v.getNombre() : "Vendedor"));
            vendorCounts.add((long) productoService.listarProductosPorVendedor(v).size());
        }
        // ordenar por cantidad y limitar a 5
        java.util.List<java.util.AbstractMap.SimpleEntry<String, Long>> vendorPairs = new java.util.ArrayList<>();
        for (int i = 0; i < vendorLabels.size(); i++) vendorPairs.add(new java.util.AbstractMap.SimpleEntry<>(vendorLabels.get(i), vendorCounts.get(i)));
        vendorPairs.sort((a,b) -> Long.compare(b.getValue(), a.getValue()));
        java.util.List<String> topVLabels = new java.util.ArrayList<>();
        java.util.List<Long> topVCounts = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(5, vendorPairs.size()); i++) {
            topVLabels.add(vendorPairs.get(i).getKey());
            topVCounts.add(vendorPairs.get(i).getValue());
        }
        model.addAttribute("vendorProductsLabels", topVLabels);
        model.addAttribute("vendorProductsCounts", topVCounts);

        // Valores básicos para charts que no tenemos por mes: etiquetas por defecto
        model.addAttribute("usuariosLabels", java.util.Arrays.asList("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"));
        model.addAttribute("usuariosTotals", java.util.Arrays.asList(totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios,totalUsuarios));

        // Valores para productos (usar totales por categoría como aproximación)
        model.addAttribute("productsLabels", categorias);
        model.addAttribute("productsTotals", categoriasCounts);

        // Status y precios: aproximaciones desde servicios
        model.addAttribute("statusLabels", java.util.Arrays.asList("Activos","Destacados","Inactivos"));
        long activos = productoService.listarProductos().stream().filter(p -> !p.isDestacado()).count();
        long destacados = productoService.contarDestacados();
        long inactivos = Math.max(0, productoService.listarProductos().size() - (int)(activos + destacados));
        model.addAttribute("statusCounts", java.util.Arrays.asList(activos, destacados, inactivos));

        // Rangos de precio (buckets simples)
        model.addAttribute("priceLabels", java.util.Arrays.asList("$0-100k","$100k-500k","$500k-1M","$1M-2M","$2M+"));
        java.util.List<Long> priceCountsList = java.util.Arrays.asList(0L,0L,0L,0L,0L);
        for (com.example.confer.model.Producto p : productoService.listarProductos()) {
            if (p.getPrecio() == null) continue;
            double precio = p.getPrecio().doubleValue();
            if (precio < 100000) priceCountsList.set(0, priceCountsList.get(0)+1);
            else if (precio < 500000) priceCountsList.set(1, priceCountsList.get(1)+1);
            else if (precio < 1000000) priceCountsList.set(2, priceCountsList.get(2)+1);
            else if (precio < 2000000) priceCountsList.set(3, priceCountsList.get(3)+1);
            else priceCountsList.set(4, priceCountsList.get(4)+1);
        }
        model.addAttribute("priceCounts", priceCountsList);

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

    // ===================== PERFIL ======================

// Vista del perfil
@GetMapping("/perfil")
public String verPerfil(HttpSession session, Model model) {
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        return "redirect:/login";
    }
    model.addAttribute("usuario", usuario);
    return "perfil";
}

// Vista del perfil del vendedor (solo para rol vendedor)
@GetMapping("/perfilVendedor")
public String verPerfilVendedor(HttpSession session, Model model) {
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        return "redirect:/login";
    }
    // Si no es vendedor, redirigir al perfil genérico
    Integer rol = usuario.getIdRol();
    if (rol == null || rol != 3) {
        return "redirect:/perfil";
    }
    model.addAttribute("usuario", usuario);
    return "perfilVendedor";
}


// Actualizar datos del perfil (excepto contraseña y foto)
@PostMapping("/perfil/actualizar")
public String actualizarPerfil(@ModelAttribute Usuario datos, HttpSession session) {
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        return "redirect:/login";
    }

    usuarioService.actualizarPerfil(usuario.getId(), datos);

    // actualizar usuario en sesión
    Usuario actualizado = usuarioService.obtenerPorId(usuario.getId());
    session.setAttribute("usuario", actualizado);

    return "redirect:/perfil?ok";
}

// Subir foto de perfil
@PostMapping("/perfil/foto")
public String subirFotoPerfil(@RequestParam("foto") MultipartFile archivo,
                              HttpSession session, Model model) {

    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) {
        return "redirect:/login";
    }

    try {
        String nombreArchivo = usuarioService.guardarImagen(usuario.getId(), archivo);

        usuario.setFotoPerfil(nombreArchivo);
        session.setAttribute("usuario", usuario);

        return "redirect:/perfil?foto=ok";

    } catch (Exception e) {
        e.printStackTrace();
        model.addAttribute("errorImagen", "Error al subir imagen: " + e.getMessage());
        return "perfil";
    }
}

}