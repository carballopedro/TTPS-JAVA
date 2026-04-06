package Modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "publicacion_foto")
public class PublicacionFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NO se serializa en JSON (pero sigue en la BD)
    @JsonIgnore
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(
            name = "foto",
            nullable = false,
            columnDefinition = "LONGBLOB"
    )
    private byte[] foto;

    // también la ignoramos para evitar recursiones infinitas
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;

    // getters & setters iguales que antes

    public Long getId() {
        return id;
    }

    public byte[] getFoto() {
        return foto;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFoto(byte[] foto) {
        this.foto = foto;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }
}
