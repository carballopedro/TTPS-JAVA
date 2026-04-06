package Persistencia.DAO.Implementacion;

import Persistencia.ClasesUtilitarias.EMF;
import Persistencia.DAO.Genericos.GenericDAOHibernateJPA;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import Modelo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UsuarioDAOHibernateJPA extends GenericDAOHibernateJPA<Usuario> implements UsuarioDAO {

    public UsuarioDAOHibernateJPA() {
        super(Usuario.class);
    }

    @Override
    public Usuario getByEmail(String email) {
        EntityManager em = this.getEntityManager();
            return em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
    }

    @Override
    public List<Usuario> getHabilitados() {
        EntityManager em = this.getEntityManager();
            TypedQuery<Usuario> q = em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.habilitado = true",
                    Usuario.class
            );
            return q.getResultList();
    }

    @Override
    public List<Usuario> getDeshabilitados() {
        EntityManager em = this.getEntityManager();
            TypedQuery<Usuario> q = em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.habilitado = false",
                    Usuario.class
            );
            return q.getResultList();
    }

    // devuelve los usuarios con más puntos, limitado por el parámetro 'limite'
    @Override
    public List<Usuario> getTopColaboradores(int limite) {
        EntityManager em = this.getEntityManager();
            TypedQuery<Usuario> q = em.createQuery(
                    "SELECT u FROM Usuario u ORDER BY u.puntos DESC",
                    Usuario.class
            );
            // evita valores 0 o negativos
            q.setMaxResults(Math.max(1, limite));
            return q.getResultList();
    }

}
