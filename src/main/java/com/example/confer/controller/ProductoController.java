package com.example.confer.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;
import com.example.confer.service.ProductoService;
import com.example.confer.service.ReporteProductoPDFService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/vendedor")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @GetMapping("/index")
    public String mostrarProductos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        // Cambia la vista a indexVendedor para el panel del vendedor
        model.addAttribute("productos", productoService.listarProductos());
        model.addAttribute("nuevoProducto", new Producto());
        return "indexVendedor";
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto,
                                   @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión como vendedor.");
                return "redirect:/login";
            }
            if (imagen != null && !imagen.isEmpty()) {
                String nombreImagen = procesarImagen(imagen);
                producto.setImagenUrl(nombreImagen);
            }
            producto.setVendedor(vendedor);
            // La categoría ya viene del formulario
            productoService.guardarProducto(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado exitosamente");
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el producto: " + e.getMessage());
        }
        return "redirect:/vendedor/index";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model, HttpSession session) {
        Usuario vendedor = (Usuario) session.getAttribute("usuario");
        if (vendedor == null) {
            return "redirect:/login";
        }
        Producto producto = productoService.obtenerProductoPorId(id);
        if (producto == null || !vendedor.getId().equals(producto.getVendedor().getId())) {
            model.addAttribute("error", "Producto no encontrado o no autorizado");
            return "redirect:/vendedor/index";
        }
        model.addAttribute("productoEditar", producto);
        return "vendedor/editarProducto";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarProducto(@PathVariable Long id,
                                     @ModelAttribute Producto producto,
                                     @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                     @RequestParam(value = "mantenerImagen", defaultValue = "true") boolean mantenerImagen,
                                     RedirectAttributes redirectAttributes,
                                     HttpSession session) {
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión como vendedor.");
                return "redirect:/login";
            }
            Producto productoExistente = productoService.obtenerProductoPorId(id);
            if (productoExistente == null || !vendedor.getId().equals(productoExistente.getVendedor().getId())) {
                redirectAttributes.addFlashAttribute("error", "Producto no encontrado o no autorizado");
                return "redirect:/vendedor/index";
            }
            productoExistente.setNombre(producto.getNombre());
            productoExistente.setDescripcion(producto.getDescripcion());
            productoExistente.setPrecio(producto.getPrecio());

            if (imagen != null && !imagen.isEmpty()) {
                if (productoExistente.getImagenUrl() != null) {
                    eliminarImagen(productoExistente.getImagenUrl());
                }
                String nombreImagen = procesarImagen(imagen);
                productoExistente.setImagenUrl(nombreImagen);
            } else if (!mantenerImagen && productoExistente.getImagenUrl() != null) {
                eliminarImagen(productoExistente.getImagenUrl());
                productoExistente.setImagenUrl(null);
            }

            productoService.guardarProducto(productoExistente);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado exitosamente");
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el producto: " + e.getMessage());
        }
        return "redirect:/vendedor/index";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión como vendedor.");
                return "redirect:/login";
            }
            Producto producto = productoService.obtenerProductoPorId(id);
            if (producto != null && vendedor.getId().equals(producto.getVendedor().getId())) {
                if (producto.getImagenUrl() != null) {
                    eliminarImagen(producto.getImagenUrl());
                }
                productoService.eliminarProducto(id);
                redirectAttributes.addFlashAttribute("success", "Producto eliminado exitosamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "Producto no encontrado o no autorizado");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el producto: " + e.getMessage());
        }
        return "redirect:/vendedor/index";
    }

    private String procesarImagen(MultipartFile imagen) throws IOException {
        String contentType = imagen.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        if (imagen.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La imagen es demasiado grande. Máximo 5MB permitido");
        }
        Path directorioSubida = Paths.get(uploadDir);
        if (!Files.exists(directorioSubida)) {
            Files.createDirectories(directorioSubida);
        }
        String extension = getFileExtension(imagen.getOriginalFilename());
        String nombreUnico = UUID.randomUUID().toString() + "." + extension;
        Path rutaCompleta = directorioSubida.resolve(nombreUnico);
        Files.copy(imagen.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);
        return nombreUnico;
    }

    private void eliminarImagen(String nombreImagen) {
        try {
            Path rutaImagen = Paths.get(uploadDir).resolve(nombreImagen);
            Files.deleteIfExists(rutaImagen);
        } catch (IOException e) {
            System.err.println("Error al eliminar imagen: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    @Autowired
    private ReporteProductoPDFService reporteProductoPDFService;

    @GetMapping("/productos/reporte")
    public ResponseEntity<InputStreamResource> generarReporteProductos(@RequestParam(value = "categoria", required = false) String categoria,
                                                                     @RequestParam(value = "marca", required = false) String marca) {
        List<Producto> productos;
        if ((categoria != null && !categoria.isEmpty()) || (marca != null && !marca.isEmpty())) {
            productos = productoService.listarProductosPorCategoriaYMarca(categoria, marca);
        } else {
            productos = productoService.listarProductos();
        }
        ByteArrayInputStream bis = reporteProductoPDFService.generarReporteProductos(productos);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=productos.pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }


    // =======================
    // CRUD ADMIN PRODUCTOS
    // =======================


    @GetMapping("/admin/productos")
    public String listarProductosAdmin(Model model) {
        model.addAttribute("productos", productoService.listarProductos());
        return "admin/productos"; // Este archivo debe estar en templates/admin/productos.html
    }


    @GetMapping("/admin/productos/nuevo")
    public String mostrarFormularioNuevoProductoAdmin(Model model) {
        model.addAttribute("producto", new Producto());
        return "admin/producto-form";
    }

    @PostMapping("/admin/productos/guardar")
    public String guardarProductoAdmin(@ModelAttribute Producto producto,
                                       @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                       RedirectAttributes redirectAttributes) {
        try {
            if (imagen != null && !imagen.isEmpty()) {
                String nombreImagen = procesarImagen(imagen);
                producto.setImagenUrl(nombreImagen);
            }
            productoService.guardarProducto(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar producto: " + e.getMessage());
        }
        return "redirect:/vendedor/admin/productos";
    }

    @GetMapping("/admin/productos/editar/{id}")
    public String editarProductoAdmin(@PathVariable Long id, Model model) {
        Producto producto = productoService.obtenerProductoPorId(id);
        if (producto == null) {
            return "redirect:/vendedor/admin/productos";
        }
        model.addAttribute("producto", producto);
        return "admin/producto-form";
    }

    @GetMapping("/admin/productos/eliminar/{id}")
    public String eliminarProductoAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminarProducto(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar producto: " + e.getMessage());
        }
        return "redirect:/vendedor/admin/productos";
    }


}

@Controller
class IndexProductosController {

    @Autowired
    private ProductoService productoService;

    @GetMapping("/indexProductos")
    public String mostrarCatalogoGeneral(
            @RequestParam(value = "buscar", required = false) String buscar,
            Model model,
            HttpSession session) {
        List<Producto> productos;
        if (buscar != null && !buscar.isEmpty()) {
            productos = productoService.buscarPorNombre(buscar);
        } else {
            productos = productoService.listarProductos();
        }
        Object usuario = session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("usuario", usuario);
        }
        model.addAttribute("productos", productos);
        model.addAttribute("totalProductos", productos.size());
        model.addAttribute("terminoBusqueda", buscar);
        return "indexProductos";
    }
}