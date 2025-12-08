package com.example.confer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;
import com.example.confer.repository.ProductoRepository;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    // Listar todos los productos
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // Guardar un nuevo producto o actualizar uno existente
    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    // Obtener un producto por su ID
    public Producto obtenerProductoPorId(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    // Eliminar producto por ID
    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }

    // Listar productos por vendedor
    public List<Producto> listarProductosPorVendedor(Usuario vendedor) {
        return productoRepository.findByVendedor(vendedor);
    }

    // Buscar productos por nombre
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    // Listar productos por categoría
    public List<Producto> listarProductosPorCategoria(String categoria) {
        return productoRepository.findByCategoria(categoria);
    }

    // Listar productos por categoría y marca
    public List<Producto> listarProductosPorCategoriaYMarca(String categoria, String marca) {
        if ((categoria != null && !categoria.isEmpty()) && (marca != null && !marca.isEmpty())) {
            return productoRepository.findByCategoriaAndMarca(categoria, marca);
        } else if (categoria != null && !categoria.isEmpty()) {
            return productoRepository.findByCategoria(categoria);
        } else if (marca != null && !marca.isEmpty()) {
            return productoRepository.findByMarca(marca);
        } else {
            return listarProductos();
        }
    }

    // Listar categorías distintas
    public List<String> listarCategorias() {
        return productoRepository.findDistinctCategoria();
    }

    // Contar productos destacados
    public long contarDestacados() {
        return productoRepository.findAll().stream().filter(Producto::isDestacado).count();
    }

    // Listar productos aplicando filtros dinámicos (categoria, marca, precio mínimo/máximo, búsqueda y filtro tipo)
    public List<Producto> listarProductosFiltrados(String categoria, String marca, java.math.BigDecimal minPrecio, java.math.BigDecimal maxPrecio, String buscar, String filtro) {
        final String search = buscar != null ? buscar.trim().toLowerCase() : "";
        final String marcaFiltro = marca != null ? marca.trim().toLowerCase() : "";
        final String categoriaFiltro = categoria != null ? categoria.trim() : "";

        return listarProductos().stream()
                .filter(p -> {
                    // Búsqueda libre en nombre, descripción, categoría y marca (contiene, case-insensitive)
                    if (!search.isEmpty()) {
                        String nombre = safeLower(p.getNombre());
                        String descripcion = safeLower(p.getDescripcion());
                        String cat = safeLower(p.getCategoria());
                        String mar = safeLower(p.getMarca());
                        boolean coincide = (nombre.contains(search)
                                || descripcion.contains(search)
                                || cat.contains(search)
                                || mar.contains(search));
                        if (!coincide) return false;
                    }

                    // Categoría exacta cuando se selecciona desde el select
                    if (!categoriaFiltro.isEmpty()) {
                        if (p.getCategoria() == null || !p.getCategoria().equalsIgnoreCase(categoriaFiltro)) return false;
                    }

                    // Marca: permitir coincidencia parcial, case-insensitive
                    if (!marcaFiltro.isEmpty()) {
                        String mar = safeLower(p.getMarca());
                        if (mar.isEmpty() || !mar.contains(marcaFiltro)) return false;
                    }

                    if (minPrecio != null) {
                        if (p.getPrecio() == null || p.getPrecio().compareTo(minPrecio) < 0) return false;
                    }
                    if (maxPrecio != null) {
                        if (p.getPrecio() == null || p.getPrecio().compareTo(maxPrecio) > 0) return false;
                    }
                    if (filtro != null && !filtro.isEmpty()) {
                        if ("destacados".equalsIgnoreCase(filtro) && !p.isDestacado()) return false;
                    }
                    return true;
                })
                .toList();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}