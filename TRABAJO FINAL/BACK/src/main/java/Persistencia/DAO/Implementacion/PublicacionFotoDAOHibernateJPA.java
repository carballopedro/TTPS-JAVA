package Persistencia.DAO.Implementacion;

import Modelo.PublicacionFoto;
import Persistencia.DAO.Genericos.GenericDAOHibernateJPA;
import Persistencia.DAO.Interfaces.PublicacionFotoDAO;
import org.springframework.stereotype.Repository;

@Repository
public class PublicacionFotoDAOHibernateJPA
        extends GenericDAOHibernateJPA<PublicacionFoto>
        implements PublicacionFotoDAO {

    public PublicacionFotoDAOHibernateJPA() {
        super(PublicacionFoto.class);
    }
}
