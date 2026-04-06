package Modelo.DTOs;

import Modelo.Enums.Badge;

import java.util.List;

// DTO que representa la información del perfil de un usuario.
// Se usa para enviar al frontend solo los datos necesarios del usuario,
// incluyendo su información personal, ubicación, puntos y badges.

public class UsuarioPerfilDTO {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String barrio;
    private String ciudad;
    private Double latitud;
    private Double longitud;
    private Integer puntos;
    private boolean habilitado;

    private String instagram;
    private String sitioWeb;
    private String descripcion;

    private List<Badge> badges;

    public UsuarioPerfilDTO(
            Long id,
            String nombre,
            String apellido,
            String email,
            String telefono,
            String barrio,
            String ciudad,
            Double latitud,
            Double longitud,
            Integer puntos,
            boolean habilitado,
            String instagram,
            String sitioWeb,
            String descripcion,
            List<Badge> badges
    ) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.barrio = barrio;
        this.ciudad = ciudad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.puntos = puntos;
        this.habilitado = habilitado;
        this.instagram = instagram;
        this.sitioWeb = sitioWeb;
        this.descripcion = descripcion;
        this.badges = badges;
    }

    // getters solamente (DTO inmutable)
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getBarrio() { return barrio; }
    public String getCiudad() { return ciudad; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public Integer getPuntos() { return puntos; }
    public boolean isHabilitado() { return habilitado; }
    public String getInstagram() { return instagram; }
    public String getSitioWeb() { return sitioWeb; }
    public String getDescripcion() { return descripcion; }
    public List<Badge> getBadges() { return badges; }
}