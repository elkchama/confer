package com.example.confer.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${upload.path:uploads/perfiles/}")
    private String uploadPath;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    // ===================== CRUD BÁSICO ======================
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario guardar(Usuario usuario) {
        // Si se está editando y no se envió contraseña, conservar la anterior
        if (usuario.getId() != null) {
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null) {
                if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                    usuario.setPassword(existente.getPassword());
                } else {
                    // Si se cambió la contraseña, encriptarla
                    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
                }
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

    // ===================== BÚSQUEDAS ======================
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

    // ===================== AUTENTICACIÓN ======================
    public Optional<Usuario> autenticar(String correo, String password) {
        return usuarioRepository.findByCorreo(correo)
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()));
    }

    // ===================== PERFIL ======================
    public void actualizarPerfil(Long id, Usuario datos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Actualizamos solo información pública del perfil
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            usuario.setNombre(datos.getNombre());
        }
        
        if (datos.getCorreo() != null && !datos.getCorreo().isBlank()) {
            // Validar que el correo no esté en uso por otro usuario
            if (!usuario.getCorreo().equals(datos.getCorreo()) && existeCorreo(datos.getCorreo())) {
                throw new IllegalArgumentException("El correo ya está registrado por otro usuario");
            }
            usuario.setCorreo(datos.getCorreo());
        }
        
        if (datos.getTelefono() != null) {
            usuario.setTelefono(datos.getTelefono());
        }
        
        if (datos.getGenero() != null) {
            usuario.setGenero(datos.getGenero());
        }
        
        // Campos específicos de vendedor
        if (usuario.isVendedor()) {
            if (datos.getDireccion() != null) {
                usuario.setDireccion(datos.getDireccion());
            }
            if (datos.getEmpresa() != null) {
                usuario.setEmpresa(datos.getEmpresa());
            }
            if (datos.getNit() != null) {
                usuario.setNit(datos.getNit());
            }
        }

        usuarioRepository.save(usuario);
    }

    // ===================== MANEJO DE IMÁGENES ======================
    public String guardarImagen(Long idUsuario, MultipartFile imagen) throws IOException {
        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ninguna imagen");
        }

        // Validar tamaño
        if (imagen.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La imagen no debe superar 5MB");
        }

        // Validar extensión
        String nombreOriginal = imagen.getOriginalFilename();
        if (nombreOriginal == null || !esExtensionValida(nombreOriginal)) {
            throw new IllegalArgumentException("Solo se permiten imágenes JPG, JPEG, PNG o GIF");
        }

        // Crear directorio si no existe
        File directorio = new File(uploadPath);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        // Generar nombre único
        String extension = obtenerExtension(nombreOriginal);
        String nombreArchivo = "perfil_" + idUsuario + "_" + UUID.randomUUID().toString() + extension;

        // Guardar archivo
        Path ruta = Paths.get(uploadPath + nombreArchivo);
        Files.write(ruta, imagen.getBytes());

        // Ruta accesible desde la web
        String rutaWeb = "/uploads/perfiles/" + nombreArchivo;

        // Actualizar en base de datos
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Eliminar imagen anterior si existe
        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
            eliminarImagenAnterior(usuario.getFotoPerfil());
        }

        usuario.setFotoPerfil(rutaWeb);
        usuarioRepository.save(usuario);

        return rutaWeb;
    }

    private void eliminarImagenAnterior(String rutaWeb) {
        try {
            String nombreArchivo = rutaWeb.substring(rutaWeb.lastIndexOf("/") + 1);
            Path ruta = Paths.get(uploadPath + nombreArchivo);
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            // Log error pero no fallar la operación
            System.err.println("No se pudo eliminar la imagen anterior: " + e.getMessage());
        }
    }

    private boolean esExtensionValida(String nombreArchivo) {
        String extension = obtenerExtension(nombreArchivo).toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (extension.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    private String obtenerExtension(String nombreArchivo) {
        int ultimoPunto = nombreArchivo.lastIndexOf(".");
        return ultimoPunto > 0 ? nombreArchivo.substring(ultimoPunto) : "";
    }

    // ===================== UTILIDADES ======================
    public Long getIdFromToken(HttpServletRequest req) {
        Object id = req.getAttribute("idUsuario");
        if (id == null) {
            return null;
        }
        try {
            return Long.parseLong(id.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===================== VALIDACIONES ======================
    private void validarUsuario(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (usuario.getCorreo() == null || usuario.getCorreo().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }

        // Validar formato de correo
        if (!usuario.getCorreo().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("El formato del correo no es válido");
        }

        if (usuario.getId() == null && (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        // Validar correo duplicado
        if (usuario.getId() == null) {
            // Usuario nuevo
            if (existeCorreo(usuario.getCorreo())) {
                throw new IllegalArgumentException("El correo ya está registrado");
            }
        } else {
            // Usuario existente - validar solo si cambió el correo
            Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
            if (existente != null && !existente.getCorreo().equals(usuario.getCorreo())) {
                if (existeCorreo(usuario.getCorreo())) {
                    throw new IllegalArgumentException("El correo ya está registrado");
                }
            }
        }

        if (usuario.getPassword() != null && !usuario.getPassword().trim().isEmpty() 
            && usuario.getPassword().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }

        // Validaciones específicas para vendedores
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