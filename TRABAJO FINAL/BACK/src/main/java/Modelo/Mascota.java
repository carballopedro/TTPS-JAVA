package Modelo;

import Modelo.Enums.EstadoMascota;
import Modelo.Enums.TamanoMascota;
import Modelo.Enums.TipoMascota;
import Modelo.Enums.RazaMascota;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table (name = "mascota")
public class Mascota {

    // se genera con la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String color;

    // enums
    @Enumerated(EnumType.STRING)
    private TipoMascota tipo;      // PERRO, GATO, AVE, OTRO

    @Enumerated(EnumType.STRING)
    private TamanoMascota tamanio;  // PEQUEÑO, MEDIANO, GRANDE

    @Enumerated(EnumType.STRING)
    private EstadoMascota estado;  // PERDIDO_PROPIO, PERDIDO_AJENO, RECUPERADO, ADOPTADO

    @Enumerated(EnumType.STRING)
    private RazaMascota raza; // LABRADOR, BULLDOG, OVEJERO, BEAGLE, OTRO

    @OneToOne(mappedBy = "mascota")
    @JsonIgnore
    private Publicacion publicacion;

    // opcional
    @ManyToOne
    @JoinColumn(name = "duenio_id")
    private Usuario duenio;

    public Mascota(String nombre, String color, TipoMascota tipo, TamanoMascota tamanio, EstadoMascota estado, RazaMascota raza) {
        this.nombre = nombre;
        this.color = color;
        this.tipo = tipo;
        this.tamanio = tamanio;
        this.estado = estado;
        this.raza = raza;
        this.duenio = null; // por defecto no tiene dueño
    }

    public Mascota() {

    }

    // GETTERS
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getColor() {
        return color;
    }

    public TipoMascota getTipo() {
        return tipo;
    }

    public TamanoMascota getTamanio() {
        return tamanio;
    }

    public EstadoMascota getEstado() {
        return estado;
    }

    public RazaMascota getRaza() {
        return raza;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Usuario getDuenio() {
        return duenio;
    }

    // SETTERS

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTipo(TipoMascota tipo) {
        this.tipo = tipo;
    }

    public void setTamanio(TamanoMascota tamanio) {
        this.tamanio = tamanio;
    }

    public void setEstado(EstadoMascota estado) {
        this.estado = estado;
    }

    public void setRaza(RazaMascota raza) {
        this.raza = raza;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public void setDuenio(Usuario duenio) {
        this.duenio = duenio;
    }

}