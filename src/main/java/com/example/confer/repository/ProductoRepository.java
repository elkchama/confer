package com.example.confer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByVendedor(Usuario vendedor);
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    List<Producto> findByCategoria(String categoria);
    List<Producto> findByCategoriaAndMarca(String categoria, String marca);
    List<Producto> findByMarca(String marca);
}

