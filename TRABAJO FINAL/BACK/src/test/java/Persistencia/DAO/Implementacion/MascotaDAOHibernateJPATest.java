package Persistencia.DAO.Implementacion;

import Modelo.Enums.EstadoMascota;
import Modelo.Enums.RazaMascota;
import Modelo.Enums.TamanoMascota;
import Modelo.Enums.TipoMascota;
import Modelo.Mascota;
import Persistencia.Config.PersistenceConfig;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MascotaDAOHibernateJPATest {

    private static MascotaDAO mascotaDAO;
    private Mascota mascota;

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
        mascotaDAO = ctx.getBean(MascotaDAO.class);
    }

    // ejecuta antes de cada test para crear un usuario de prueba
    @BeforeEach
    void setUp() {
        mascota = mascotaDAO.persist(new Mascota(
                "Firulais", "Marrón",
                TipoMascota.PERRO, TamanoMascota.MEDIANO,
                EstadoMascota.PERDIDO_PROPIO, RazaMascota.LABRADOR
        ));
    }

    // ejecuta después de cada test para limpiar la base de datos
    @AfterEach
    void tearDown() {
        if (mascota != null && mascota.getId() != null) {
            mascotaDAO.delete(mascota.getId());
        }
    }

    // Tests CRUD genéricos

    @Test
    void deleteMascotaByEntityTest() {
        mascotaDAO.delete(mascota);
        assertNull(mascotaDAO.get(mascota.getId()));
    }

    @Test
    void deleteMascotaByIdTest() {
        Long id = mascota.getId();
        mascotaDAO.delete(id);
        assertNull(mascotaDAO.get(id));
    }

    @Test
    void getMascotaTest() {
        Mascota encontrada = mascotaDAO.get(mascota.getId());
        assertNotNull(encontrada);
        assertEquals(mascota.getId(), encontrada.getId());
    }

    @Test
    void getAllMascotasTest() {
        // creo una segunda mascota
        Mascota segundaMascota = mascotaDAO.persist(new Mascota(
                "Michi", "Gris",
                TipoMascota.GATO, TamanoMascota.PEQUENO,
                EstadoMascota.ADOPTADO, RazaMascota.OTRO
        ));

        List<Mascota> mascotas =  mascotaDAO.getAll("id");

        assertNotNull(mascotas);
        assertTrue(mascotas.size()>1);
        assertTrue(mascotas.stream().anyMatch(m -> m.getId().equals(mascota.getId())));
        assertTrue(mascotas.stream().anyMatch(m -> m.getId().equals(segundaMascota.getId())));

        // limpio la segunda mascota creada
        mascotaDAO.delete(segundaMascota.getId());
    }

    @Test
    void updateMascotaTest() {
        mascota.setColor("Negro");
        Mascota upd = mascotaDAO.update(mascota);
        assertEquals("Negro", upd.getColor());
    }

    @Test
    void persistMascotaTest() {
        assertNotNull(mascota.getId());
        assertEquals("Firulais", mascota.getNombre());
    }

    // Tests métodos específicos MascotaDAO

    @Test
    void getByEstadoTest() {
        assertTrue(mascotaDAO.getByEstado(EstadoMascota.PERDIDO_PROPIO)
                .stream().anyMatch(m -> m.getId().equals(mascota.getId())));
    }

    @Test
    void getByTipoTest() {
        assertTrue(mascotaDAO.getByTipo(TipoMascota.PERRO)
                .stream().anyMatch(m -> m.getId().equals(mascota.getId())));
    }

    @Test
    void getByRazaTest() {
        assertTrue(mascotaDAO.getByRaza(RazaMascota.LABRADOR)
                .stream().anyMatch(m -> m.getId().equals(mascota.getId())));
    }

    @Test
    void getByTamanoTest() {
        assertTrue(mascotaDAO.getByTamano(TamanoMascota.MEDIANO)
                .stream().anyMatch(m -> m.getId().equals(mascota.getId())));
    }

}
