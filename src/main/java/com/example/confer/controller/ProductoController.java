package com.example.confer.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;
import com.example.confer.service.ProductoService;
import com.example.confer.service.ReporteProductoPDFService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        // Mostrar únicamente los productos pertenecientes al vendedor autenticado
        model.addAttribute("productos", productoService.listarProductosPorVendedor(usuario));
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

    @PostMapping("/guardar-multiple")
    public String guardarMultiplesProductos(@RequestParam("productos") String productosJson,
                                      @RequestParam("imagenes") MultipartFile[] imagenes,
                                      RedirectAttributes redirectAttributes,
                                      HttpSession session) {
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión como vendedor.");
                return "redirect:/login";
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Producto> productos = mapper.readValue(productosJson, 
                new TypeReference<List<Producto>>() {});

            int productosGuardados = 0;
            for (int i = 0; i < productos.size(); i++) {
                Producto producto = productos.get(i);
                producto.setVendedor(vendedor);
                
                // Procesar imagen si existe
                if (i < imagenes.length && !imagenes[i].isEmpty()) {
                    String nombreImagen = procesarImagen(imagenes[i]);
                    producto.setImagenUrl(nombreImagen);
                }
                
                productoService.guardarProducto(producto);
                productosGuardados++;
            }

            redirectAttributes.addFlashAttribute("success", 
                "Se guardaron " + productosGuardados + " productos exitosamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al guardar los productos: " + e.getMessage());
        }
        return "redirect:/vendedor/index";
    }

    @PostMapping("/destacar")
    public ResponseEntity<Map<String, Object>> destacarProducto(@RequestParam("id") Long id,
                                                                 @RequestParam(value = "porcentaje", required = false) String porcentajeStr,
                                                                 HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                resp.put("error", "Sesión no iniciada");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }
            Producto producto = productoService.obtenerProductoPorId(id);
            if (producto == null || producto.getVendedor() == null || !vendedor.getId().equals(producto.getVendedor().getId())) {
                resp.put("error", "Producto no encontrado o no autorizado");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }
            // Parsear porcentaje (acepta string vacío o nulo)
            java.math.BigDecimal porcentaje = java.math.BigDecimal.ZERO;
            if (porcentajeStr != null && !porcentajeStr.trim().isEmpty()) {
                try {
                    porcentaje = new java.math.BigDecimal(porcentajeStr.trim());
                    if (porcentaje.compareTo(java.math.BigDecimal.ZERO) < 0) porcentaje = java.math.BigDecimal.ZERO;
                    if (porcentaje.compareTo(new java.math.BigDecimal("100")) > 0) porcentaje = new java.math.BigDecimal("100");
                } catch (NumberFormatException e) {
                    porcentaje = java.math.BigDecimal.ZERO;
                }
            }
            if (porcentaje.compareTo(java.math.BigDecimal.ZERO) > 0) {
                producto.setDestacado(true);
                producto.setPorcentajeDescuento(porcentaje);
            } else {
                producto.setDestacado(false);
                producto.setPorcentajeDescuento(java.math.BigDecimal.ZERO);
            }
            productoService.guardarProducto(producto);
            resp.put("success", true);
            resp.put("destacado", producto.isDestacado());
            resp.put("porcentaje", producto.getPorcentajeDescuento());
            resp.put("precioConDescuento", producto.getPrecioConDescuento());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    @PostMapping("/destacar-multiple")
    public ResponseEntity<Map<String, Object>> destacarMultiples(@RequestBody Map<String, Object> payload,
                                                                  HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Usuario vendedor = (Usuario) session.getAttribute("usuario");
            if (vendedor == null) {
                resp.put("error", "Sesión no iniciada");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }

            Object idsObj = payload.get("ids");
            Object porcentajeObj = payload.get("porcentaje");

            if (idsObj == null) {
                resp.put("error", "No se recibieron IDs");
                return ResponseEntity.badRequest().body(resp);
            }

            // Convertir ids a lista de Long
            List<Long> ids = new ArrayList<>();
            if (idsObj instanceof List<?>) {
                for (Object o : (List<?>) idsObj) {
                    if (o == null) continue;
                    try {
                        ids.add(Long.valueOf(String.valueOf(o)));
                    } catch (NumberFormatException ex) {
                        // ignorar entradas no válidas
                    }
                }
            }

            java.math.BigDecimal porcentaje = java.math.BigDecimal.ZERO;
            if (porcentajeObj != null) {
                try {
                    porcentaje = new java.math.BigDecimal(String.valueOf(porcentajeObj));
                    if (porcentaje.compareTo(java.math.BigDecimal.ZERO) < 0) porcentaje = java.math.BigDecimal.ZERO;
                    if (porcentaje.compareTo(new java.math.BigDecimal("100")) > 0) porcentaje = new java.math.BigDecimal("100");
                } catch (Exception ex) {
                    porcentaje = java.math.BigDecimal.ZERO;
                }
            }

            int updated = 0;
            List<Long> skipped = new ArrayList<>();
            for (Long id : ids) {
                Producto producto = productoService.obtenerProductoPorId(id);
                if (producto == null || producto.getVendedor() == null || !vendedor.getId().equals(producto.getVendedor().getId())) {
                    skipped.add(id);
                    continue;
                }
                if (porcentaje.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    producto.setDestacado(true);
                    producto.setPorcentajeDescuento(porcentaje);
                } else {
                    producto.setDestacado(false);
                    producto.setPorcentajeDescuento(java.math.BigDecimal.ZERO);
                }
                productoService.guardarProducto(producto);
                updated++;
            }

            resp.put("success", true);
            resp.put("updated", updated);
            resp.put("skipped", skipped);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

}

// prueba git hola

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