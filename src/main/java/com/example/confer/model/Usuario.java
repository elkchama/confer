package com.example.confer.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(unique = true, nullable = false)
    private String correo;
    
    private String telefono;
    
    private String genero;
    
    @Column(nullable = false)
    private String password;
    
    @Transient
    private String confirmarPassword;
    
    // Campos específicos para vendedores
    private String empresa;      
    private String nit;          
    private String direccion;    
    
    @Column(name = "id_rol")
    private Integer idRol = 3; // Por defecto cliente (3)

    @Column(name = "requiere_cambio_contrasena")
    private boolean requiereCambioContrasena = false;

    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;

    // Campos para recuperación de contraseña
    @Column(name = "codigo_recuperacion", length = 6)
    private String codigoRecuperacion;
    
    @Column(name = "fecha_codigo_expira")
    private LocalDateTime fechaCodigoExpira;

    // Constructores
    public Usuario() {}

    public Usuario(String nombre, String correo, String password) {
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
    }

    // Getters y Setters
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

    // Métodos auxiliares
    /**
     * Verifica si es un vendedor (rol 3)
     */
    @Transient
    public boolean isVendedor() {
        return idRol != null && idRol == 3;
    }

    /**
     * Verifica si es un administrador (rol 1)
     */
    @Transient
    public boolean isAdmin() {
        return idRol != null && idRol == 1;
    }

    /**
     * Verifica si es un cliente (rol 2)
     */
    @Transient
    public boolean isCliente() {
        return idRol != null && idRol == 2;
    }

    /**
     * Verifica si el código de recuperación está vigente
     */
    @Transient
    public boolean codigoRecuperacionValido() {
        return codigoRecuperacion != null 
               && fechaCodigoExpira != null 
               && LocalDateTime.now().isBefore(fechaCodigoExpira);
    }

    /**
     * Limpia los datos de recuperación de contraseña
     */
    public void limpiarDatosRecuperacion() {
        this.codigoRecuperacion = null;
        this.fechaCodigoExpira = null;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", correo='" + correo + '\'' +
                ", telefono='" + telefono + '\'' +
                ", genero='" + genero + '\'' +
                ", empresa='" + empresa + '\'' +
                ", idRol=" + idRol +
                ", requiereCambioContrasena=" + requiereCambioContrasena +
                '}';
    }
}