package Modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "publicacion")
public class Publicacion {

    // se genera con la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;

    private String descripcion;
    private Double latitud;
    private Double longitud;
    private String barrio;
    private String ciudad;

    @OneToMany(
            mappedBy = "publicacion",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<PublicacionFoto> fotos = new ArrayList<>();

    private boolean activa = true;

    // relaciones con otras clases
    @OneToOne
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    @OneToMany(mappedBy = "publicacion")
    @JsonIgnore
    private List<Avistamiento> avistamientos;

    public Publicacion(LocalDate fecha, String descripcion, Double latitud, Double longitud,
                       String barrio, String ciudad, List<PublicacionFoto> fotos, Mascota mascota, Usuario creador) {
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.barrio = barrio;
        this.ciudad = ciudad;
        this.fotos = fotos;
        this.activa = true; // por defecto publicacion está activa
        this.mascota = mascota;
        this.creador = creador;
        this.avistamientos = new ArrayList<>(); // por defecto lista de avistamientos vacía
    }

    public Publicacion() {

    }

    // GETTERS
    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
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

    public String getCiudad() { return ciudad;}

    public List<PublicacionFoto> getFotos() {
        return fotos;
    }

    public boolean isActiva() {
        return activa;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public Usuario getCreador() {
        return creador;
    }

    public List<Avistamiento> getAvistamientos() {
        return avistamientos;
    }


    // SETTERS

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public void setMascota(Mascota mascota) {
        this.mascota = mascota;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

    public void setAvistamientos(List<Avistamiento> avistamientos) {
        this.avistamientos = avistamientos;
    }

    public void addFoto(PublicacionFoto foto) {
        fotos.add(foto);
        foto.setPublicacion(this);
    }

    public void removeFoto(PublicacionFoto foto) {
        fotos.remove(foto);
        foto.setPublicacion(null);
    }

    // forzar que al persistir una publicacion, esta quede activa
    @PrePersist
    public void prePersist() {
        this.activa = true;
    }

}