package com.example.confer.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    @Column(unique = true, nullable = false)
    private String correo;
    
    private String telefono;
    
    @Column(nullable = false)
    @JsonIgnore
    private String password;
    
    @Transient
    @JsonIgnore
    private String confirmarPassword;
    
    private String empresa;      
    private String nit;          
    private String direccion;    
    
    @Column(name = "id_rol")
    private Integer idRol = 3;

    private boolean requiereCambioContrasena;

    // ===== NUEVO CAMPO PARA PERFIL =====
    @Column(name = "foto_perfil")
    private String fotoPerfil;  // Guardará la URL o nombre del archivo

    // ===== Recuperación de contraseña =====
    @Column(name = "codigo_recuperacion")
    @JsonIgnore
    private String codigoRecuperacion;
    
    @Column(name = "fecha_codigo_expira")
    @JsonIgnore
    private LocalDateTime fechaCodigoExpira;

        // ===== RELACIÓN CON PRODUCTOS (AGREGAR ESTO) =====
    @OneToMany(mappedBy = "vendedor")
    @JsonIgnore
    private List<Producto> productos;

    // ===== Getters y Setters =====
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getNombre() { 
        return nombre; 
    }
    
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }
    
    public String getCorreo() { 
        return correo; 
    }
    
    public void setCorreo(String correo) { 
        this.correo = correo; 
    }
    
    public String getTelefono() { 
        return telefono; 
    }
    
    public void setTelefono(String telefono) { 
        this.telefono = telefono; 
    }
    
    
    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public String getConfirmarPassword() { 
        return confirmarPassword; 
    }
    
    public void setConfirmarPassword(String confirmarPassword) { 
        this.confirmarPassword = confirmarPassword; 
    }
    
    public String getEmpresa() { 
        return empresa; 
    }
    
    public void setEmpresa(String empresa) { 
        this.empresa = empresa; 
    }
    
    public String getNit() { 
        return nit; 
    }
    
    public void setNit(String nit) { 
        this.nit = nit; 
    }
    
    public String getDireccion() { 
        return direccion; 
    }
    
    public void setDireccion(String direccion) { 
        this.direccion = direccion; 
    }
    
    public Integer getIdRol() { 
        return idRol; 
    }
    
    public void setIdRol(Integer idRol) { 
        this.idRol = idRol; 
    }
    
    public boolean isRequiereCambioContrasena() { 
        return requiereCambioContrasena; 
    }
    
    public void setRequiereCambioContrasena(boolean requiereCambioContrasena) { 
        this.requiereCambioContrasena = requiereCambioContrasena; 
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public String getCodigoRecuperacion() {
        return codigoRecuperacion;
    }
    
    public void setCodigoRecuperacion(String codigoRecuperacion) {
        this.codigoRecuperacion = codigoRecuperacion;
    }
    
    public LocalDateTime getFechaCodigoExpira() {
        return fechaCodigoExpira;
    }
    
    public void setFechaCodigoExpira(LocalDateTime fechaCodigoExpira) {
        this.fechaCodigoExpira = fechaCodigoExpira;
    }
}