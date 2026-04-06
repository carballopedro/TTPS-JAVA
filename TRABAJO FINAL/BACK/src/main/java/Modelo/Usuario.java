package Modelo;

import Modelo.Enums.Badge;
import Modelo.Enums.RolUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "usuario")
public class Usuario {
    // se genera con la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;

    // se utiliza como identificador único
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String telefono;
    private String barrio;
    private String ciudad;
    private Integer puntos;
    private boolean habilitado;

    private Double latitud;
    private Double longitud;

    // campos opcionales
    private String instagram;
    private String sitioWeb;
    private String descripcion;

    // relaciones con otras clases
    @Enumerated(EnumType.STRING)
    private RolUsuario rol;

    @ElementCollection(targetClass = Badge.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuario_badge", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "badge")
    @JsonIgnore
    private List<Badge> badges;

    @OneToMany(mappedBy = "creador")
    @JsonIgnore
    private List<Publicacion> publicaciones;

    @OneToMany(mappedBy = "creador")
    @JsonIgnore
    private List<Avistamiento> avistamientos;

    // para inicializar valores por defecto antes de persistir
    @PrePersist
    public void prePersist() {
        if (puntos == null) puntos = 0;
        if (rol == null) rol = RolUsuario.USUARIO;
        habilitado = true;
    }

    public Usuario(String nombre, String apellido, String email, String password, String telefono, String barrio, String ciudad, Double latitud, Double longitud) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.barrio = barrio;
        this.ciudad = ciudad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.puntos = 0;
        this.habilitado = true; // por defecto el usuario está habilitado
        this.rol = RolUsuario.USUARIO; // por defecto el rol es USUARIO

        // por defecto null
        this.instagram = null;
        this.sitioWeb = null;
        this.descripcion = null;

        // por defecto listas vacías
        this.badges = new ArrayList<>();
        this.publicaciones = new ArrayList<>();
        this.avistamientos = new ArrayList<>();
    }

    public Usuario() {

    }

    // GETTERS
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getBarrio() {
        return barrio;
    }

    public String getCiudad() {
        return ciudad;
    }

    public Double getLatitud() { return latitud;}

    public Double getLongitud() { return longitud; }

    public Integer getPuntos() {
        return puntos;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public List<Badge> getBadges() {
        return badges;
    }

    public List<Publicacion> getPublicaciones() {
        return publicaciones;
    }

    public List<Avistamiento> getAvistamientos() {
        return avistamientos;
    }

    // SETTERS

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public void setBadges(List<Badge> badges) {
        this.badges = badges;
    }

    public void setPublicaciones(List<Publicacion> publicaciones) {
        this.publicaciones = publicaciones;
    }

    public void setAvistamientos(List<Avistamiento> avistamientos) {
        this.avistamientos = avistamientos;
    }

}