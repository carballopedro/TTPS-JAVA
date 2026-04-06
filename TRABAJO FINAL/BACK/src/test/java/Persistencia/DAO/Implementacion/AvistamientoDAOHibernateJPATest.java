package Persistencia.DAO.Implementacion;

import Modelo.Avistamiento;
import Modelo.Publicacion;
import Modelo.Usuario;
import Persistencia.Config.PersistenceConfig;
import Persistencia.DAO.Interfaces.AvistamientoDAO;
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

public class AvistamientoDAOHibernateJPATest {

    private static AvistamientoDAO avistamientoDAO;
    private static PublicacionDAO publicacionDAO;
    private static UsuarioDAO usuarioDAO;

    private Usuario creador;
    private Publicacion publicacion;
    private Avistamiento avistamiento;


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
        avistamientoDAO = ctx.getBean(AvistamientoDAO.class);
    }

    // ejecuta antes de cada test para crear un usuario de prueba
    @BeforeEach
    void setUp() {

        creador = usuarioDAO.persist(new Usuario("Reportero", "Test", "reportero@gmail.com", "pwd", "222", "Norte", "La Plata", -34.921, -57.954));

        publicacion = publicacionDAO.persist(new Publicacion(
                LocalDate.now(), "Publicación base", -34.921, -57.954,
                "Norte", "La Plata", new ArrayList<>(), null, creador
        ));

        avistamiento = avistamientoDAO.persist(new Avistamiento(
                LocalDate.now(),  -34.921, -57.954,
                "Norte", "Visto en la esquina", new ArrayList<>(), creador, publicacion
        ));
    }

    // ejecuta después de cada test para limpiar la base de datos
    @AfterEach
    void tearDown() {
        if (avistamiento != null && avistamiento.getId() != null) avistamientoDAO.delete(avistamiento.getId());
        if (publicacion != null && publicacion.getId() != null) publicacionDAO.delete(publicacion.getId());
        if (creador != null && creador.getId() != null) usuarioDAO.delete(creador.getId());
    }

    // Tests CRUD genéricos

    @Test
    void deleteAvistamientoByEntityTest() {
        avistamientoDAO.delete(avistamiento);
        assertNull(avistamientoDAO.get(avistamiento.getId()));
    }

    @Test
    void deleteAvistamientoByIdTest() {
        Long id = avistamiento.getId();
        avistamientoDAO.delete(id);
        assertNull(avistamientoDAO.get(id));
    }

    @Test
    void getAvistamientoTest() {
        Avistamiento a = avistamientoDAO.get(avistamiento.getId());
        assertNotNull(a);
        assertEquals(avistamiento.getId(), a.getId());
    }

    @Test
    void getAllAvistamientosTest() {
        // creo un segundo avistamiento con las mismas dependencias
        Avistamiento otro = avistamientoDAO.persist(new Avistamiento(
                LocalDate.now(),
                -32.921, -24.430,
                "Norte",
                "Segundo avistamiento",
                new ArrayList<>(),
                creador,
                publicacion
        ));

        List<Avistamiento> avistamientos = avistamientoDAO.getAll("id");

        assertNotNull(avistamientos);
        assertTrue(avistamientos.size() > 1);
        assertTrue(avistamientos.stream().anyMatch(a -> a.getId().equals(avistamiento.getId())));
        assertTrue(avistamientos.stream().anyMatch(a -> a.getId().equals(otro.getId())));

        // limpio el segundo avistamiento
        avistamientoDAO.delete(otro.getId());
    }

    @Test
    void updateAvistamientoTest() {
        avistamiento.setComentario("Actualizado");
        Avistamiento upd = avistamientoDAO.update(avistamiento);
        assertEquals("Actualizado", upd.getComentario());
    }

    @Test
    void persistAvistamientoTest() {
        assertNotNull(avistamiento.getId());
        assertEquals("Norte", avistamiento.getBarrio());
    }

    // Tests métodos específicos AvistamientoDAO

    @Test
    void getByLatitudYLongitudTest() {
        List<Avistamiento> resultados = avistamientoDAO.getByLatitudYLongitud(
                avistamiento.getLatitud(),
                avistamiento.getLongitud()
        );

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream()
                .anyMatch(p -> p.getId().equals(avistamiento.getId())));
    }

    @Test
    void getByPublicacionTest() {
        assertTrue(avistamientoDAO.getByPublicacion(publicacion).stream()
                .anyMatch(a -> a.getId().equals(avistamiento.getId())));
    }
}
