package com.example.confer.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.confer.model.Usuario;
import com.example.confer.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario guardar(Usuario usuario) {
        // Si se está editando y no se envió contraseña, conservar la anterior
        if (usuario.getId() != null) {
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null && (usuario.getPassword() == null || usuario.getPassword().isBlank())) {
                usuario.setPassword(existente.getPassword());
            } else if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                // Si se cambió la contraseña, encriptarla
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        } else {
            // Registro nuevo: encriptar contraseña
            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
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

    // Autenticación con password encriptado
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

        // Validar correo duplicado solo si es un nuevo usuario o si cambió el correo
        if (usuario.getId() == null) {
            if (existeCorreo(usuario.getCorreo())) {
                throw new IllegalArgumentException("El correo ya está registrado");
            }
        } else {
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null && !existente.getCorreo().equals(usuario.getCorreo())) {
                if (existeCorreo(usuario.getCorreo())) {
                    throw new IllegalArgumentException("El correo ya está registrado");
                }
            }
        }

        if (usuario.getPassword() != null && !usuario.getPassword().trim().isEmpty() && usuario.getPassword().length() < 4) {
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

    // Obtener ID del usuario desde el request
    public Long getIdFromToken(HttpServletRequest req) {
        // First prefer the request attribute set by any token-based filter
        Object id = req.getAttribute("idUsuario");
        if (id != null) {
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return Long.valueOf((String) id);
            try {
                return Long.valueOf(id.toString());
            } catch (NumberFormatException ex) {
                // ignore and continue
            }
        }

        // Fallback to session attribute (used by session-based login in UsuarioController)
        try {
            if (req.getSession(false) != null) {
                Object usuarioSesion = req.getSession(false).getAttribute("usuario");
                if (usuarioSesion != null && usuarioSesion instanceof com.example.confer.model.Usuario) {
                    com.example.confer.model.Usuario u = (com.example.confer.model.Usuario) usuarioSesion;
                    return u.getId();
                }
            }
        } catch (IllegalStateException e) {
            // session invalidated or unavailable, ignore and return null below
        }

        return null;
    }

    // Actualizar perfil (solo datos permitidos, no contraseña)
    public void actualizarPerfil(Long id, Usuario datos) {
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        // Actualizamos solo información pública del perfil
        if (datos.getNombre() != null) u.setNombre(datos.getNombre());
        if (datos.getCorreo() != null) u.setCorreo(datos.getCorreo());
        if (datos.getTelefono() != null) u.setTelefono(datos.getTelefono());
        // Sólo aplicar campos de vendedor (empresa, nit, direccion) si el usuario es vendedor (idRol == 3)
        if (u.getIdRol() != null && u.getIdRol() == 3) {
            if (datos.getDireccion() != null) u.setDireccion(datos.getDireccion());
            if (datos.getEmpresa() != null) u.setEmpresa(datos.getEmpresa());
            if (datos.getNit() != null) u.setNit(datos.getNit());
        }

        usuarioRepository.save(u);
    }

    // Guardar imagen de perfil en carpeta local
    public String guardarImagen(Long idUsuario, MultipartFile imagen) throws Exception {
        if (imagen == null || imagen.isEmpty()) {
        throw new IllegalArgumentException("No se recibió ninguna imagen");
        }

        String carpeta = "uploads/perfiles/";

        File directorio = new File(carpeta);
        if (!directorio.exists()) {
        directorio.mkdirs();
    }

        // Crear nombre único
        String nombreArchivo = "perfil_" + idUsuario + "_" + System.currentTimeMillis() + "_" + imagen.getOriginalFilename();

            Path ruta = Paths.get(carpeta + nombreArchivo);
        Files.write(ruta, imagen.getBytes());

        // Ruta accesible desde la web
    String rutaWeb = "/uploads/perfiles/" + nombreArchivo;

    // Guardar nombre en BD
    Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
    if (usuario != null) {
        usuario.setFotoPerfil(rutaWeb);
        usuarioRepository.save(usuario);
    }

    return rutaWeb;
}
}