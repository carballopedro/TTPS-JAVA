package Persistencia.DAO.Implementacion;

import Modelo.Publicacion;
import Modelo.Usuario;
import Persistencia.ClasesUtilitarias.EMF;
import Persistencia.DAO.Genericos.GenericDAOHibernateJPA;
import Persistencia.DAO.Interfaces.AvistamientoDAO;
import Modelo.Avistamiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AvistamientoDAOHibernateJPA extends GenericDAOHibernateJPA<Avistamiento> implements AvistamientoDAO {

    public AvistamientoDAOHibernateJPA() {
        super(Avistamiento.class);
    }

    @Override
    public List<Avistamiento> getByLatitudYLongitud(Double latitud, Double longitud) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Avistamiento> q = em.createQuery(
                    "SELECT a FROM Avistamiento a WHERE a.latitud = :lat AND a.longitud = :lon",
                    Avistamiento.class
            );
            q.setParameter("lat", latitud);
            q.setParameter("lon", longitud);
            return q.getResultList();
    }

    @Override
    public List<Avistamiento> getByPublicacion(Publicacion publicacion) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Avistamiento> q = em.createQuery(
                    "SELECT a FROM Avistamiento a WHERE a.publicacion = :publicacion",
                    Avistamiento.class
            );
            q.setParameter("publicacion", publicacion);
            return q.getResultList();
    }
}

