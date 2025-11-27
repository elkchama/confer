package com.example.confer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.confer.dto.UsuarioPerfilDTO;
import com.example.confer.model.Usuario;
import com.example.confer.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioPerfilController {

    @Autowired
    private UsuarioService usuarioService;

    // ===================== OBTENER PERFIL =====================
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioPerfilDTO> obtenerPerfil(HttpServletRequest req) {

        Long idUsuario = usuarioService.getIdFromToken(req);
        if (idUsuario == null) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = usuarioService.obtenerPorId(idUsuario);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        // Map to DTO and expose vendor-only fields only if user is vendor
        UsuarioPerfilDTO dto = new UsuarioPerfilDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        dto.setIdRol(usuario.getIdRol());
        dto.setFotoPerfil(usuario.getFotoPerfil());

        if (usuario.getIdRol() != null && usuario.getIdRol() == 3) {
            dto.setEmpresa(usuario.getEmpresa());
            dto.setNit(usuario.getNit());
            dto.setDireccion(usuario.getDireccion());
        }

        return ResponseEntity.ok(dto);
    }

    // ===================== ACTUALIZAR PERFIL =====================
    @PutMapping("/perfil")
    public ResponseEntity<String> actualizarPerfil(
            @RequestBody Usuario datosPerfil,
            HttpServletRequest req
    ) {

        Long idUsuario = usuarioService.getIdFromToken(req);
        if (idUsuario == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }

        // Prevent clients from updating vendor-only fields
        Usuario existente = usuarioService.obtenerPorId(idUsuario);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }
        boolean intentandoActualizarCamposVendedor = (datosPerfil.getEmpresa() != null || datosPerfil.getNit() != null || datosPerfil.getDireccion() != null);
        if (intentandoActualizarCamposVendedor && (existente.getIdRol() == null || existente.getIdRol() != 3)) {
            return ResponseEntity.status(403).body("No autorizado para editar campos de vendedor");
        }

        usuarioService.actualizarPerfil(idUsuario, datosPerfil);
        // Actualizar la sesión si existe
        try {
            if (req.getSession(false) != null) {
                Usuario actualizado = usuarioService.obtenerPorId(idUsuario);
                req.getSession(false).setAttribute("usuario", actualizado);
            }
        } catch (IllegalStateException e) {
            // session not available or invalidated
        }
        return ResponseEntity.ok("Perfil actualizado correctamente");
    }

    // ===================== SUBIR FOTO DE PERFIL =====================
    @PostMapping("/perfil/foto")
    public ResponseEntity<String> subirFoto(
            HttpServletRequest req,
            @RequestParam("file") MultipartFile imagen
    ) {
        try {
            Long idUsuario = usuarioService.getIdFromToken(req);
            if (idUsuario == null) {
                return ResponseEntity.status(401).body("No autenticado");
            }

            String ruta = usuarioService.guardarImagen(idUsuario, imagen);
            // Actualizar la sesión con la ruta nueva si existe
            try {
                if (req.getSession(false) != null) {
                    Usuario u = usuarioService.obtenerPorId(idUsuario);
                    req.getSession(false).setAttribute("usuario", u);
                }
            } catch (IllegalStateException e) {
                // ignore
            }
            return ResponseEntity.ok("Foto actualizada: " + ruta);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al subir foto: " + e.getMessage());
        }
    }

    // ===================== ELIMINAR CUENTA =====================
    @DeleteMapping("/perfil")
    public ResponseEntity<String> eliminarCuenta(HttpServletRequest req) {

        Long idUsuario = usuarioService.getIdFromToken(req);
        if (idUsuario == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }

        usuarioService.eliminar(idUsuario);
        // Invalidar sesión si existe
        try {
            if (req.getSession(false) != null) {
                req.getSession(false).invalidate();
            }
        } catch (IllegalStateException e) {
            // ignore
        }
        return ResponseEntity.ok("Cuenta eliminada correctamente");
    }
}
