package com.example.confer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

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
    private String genero;
    
    @Column(nullable = false)
    private String password;
    
    @Transient
    private String confirmarPassword;
    
    private String empresa;      // Solo si es vendedor
    private String nit;          // Solo si es vendedor
    private String direccion;    // Solo si es vendedor
    
    @Column(name = "id_rol")
    private Integer idRol = 3; // Por defecto cliente (3)
    
    private boolean requiereCambioContrasena;
    
    // ===== NUEVOS CAMPOS PARA RECUPERACIÓN DE CONTRASEÑA =====
    @Column(name = "codigo_recuperacion")
    private String codigoRecuperacion;
    
    @Column(name = "fecha_codigo_expira")
    private LocalDateTime fechaCodigoExpira;
    
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
    
    public String getGenero() { 
        return genero; 
    }
    
    public void setGenero(String genero) { 
        this.genero = genero; 
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
    
    // ===== Getters y Setters para recuperación de contraseña =====
    
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