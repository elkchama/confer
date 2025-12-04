package com.example.confer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.confer.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoAndPassword(String correo, String password);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findByIdRol(Integer idRol);

    default List<Usuario> findVendedores() { return findByIdRol(3); }

    default List<Usuario> findClientes() { return findByIdRol(2); }
}