package Persistencia.ClasesUtilitarias;

import Persistencia.DAO.Implementacion.AvistamientoDAOHibernateJPA;
import Persistencia.DAO.Implementacion.MascotaDAOHibernateJPA;
import Persistencia.DAO.Implementacion.PublicacionDAOHibernateJPA;
import Persistencia.DAO.Interfaces.AvistamientoDAO;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Persistencia.DAO.Interfaces.PublicacionDAO;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import Persistencia.DAO.Implementacion.UsuarioDAOHibernateJPA;

// Fábrica de DAOs que se usaba para crear manualmente las implementaciones
// concretas de cada DAO sin usar inyección de dependencias.

// ACTUALMENTE NO SE USA.
// Fue reemplazada por Spring, que crea e inyecta los DAOs automáticamente
// mediante @Autowired.

public class FactoryDAO {

    public static UsuarioDAO getUsuarioDAO() {
        return new UsuarioDAOHibernateJPA();
    }

    public static MascotaDAO getMascotaDAO() {
        return new MascotaDAOHibernateJPA();
    }

    public static PublicacionDAO getPublicacionDAO() {
        return new PublicacionDAOHibernateJPA();
    }

    public static AvistamientoDAO getAvistamientoDAO() {
        return new AvistamientoDAOHibernateJPA();
    }

}
