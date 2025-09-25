package com.example.confer.service;

import com.example.confer.model.Usuario;
import com.example.confer.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // ✅ Usa PasswordEncoder, no directamente BCrypt

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario guardar(Usuario usuario) {
        // Si se está editando y no se envió contraseña, conservar la anterior
        if (usuario.getId() != null) {
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null && (usuario.getPassword() == null || usuario.getPassword().isBlank())) {
                usuario.setPassword(existente.getPassword());
            } else {
                // Si se cambió la contraseña, encriptarla
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        } else {
            // Registro nuevo: encriptar contraseña
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        validarUsuario(usuario);
        return usuarioRepository.save(usuario);
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo);
    }

    public List<Usuario> listarPorRol(Integer idRol) {
        return usuarioRepository.findByIdRol(idRol);
    }

    public List<Usuario> listarVendedores() {
        return usuarioRepository.findVendedores();
    }

    public List<Usuario> listarClientes() {
        return usuarioRepository.findClientes();
    }

    // ✅ Autenticación con password encriptado
    public Optional<Usuario> autenticar(String correo, String password) {
        return usuarioRepository.findByCorreo(correo)
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()));
    }

    // Validación de datos según tipo de usuario
    private void validarUsuario(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (usuario.getCorreo() == null || usuario.getCorreo().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }

        if (usuario.getId() == null && (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (usuario.getId() == null && existeCorreo(usuario.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (usuario.getPassword() != null && usuario.getPassword().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }

        if (usuario.getIdRol() != null && usuario.getIdRol() == 3 && usuario.getId() == null) {
            if (usuario.getEmpresa() == null || usuario.getEmpresa().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre de la empresa es obligatorio para vendedores");
            }

            if (usuario.getNit() == null || usuario.getNit().trim().isEmpty()) {
                throw new IllegalArgumentException("El NIT es obligatorio para vendedores");
            }

            if (usuario.getDireccion() == null || usuario.getDireccion().trim().isEmpty()) {
                throw new IllegalArgumentException("La dirección es obligatoria para vendedores");
            }
        }
    }
}
