package Persistencia.DAO.Implementacion;

import Modelo.Enums.EstadoMascota;
import Modelo.Enums.RazaMascota;
import Modelo.Enums.TamanoMascota;
import Modelo.Enums.TipoMascota;
import Persistencia.ClasesUtilitarias.EMF;
import Persistencia.DAO.Genericos.GenericDAOHibernateJPA;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Modelo.Mascota;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MascotaDAOHibernateJPA extends GenericDAOHibernateJPA<Mascota> implements MascotaDAO {

    public MascotaDAOHibernateJPA() {
        super(Mascota.class);
    }

    @Override
    public List<Mascota> getByEstado(EstadoMascota estado) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Mascota> q = em.createQuery(
                    "SELECT m FROM Mascota m WHERE m.estado = :estado",
                    Mascota.class
            );
            q.setParameter("estado", estado);
            return q.getResultList();
    }

    @Override
    public List<Mascota> getByTipo(TipoMascota tipo) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Mascota> q = em.createQuery(
                    "SELECT m FROM Mascota m WHERE m.tipo = :tipo",
                    Mascota.class
            );
            q.setParameter("tipo", tipo);
            return q.getResultList();
    }

    @Override
    public List<Mascota> getByRaza(RazaMascota raza) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Mascota> q = em.createQuery(
                    "SELECT m FROM Mascota m WHERE m.raza = :raza",
                    Mascota.class
            );
            q.setParameter("raza", raza);
            return q.getResultList();
    }

    @Override
    public List<Mascota> getByTamano(TamanoMascota tamanio) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Mascota> q = em.createQuery(
                    "SELECT m FROM Mascota m WHERE m.tamanio = :tamanio",
                    Mascota.class
            );
            q.setParameter("tamanio", tamanio);
            return q.getResultList();
    }
}