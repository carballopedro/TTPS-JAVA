package Persistencia.DAO.Implementacion;

import Modelo.Avistamiento;
import Persistencia.ClasesUtilitarias.EMF;
import Persistencia.DAO.Genericos.GenericDAOHibernateJPA;
import Persistencia.DAO.Interfaces.PublicacionDAO;
import Modelo.Publicacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PublicacionDAOHibernateJPA extends GenericDAOHibernateJPA<Publicacion> implements PublicacionDAO {

    public PublicacionDAOHibernateJPA() {
        super(Publicacion.class);
    }

    @Override
    public List<Publicacion> getByLatitudYLongitud(Double latitud, Double longitud) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Publicacion> q = em.createQuery(
                    "SELECT p FROM Publicacion p WHERE p.latitud = :lat AND p.longitud = :lon",
                    Publicacion.class
            );
            q.setParameter("lat", latitud);
            q.setParameter("lon", longitud);
            return q.getResultList();
    }

    @Override
    public List<Publicacion> getActivas() {
        EntityManager em = this.getEntityManager();
            TypedQuery<Publicacion> q = em.createQuery(
                    "SELECT p FROM Publicacion p WHERE p.activa = true",
                    Publicacion.class
            );
            return q.getResultList();
    }

    @Override
    public List<Publicacion> getInactivas() {
        EntityManager em = this.getEntityManager();
            TypedQuery<Publicacion> q = em.createQuery(
                    "SELECT p FROM Publicacion p WHERE p.activa = false",
                    Publicacion.class
            );
            return q.getResultList();
    }

}
