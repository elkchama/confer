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
}
