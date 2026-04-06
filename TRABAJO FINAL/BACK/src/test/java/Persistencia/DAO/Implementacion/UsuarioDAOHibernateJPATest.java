package Persistencia.DAO.Implementacion;

import Modelo.Usuario;
import Persistencia.Config.PersistenceConfig;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UsuarioDAOHibernateJPATest {

    private static UsuarioDAO usuarioDAO;
    private Usuario usuario;
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
    }

    // ejecuta antes de cada test para crear un usuario de prueba
    @BeforeEach
    void setUp() {

        usuario = usuarioDAO.persist(new Usuario("Pedro",
                "Carballo",
                "pedrocarballo@gmail.com",
                "1234",
                "123456789",
                "Centro",
                "La Plata", -34.921, -57.954
        ));
    }

    // ejecuta después de cada test para limpiar la base de datos
    @AfterEach
    void tearDown() {
        // elimina solo el usuario creado en este test
        if (usuario != null && usuario.getId() != null) {
            usuarioDAO.delete(usuario.getId());
        }
    }

    // Tests CRUD genéricos

    @Test
    void deleteUsuarioByEntityTest() {
        usuarioDAO.delete(usuario);
        Usuario eliminado = usuarioDAO.get(usuario.getId());
        assertNull(eliminado);
    }

    @Test
    void deleteUsuarioByIdTest() {
        Long id = usuario.getId();
        usuarioDAO.delete(id);
        Usuario eliminado = usuarioDAO.get(id);
        assertNull(eliminado);
    }

    @Test
    void getUsuarioTest() {
        Usuario encontrado = usuarioDAO.get(usuario.getId());
        assertNotNull(encontrado);
        assertEquals(usuario.getEmail(), encontrado.getEmail());
        assertEquals("Pedro", encontrado.getNombre());
    }

    @Test
    void getAllUsuariosTest() {
        // creo un segundo usuario
        Usuario otro = usuarioDAO.persist(new Usuario(
                "Laura",
                "Gomez",
                "lauragomez@gmail.com",
                "abcd",
                "987654321",
                "Centro",
                "La Plata", -34.921, -57.954
        ));

        List<Usuario> usuarios = usuarioDAO.getAll("id");

        assertNotNull(usuarios);
        assertTrue(usuarios.size() > 1);
        assertTrue(usuarios.stream().anyMatch(u -> u.getEmail().equals(usuario.getEmail())));
        assertTrue(usuarios.stream().anyMatch(u -> u.getEmail().equals(otro.getEmail())));

        // limpio el segundo usuario
        usuarioDAO.delete(otro.getId());
    }

    @Test
    void updateUsuarioTest() {
        usuario.setTelefono("999999");
        usuarioDAO.update(usuario);

        Usuario actualizado = usuarioDAO.get(usuario.getId());
        assertEquals("999999", actualizado.getTelefono());
    }

    @Test
    void persistUsuarioTest() {
        assertNotNull(usuario.getId());
        assertEquals("Pedro", usuario.getNombre());
        assertEquals("pedrocarballo@gmail.com", usuario.getEmail());
    }

    // Tests métodos específicos UsuarioDAO

    @Test
    void getByEmailTest(){
        Usuario encontrado = usuarioDAO.getByEmail(usuario.getEmail());
        assertNotNull(encontrado);
        assertEquals(usuario.getId(), encontrado.getId());
    }

    @Test
    void getHabilitadosTest() {
        // hay un usuario habilitado
        List<Usuario> habilitados = usuarioDAO.getHabilitados();
        assertNotNull(habilitados);
        assertFalse(habilitados.isEmpty());
        assertTrue(habilitados.stream().anyMatch(u -> u.getId().equals(usuario.getId())));
    }

    @Test
    void getDeshabilitadosTest() {
        // deshabilito el usuario
        usuario.setHabilitado(false);
        usuarioDAO.update(usuario);
        List<Usuario> deshabilitados = usuarioDAO.getDeshabilitados();
        assertNotNull(deshabilitados);
        assertFalse(deshabilitados.isEmpty());
        assertTrue(deshabilitados.stream().anyMatch(u -> u.getId().equals(usuario.getId())));
    }

    @Test
    void getTopColaboradoresTest() {
        // creo un segundo usuario
        Usuario otro = usuarioDAO.persist(new Usuario(
                "Laura",
                "Gomez",
                "lauragomez@gmail.com",
                "abcd",
                "987654321",
                "Centro",
                "La Plata", -34.921, -57.954
        ));

        // asigno puntos
        usuario.setPuntos(150);
        usuarioDAO.update(usuario);
        otro.setPuntos(100);
        usuarioDAO.update(otro);

        // obtengo el top 1
        List<Usuario> topColaboradores = usuarioDAO.getTopColaboradores(1);
        assertNotNull(topColaboradores);
        assertEquals(1, topColaboradores.size());
        assertEquals(usuario.getId(), topColaboradores.get(0).getId());

        // limpio el segundo usuario
        usuarioDAO.delete(otro.getId());
    }
}
