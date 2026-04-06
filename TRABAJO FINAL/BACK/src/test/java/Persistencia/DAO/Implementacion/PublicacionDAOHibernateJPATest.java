package Persistencia.DAO.Implementacion;
import Modelo.Enums.EstadoMascota;
import Modelo.Enums.RazaMascota;
import Modelo.Enums.TamanoMascota;
import Modelo.Enums.TipoMascota;
import Modelo.Mascota;
import Modelo.Publicacion;
import Modelo.Usuario;
import Persistencia.Config.PersistenceConfig;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Persistencia.DAO.Interfaces.PublicacionDAO;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PublicacionDAOHibernateJPATest {

    private static PublicacionDAO publicacionDAO;
    private static MascotaDAO mascotaDAO;
    private static UsuarioDAO usuarioDAO;

    private Usuario creador;
    private Mascota mascota;
    private Publicacion publicacion;

    private static AnnotationConfigApplicationContext ctx;

    // ejecuta antes de todos los tests para inicializar el contexto de Spring
    @BeforeAll
    static void init() {
        // Create a new AnnotationConfigApplicationContext
        ctx = new AnnotationConfigApplicationContext();
        // Registra la clase de configuration (PersistenceConfig)
        ctx.register(PersistenceConfig.class);
        // Refresca para actualizar la creacion de beans
        ctx.refresh();
        usuarioDAO = ctx.getBean(UsuarioDAO.class);
        publicacionDAO = ctx.getBean(PublicacionDAO.class);
        mascotaDAO = ctx.getBean(MascotaDAO.class);
    }

    // ejecuta antes de cada test para crear un usuario de prueba
    @BeforeEach
    void setUp() {

        creador = usuarioDAO.persist(new Usuario("Creador", "Test", "creador@gmail.com", "pwd", "111", "Centro", "La Plata", -34.921, -57.954));

        mascota = mascotaDAO.persist(new Mascota(
                "Lola", "Negro",
                TipoMascota.PERRO, TamanoMascota.PEQUENO,
                EstadoMascota.PERDIDO_AJENO, RazaMascota.OTRO
        ));

        publicacion = publicacionDAO.persist(new Publicacion(
                LocalDate.now(), "Se perdió cerca de plaza", -34.9205, -57.9536,
                "Centro", "La Plata", new ArrayList<>(), mascota, creador
        ));
    }

    // ejecuta después de cada test para limpiar la base de datos
    @AfterEach
    void tearDown() {
        if (publicacion != null && publicacion.getId() != null) publicacionDAO.delete(publicacion.getId());
        if (mascota != null && mascota.getId() != null) mascotaDAO.delete(mascota.getId());
        if (creador != null && creador.getId() != null) usuarioDAO.delete(creador.getId());
    }

    // Tests CRUD genéricos

    @Test
    void deletePublicacionByEntityTest() {
        publicacionDAO.delete(publicacion);
        assertNull(publicacionDAO.get(publicacion.getId()));
    }

    @Test
    void deletePublicacionByIdTest() {
        Long id = publicacion.getId();
        publicacionDAO.delete(id);
        assertNull(publicacionDAO.get(id));
    }

    @Test
    void getPublicacionTest() {
        Publicacion p = publicacionDAO.get(publicacion.getId());
        assertNotNull(p);
        assertEquals(publicacion.getId(), p.getId());
    }

    @Test
    void getAllPublicacionesTest() {
        // creo nueva mascota para la segunda publicación
        Mascota otraMascota = mascotaDAO.persist(new Mascota(
                "Lola2", "Marrón",
                TipoMascota.PERRO, TamanoMascota.MEDIANO,
                EstadoMascota.PERDIDO_AJENO, RazaMascota.OTRO
        ));

        // creo segunda publicación con mascota distinta
        Publicacion otra = publicacionDAO.persist(new Publicacion(
                LocalDate.now(), "Segunda publicación", -34.9210, -57.9540,
                "Centro", "La Plata", new ArrayList<>(), otraMascota, creador
        ));

        List<Publicacion> publicaciones = publicacionDAO.getAll("id");

        assertNotNull(publicaciones);
        assertTrue(publicaciones.size() > 1);
        assertTrue(publicaciones.stream().anyMatch(p -> p.getId().equals(publicacion.getId())));
        assertTrue(publicaciones.stream().anyMatch(p -> p.getId().equals(otra.getId())));

        // limpieza de lo creado por este test
        publicacionDAO.delete(otra.getId());
        mascotaDAO.delete(otraMascota.getId());
    }

    @Test
    void updatePublicacionTest() {
        publicacion.setDescripcion("Actualizada");
        Publicacion upd = publicacionDAO.update(publicacion);
        assertEquals("Actualizada", upd.getDescripcion());
    }

    @Test
    void persistPublicacionTest() {
        assertNotNull(publicacion.getId());
        assertTrue(publicacion.isActiva());
        assertEquals("Centro", publicacion.getBarrio());
    }


    // Tests métodos específicos PublicacionDAO

    @Test
    void getByLatitudYLongitudTest() {
        List<Publicacion> resultados = publicacionDAO.getByLatitudYLongitud(
                publicacion.getLatitud(),
                publicacion.getLongitud()
        );

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream()
                .anyMatch(p -> p.getId().equals(publicacion.getId())));
    }

    @Test
    void getActivasTest() {
        assertTrue(publicacionDAO.getActivas().stream()
                .anyMatch(p -> p.getId().equals(publicacion.getId())));
    }

    @Test
    void getInactivasTest() {
        assertTrue(publicacionDAO.getInactivas().stream()
                .noneMatch(p -> p.getId().equals(publicacion.getId())));
    }

}
