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
    public ResponseEntity<Usuario> obtenerPerfil(HttpServletRequest req) {

        Long idUsuario = usuarioService.getIdFromToken(req);
        if (idUsuario == null) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = usuarioService.obtenerPorId(idUsuario);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(usuario);
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

        usuarioService.actualizarPerfil(idUsuario, datosPerfil);
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
        return ResponseEntity.ok("Cuenta eliminada correctamente");
    }
}
