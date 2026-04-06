package Modelo.DTOs;

import Modelo.Publicacion;
import Modelo.Usuario;

// DTO usado para exponer una publicación al público.
// Incluye los datos de la publicación y algunos datos básicos del usuario creador,
// evitando exponer la entidad Usuario completa.

public class PublicacionPublicaDTO {

    private Publicacion publicacion;  // ← TODA la publicación, igual que antes

    private Long creadorId;
    private String creadorNombre;
    private String creadorApellido;
    private String creadorEmail;
    private String creadorTelefono;

    public PublicacionPublicaDTO(Publicacion pub) {
        this.publicacion = pub;

        Usuario creador = pub.getCreador();
        if (creador != null) {
            this.creadorId = creador.getId();
            this.creadorNombre = creador.getNombre();
            this.creadorApellido = creador.getApellido();
            this.creadorEmail = creador.getEmail();
            this.creadorTelefono = creador.getTelefono();
        }
    }

    // Getters y setters

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public Long getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(Long creadorId) {
        this.creadorId = creadorId;
    }

    public String getCreadorNombre() {
        return creadorNombre;
    }

    public void setCreadorNombre(String creadorNombre) {
        this.creadorNombre = creadorNombre;
    }

    public String getCreadorApellido() {
        return creadorApellido;
    }

    public void setCreadorApellido(String creadorApellido) {
        this.creadorApellido = creadorApellido;
    }

    public String getCreadorEmail() {
        return creadorEmail;
    }

    public void setCreadorEmail(String creadorEmail) {
        this.creadorEmail = creadorEmail;
    }

    public String getCreadorTelefono() {
        return creadorTelefono;
    }

    public void setCreadorTelefono(String creadorTelefono) {
        this.creadorTelefono = creadorTelefono;
    }
}