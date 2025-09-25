package com.example.confer.repository;

import com.example.confer.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoAndPassword(String correo, String password);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findByIdRol(Integer idRol);

    default List<Usuario> findVendedores() { return findByIdRol(3); }

    default List<Usuario> findClientes() { return findByIdRol(2); }
}
