package Modelo.DTOs;

import Modelo.Enums.Badge;

import java.util.List;

// DTO usado para el ranking de usuarios.
// Contiene información resumida del usuario y sus puntos,
// evitando exponer datos sensibles o innecesarios.

public class UsuarioRankingDTO {

    private Long id;
    private String nombre;
    private String apellido;
    private String barrio;
    private String ciudad;
    private Integer puntos;
    private List<Badge> badges;

    public UsuarioRankingDTO(Long id,
                             String nombre,
                             String apellido,
                             String barrio,
                             String ciudad,
                             Integer puntos,
                             List<Badge> badges) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.barrio = barrio;
        this.ciudad = ciudad;
        this.puntos = puntos;
        this.badges = badges;
    }

    // Getters (no necesitás setters)
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getBarrio() { return barrio; }
    public String getCiudad() { return ciudad; }
    public Integer getPuntos() { return puntos; }
    public List<Badge> getBadges() { return badges; }
}