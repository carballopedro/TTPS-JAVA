package Modelo;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table (name = "avistamiento")
public class Avistamiento {

    // se genera con la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private Double latitud;
    private Double longitud;
    private String barrio;
    private String comentario;

    @ElementCollection
    @CollectionTable(name = "avistamiento_foto", joinColumns = @JoinColumn(name = "avistamiento_id"))
    @Column(name = "foto")
    private List<String> fotos;

    // relaciones con otras clases

    @ManyToOne
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    @ManyToOne
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;

    public Avistamiento(LocalDate fecha, Double latidud, Double longitud, String barrio, String comentario,
                        List<String> fotos, Usuario creador, Publicacion publicacion) {
        this.fecha = fecha;
        this.latitud = latidud;
        this.longitud = longitud;
        this.barrio = barrio;
        this.comentario = comentario;
        this.fotos = fotos;
        this.creador = creador;
        this.publicacion = publicacion;
    }

    public Avistamiento() {}

    // GETTERS
    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getBarrio() {
        return barrio;
    }

    public String getComentario() {
        return comentario;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public Usuario getCreador() {
        return creador;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    // SETTERS

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }
}