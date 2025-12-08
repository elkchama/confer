package com.example.confer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByVendedor(Usuario vendedor);
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    List<Producto> findByCategoria(String categoria);
    List<Producto> findByCategoriaAndMarca(String categoria, String marca);
    List<Producto> findByMarca(String marca);

    @Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.categoria IS NOT NULL")
    List<String> findDistinctCategoria();
}